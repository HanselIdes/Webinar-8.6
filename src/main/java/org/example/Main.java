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

            for (int i = 0; i < 100; i++) {
                createProcessInstance(client, i);
                for (int k = 0; k < 2; k++) {
                    correlateOrPublishMessage(client, i);
                }
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
        System.out.println("Deployed process resources with key: " + deploymentKey);
    }

    private static void createProcessInstance(ZeebeClient client, Integer i) {
        Map<String, Object> var = new HashMap<>();
        var.put("first_name", "Laya");
        var.put("last_name", "Williams");
        var.put("email", "another@demo.org");
        var.put("customerId", "A-"+i.toString());
        var.put("annual_salary", 55200);
        var.put("tenure", 36);

        var event = client.newCreateInstanceCommand()
                .bpmnProcessId("loanOrigination")
                .latestVersion()
                .variables(var)
                .send()
                .join();
        System.out.println(i+". Process instance created with key: " + event.getProcessInstanceKey());
    }

    private static void correlateOrPublishMessage(ZeebeClient client, Integer correlationKey) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("creditScore", 760);

        Map<String, Object> var = new HashMap<>();
        var.put("current_employer", "Styles and Speed Printing LLC.");
        var.put("monthly_debt", 1234);

        try {
            long instanceKey = client.newCorrelateMessageCommand()
                    .messageName("customerInformation")
                    .correlationKey("A-"+correlationKey.toString())
                    .variables(variables)
                    .send().join().getProcessInstanceKey();
            System.out.println(correlationKey+". Message correlated to instance: " + instanceKey);
        } catch (Exception e) {
            long messageKey = client.newPublishMessageCommand()
                    .messageName("customerInformation")
                    .correlationKey("A-"+correlationKey.toString())
                    .variables(var)
                    .timeToLive(Duration.ofMinutes(10))
                    .send().join().getMessageKey();
            System.out.println(correlationKey+". Message published with key: " + messageKey);
        }
    }
}