package com.michaelinacio.saga.camunda;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderWorkers {
    private static final Logger log = LoggerFactory.getLogger(OrderWorkers.class);

    @JobWorker(type = "reserve-inventory", fetchAllVariables = true)
    public Map<String, Object> reserveInventory(ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("E2E reserve-inventory orderId={}", variables.get("orderId"));
        return Map.of("inventoryReserved", true);
    }

    @JobWorker(type = "release-inventory", fetchAllVariables = true)
    public Map<String, Object> releaseInventory(ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("E2E release-inventory compensation orderId={}", variables.get("orderId"));
        return Map.of("inventoryCompensated", true);
    }

    @JobWorker(type = "charge-payment", fetchAllVariables = true)
    public Map<String, Object> chargePayment(ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("E2E sandbox charge-payment orderId={}", variables.get("orderId"));
        return Map.of("paymentCharged", true);
    }

    @JobWorker(type = "refund-payment", fetchAllVariables = true)
    public Map<String, Object> refundPayment(ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("E2E refund-payment compensation orderId={}", variables.get("orderId"));
        return Map.of("paymentCompensated", true);
    }

    @JobWorker(type = "reserve-delivery", autoComplete = false, fetchAllVariables = true)
    public void reserveDelivery(ActivatedJob job, JobClient client) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String orderId = String.valueOf(variables.get("orderId"));
        if (Boolean.TRUE.equals(variables.get("failDelivery"))) {
            log.info("E2E simulated delivery failure orderId={}", orderId);
            client.newThrowErrorCommand(job)
                    .errorCode("ORDER_STEP_FAILED")
                    .errorMessage("Simulated delivery reservation failure")
                    .send()
                    .join();
            return;
        }

        log.info("E2E reserve-delivery orderId={}", orderId);
        client.newCompleteCommand(job)
                .variables(Map.of("deliveryReserved", true))
                .send()
                .join();
    }
}
