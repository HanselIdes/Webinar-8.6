package org.example;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Scanner;

public class JobWorkerEL {

    //private static final String zeebeGrpc = "grpcs://9d44c38d-cb28-4346-971d-ace24dd4ed1b.bru-2.zeebe.camunda.io:443";
    private static final String zeebeRest = "https://bru-2.zeebe.camunda.io/9d44c38d-cb28-4346-971d-ace24dd4ed1b";
    private static final String audience = "zeebe.camunda.io";
    private static final String clientId = "e_f8.cIz~6Kk~IUjOpW-f8yb-nKMEKMa";
    private static final String clientSecret = "nYO6kEtp9QHbjzh29N3iYBCyZG0pM6R8u4qOcv6ZDtvQU1aX3DZUrebjn748OWhU";
    private static final String oAuthAPI = "https://login.cloud.camunda.io/oauth/token";

    public static void main(String[] args) {

        //create Zeebe client
        OAuthCredentialsProvider credentialsProvider =
                new OAuthCredentialsProviderBuilder()
                        .authorizationServerUrl(oAuthAPI)
                        .audience(audience)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build();


        final ZeebeClientBuilder clientBuilder = ZeebeClient.newClientBuilder().preferRestOverGrpc(true)
                .restAddress(URI.create(zeebeRest))
                .credentialsProvider(credentialsProvider);

        // create job worker
        final String jobType = "endEL";

        try (final ZeebeClient client = clientBuilder.build()) {

            System.out.println("Opening job worker.");

            try (final JobWorker workerRegistration =
                         client
                                 .newWorker()
                                 .jobType(jobType)
                                 .handler(new DemoJobHandler())
                                 .timeout(Duration.ofSeconds(10))
                                 .open()) {
                System.out.println("Job worker opened and receiving jobs.");

                // run until System.in receives exit command
                waitUntilSystemInput("exit");
            }
        }
    }

    private static void waitUntilSystemInput(final String exitCode) {
        try (final Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                final String nextLine = scanner.nextLine();
                if (nextLine.contains(exitCode)) {
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

    private static class DemoJobHandler implements JobHandler {
        @Override
        public void handle(final JobClient client, final ActivatedJob job) {
            // read the variables of the job
            final Order order = job.getVariablesAsType(Order.class);
            System.out.println("New job with orderId: " + order.getOrderId() +" for process instance: "+job.getProcessInstanceKey());

            // update the variables and complete the job

            // Create an instance of the GetPrice class
            GetPrice getPriceInstance = new GetPrice();

            // Call the fetchPrice() method to retrieve and set the price from the API
            getPriceInstance.fetchPrice();

            // Now, get the price as a Long using the getPrice() method
            Double price = getPriceInstance.getPrice();

            // Output the price
            System.out.println("The price fetched is: " + price);

            order.setTotalPrice(price);

            client.newCompleteCommand(job.getKey()).variables(order).send();
        }
    }
}
