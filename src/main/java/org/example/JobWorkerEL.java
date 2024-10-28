package org.example;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;

import java.net.URI;
import java.time.Duration;
import java.util.Scanner;

public class JobWorkerEL {

    public static void main(String[] args) {

        ConfigLoader configLoader = new ConfigLoader("src/main/resources/config.properties");
        var credentialsProvider = configLoader.getCredentialsProvider();

        try (ZeebeClient client = createZeebeClient(credentialsProvider, configLoader)) {
            openJobWorkers(client);
        }
    }

    private static ZeebeClient createZeebeClient(OAuthCredentialsProvider credentialsProvider, ConfigLoader configLoader) {
        return ZeebeClient.newClientBuilder()
                .preferRestOverGrpc(true)
                .restAddress(URI.create(configLoader.getProperty("ZEEBE_REST_ADDRESS")))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    private static void openJobWorkers(ZeebeClient client) {
        final String executionListenerType = "endEl";
        final String jobWorkerType = "job";

        System.out.println("Opening job workers.");

        try (JobWorker executionListenerWorker = createJobWorker(client, executionListenerType, new ExecutionListenerHandler());
             JobWorker exampleWorker = createJobWorker(client, jobWorkerType, new ExampleJobHandler())) {

            System.out.println("Job workers opened and receiving jobs.");
            waitUntilSystemInput("exit");
        }
    }

    private static JobWorker createJobWorker(ZeebeClient client, String jobType, JobHandler handler) {
        return client.newWorker()
                .jobType(jobType)
                .handler(handler)
                .timeout(Duration.ofSeconds(10))
                .open();
    }

    private static void waitUntilSystemInput(final String exitCode) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().equalsIgnoreCase(exitCode)) {
                    return;
                }
            }
        }
    }

    public static class Order {
        private long orderId;
        private double totalPrice;

        public long getOrderId() {
            return orderId;
        }

        public void setOrderId(final long orderId) {
            this.orderId = orderId;
        }

        public double getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(final double totalPrice) {
            this.totalPrice = totalPrice;
        }
    }

    private static class ExecutionListenerHandler implements JobHandler {
        @Override
        public void handle(final JobClient client, final ActivatedJob job) {
            Order order = job.getVariablesAsType(Order.class);
            System.out.println("New job with orderId: " + order.getOrderId() + " for process instance: " + job.getProcessInstanceKey());

            GetPrice getPriceInstance = new GetPrice();
            getPriceInstance.fetchPrice();
            double price = getPriceInstance.getPrice();

            System.out.println("The price fetched is: " + price);
            order.setTotalPrice(price);

            client.newCompleteCommand(job.getKey()).variables(order).send().join();
        }
    }

    private static class ExampleJobHandler implements JobHandler {
        @Override
        public void handle(final JobClient client, final ActivatedJob job) {
            System.out.println("Handling job: " + job.getKey());
            client.newCompleteCommand(job.getKey()).send().join();
        }
    }
}