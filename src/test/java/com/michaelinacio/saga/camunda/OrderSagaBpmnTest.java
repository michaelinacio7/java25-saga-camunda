package com.michaelinacio.saga.camunda;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class OrderSagaBpmnTest {
    @Test
    void bpmnContainsRunnableZeebeTasksAndCompensationPath() throws Exception {
        var resource = new ClassPathResource("processes/order-saga.bpmn");
        assertTrue(resource.exists(), "BPMN resource must be packaged");

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document;
        try (var input = resource.getInputStream()) {
            document = factory.newDocumentBuilder().parse(input);
        }

        NodeList tasks = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "serviceTask");
        Set<String> ids = IntStream.range(0, tasks.getLength())
                .mapToObj(i -> (Element) tasks.item(i))
                .map(e -> e.getAttribute("id"))
                .collect(Collectors.toSet());

        assertTrue(ids.containsAll(Set.of("ReserveInventory", "ChargePayment", "ReserveDelivery",
                "RefundPayment", "ReleaseInventory")));
        assertEquals(5, tasks.getLength(), "Every service task must have a known job type");

        NodeList errors = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "error");
        assertEquals(1, errors.getLength());
        assertEquals("ORDER_STEP_FAILED", ((Element) errors.item(0)).getAttribute("errorCode"));

        NodeList boundaryEvents = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "boundaryEvent");
        assertEquals(1, boundaryEvents.getLength());
        assertEquals("ReserveDelivery", ((Element) boundaryEvents.item(0)).getAttribute("attachedToRef"));
    }
}