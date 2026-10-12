package com.michaelinacio.saga.camunda;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderWorkers {
    @JobWorker(type = "reserve-inventory")
    public Map<String, Object> reserveInventory(Map<String, Object> variables) {
        return Map.of("inventoryReserved", true);
    }

    @JobWorker(type = "release-inventory")
    public Map<String, Object> releaseInventory(Map<String, Object> variables) {
        return Map.of("inventoryCompensated", true);
    }

    @JobWorker(type = "charge-payment")
    public Map<String, Object> chargePayment(Map<String, Object> variables) {
        // Sandbox only: no payment provider is called.
        return Map.of("paymentCharged", true);
    }

    @JobWorker(type = "refund-payment")
    public Map<String, Object> refundPayment(Map<String, Object> variables) {
        return Map.of("paymentCompensated", true);
    }

    @JobWorker(type = "reserve-delivery", autoComplete = false)
    public void reserveDelivery(ActivatedJob job, JobClient client) {
        Map<String, Object> variables = job.getVariablesAsMap();
        if (Boolean.TRUE.equals(variables.get("failDelivery"))) {
            client.newThrowErrorCommand(job)
                    .errorCode("ORDER_STEP_FAILED")
                    .errorMessage("Simulated delivery reservation failure")
                    .send()
                    .join();
            return;
        }

        client.newCompleteCommand(job)
                .variables(Map.of("deliveryReserved", true))
                .send()
                .join();
    }
}
