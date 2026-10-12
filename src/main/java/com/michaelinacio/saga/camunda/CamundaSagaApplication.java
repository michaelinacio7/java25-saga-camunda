package com.michaelinacio.saga.camunda;

import io.camunda.client.CamundaClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CamundaSagaApplication {
    public static void main(String[] args) {
        SpringApplication.run(CamundaSagaApplication.class, args);
    }

    @Bean
    CommandLineRunner deployOrderSaga(CamundaClient client) {
        return args -> client.newDeployResourceCommand()
                .addResourceFromClasspath("processes/order-saga.bpmn")
                .send()
                .join();
    }
}
