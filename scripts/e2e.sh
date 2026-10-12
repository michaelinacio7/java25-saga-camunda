#!/usr/bin/env bash
set -euo pipefail

APP_LOG="${RUNNER_TEMP:-/tmp}/camunda-saga-e2e.log"
APP_PID=""

cleanup() {
  if [[ -n "$APP_PID" ]]; then kill "$APP_PID" 2>/dev/null || true; fi
  docker compose down -v --remove-orphans
}
trap cleanup EXIT

docker compose up -d

camunda_ready=0
for _ in $(seq 1 180); do
  code="$(curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/v2/process-definitions/search \
    -H 'Content-Type: application/json' -d '{"filter":{"processDefinitionId":"order-saga"},"page":{"limit":1}}' || true)"
  if [[ "$code" == "200" ]]; then camunda_ready=1; break; fi
  sleep 2
done
if [[ "$camunda_ready" != "1" ]]; then
  docker compose logs --no-color
  echo "Camunda Orchestration Cluster API did not become ready"
  exit 1
fi

mvn --batch-mode --no-transfer-progress spring-boot:run >"$APP_LOG" 2>&1 &
APP_PID=$!

app_ready=0
for _ in $(seq 1 120); do
  code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/api/orders || true)"
  if [[ "$code" == "405" ]]; then app_ready=1; break; fi
  if ! kill -0 "$APP_PID" 2>/dev/null; then cat "$APP_LOG"; exit 1; fi
  sleep 2
done
if [[ "$app_ready" != "1" ]]; then cat "$APP_LOG"; echo "Spring API did not become ready"; exit 1; fi

curl --fail-with-body -sS -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"e2e-success","customerId":"customer-e2e","sku":"SKU-OK","quantity":1,"amount":49.90,"currency":"BRL"}'

curl --fail-with-body -sS -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"e2e-failure","customerId":"customer-e2e","sku":"FAIL_DELIVERY","quantity":1,"amount":49.90,"currency":"BRL"}'

wait_for_log() {
  local pattern="$1"
  for _ in $(seq 1 90); do
    if grep -Fq "$pattern" "$APP_LOG"; then return 0; fi
    sleep 2
  done
  cat "$APP_LOG"
  echo "Expected Camunda worker event not found: $pattern"
  return 1
}

wait_for_log "E2E reserve-delivery orderId=e2e-success"
wait_for_log "E2E simulated delivery failure orderId=e2e-failure"
wait_for_log "E2E refund-payment compensation orderId=e2e-failure"
wait_for_log "E2E release-inventory compensation orderId=e2e-failure"

refund_line="$(grep -nF "E2E refund-payment compensation orderId=e2e-failure" "$APP_LOG" | tail -n1 | cut -d: -f1)"
inventory_line="$(grep -nF "E2E release-inventory compensation orderId=e2e-failure" "$APP_LOG" | tail -n1 | cut -d: -f1)"
if (( refund_line >= inventory_line )); then
  cat "$APP_LOG"
  echo "Compensation order is incorrect: payment refund must precede inventory release"
  exit 1
fi

echo "Camunda end-to-end smoke test passed: delivery failure triggered refund then inventory release."
