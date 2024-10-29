package org.example;

import io.camunda.zeebe.client.ZeebeClient;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        ConfigLoader configLoader = new ConfigLoader("src/main/resources/config.properties");
        var credentialsProvider = configLoader.getCredentialsProvider();

        try (var client = ZeebeClient.newClientBuilder()
                .preferRestOverGrpc(true)
                .restAddress(URI.create(configLoader.getProperty("ZEEBE_REST_ADDRESS")))
                .credentialsProvider(credentialsProvider)
                .build()) {
            deployResources(client);

            for (int i = 0; i < 5; i++) {
                long processInstanceKey = createProcessInstance(client);
                correlateOrPublishMessage(client, processInstanceKey);
            }
        }
    }

    private static void deployResources(ZeebeClient client) {
        long deploymentKey = client.newDeployResourceCommand()
                .addResourceFromClasspath("bank loan origination and processing.bpmn")
                .addResourceFromClasspath("calculate loan results.dmn")
                .addResourceFromClasspath("loan application form.form")
                .addResourceFromClasspath("loan underwriting form.form")
                .send().join().getKey();
        System.out.println("Deployed process definition with key: " + deploymentKey);
    }

    private static long createProcessInstance(ZeebeClient client) {
        Map<String, Object> var = new HashMap<>();
        var.put("first_name", "Laya");
        var.put("last_name", "Williams");
        var.put("email", "laya.williams@mail.mail");
        var.put("ticketId", "134-31-1679");

        var event = client.newCreateInstanceCommand()
                .bpmnProcessId("loanOrigination")
                .latestVersion()
                .variables(var)
                .send()
                .join();
        System.out.println("Process instance created with key: " + event.getProcessInstanceKey());
        return event.getProcessInstanceKey();
    }

    private static void correlateOrPublishMessage(ZeebeClient client, long correlationKey) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("creditScore", 760);

        try {
            long instanceKey = client.newCorrelateMessageCommand()
                    .messageName("customerInformation")
                    .correlationKey("134-31-1679")
                    .variables(variables)
                    .send().join().getProcessInstanceKey();
            System.out.println("Message correlated to instance: " + instanceKey);
        } catch (Exception e) {
            long messageKey = client.newPublishMessageCommand()
                    .messageName("customerInformation")
                    .correlationKey("134-31-1679")
                    .variables(variables)
                    .timeToLive(Duration.ofHours(1))
                    .send().join().getMessageKey();
            System.out.println("Message published with key: " + messageKey);
        }
    }
}