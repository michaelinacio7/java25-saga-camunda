package com.michaelinacio.saga.camunda;

import io.camunda.client.CamundaClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CamundaClient camundaClient;

    public OrderController(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody OrderRequest request) {
        if (request == null || blank(request.orderId()) || blank(request.customerId())
                || blank(request.sku()) || request.quantity() < 1
                || request.amount() == null || request.amount().signum() <= 0
                || blank(request.currency())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "orderId, customerId, sku, positive quantity/amount and currency are required"));
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", request.orderId());
        variables.put("customerId", request.customerId());
        variables.put("sku", request.sku());
        variables.put("quantity", request.quantity());
        variables.put("amount", request.amount());
        variables.put("currency", request.currency());
        variables.put("failDelivery", "FAIL_DELIVERY".equals(request.sku()));

        var instance = camundaClient.newCreateInstanceCommand()
                .bpmnProcessId("order-saga")
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        return ResponseEntity.accepted().body(Map.of(
                "processInstanceKey", instance.getProcessInstanceKey(),
                "processDefinitionId", "order-saga",
                "status", "STARTED",
                "sandbox", true));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record OrderRequest(String orderId, String customerId, String sku, int quantity,
                               BigDecimal amount, String currency) {}
}