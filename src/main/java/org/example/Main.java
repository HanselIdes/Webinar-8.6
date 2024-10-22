package org.example;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Main {
    //private static final String zeebeGrpc = "grpcs://9d44c38d-cb28-4346-971d-ace24dd4ed1b.bru-2.zeebe.camunda.io:443";
    private static final String zeebeRest = "https://bru-2.zeebe.camunda.io/9d44c38d-cb28-4346-971d-ace24dd4ed1b";
    private static final String audience = "zeebe.camunda.io";
    private static final String clientId = "e_f8.cIz~6Kk~IUjOpW-f8yb-nKMEKMa";
    private static final String clientSecret = "nYO6kEtp9QHbjzh29N3iYBCyZG0pM6R8u4qOcv6ZDtvQU1aX3DZUrebjn748OWhU";
    private static final String oAuthAPI = "https://login.cloud.camunda.io/oauth/token";

    public static void main(String[] args) {
        OAuthCredentialsProvider credentialsProvider =
                new OAuthCredentialsProviderBuilder()
                        .authorizationServerUrl(oAuthAPI)
                        .audience(audience)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build();

        final ZeebeClientBuilder clientBuilder;

        clientBuilder = ZeebeClient.newClientBuilder().preferRestOverGrpc(true)
                .restAddress(URI.create(zeebeRest))
                .credentialsProvider(credentialsProvider);


        try (final ZeebeClient client = clientBuilder.build()) {

            Long deploymentKey = client.newDeployResourceCommand().addResourceFromClasspath("webinar8.6.bpmn").send().join().getKey();
            System.out.println("Deployed process definition with key: "+deploymentKey);

            System.out.println("Creating process instance");

            final ProcessInstanceEvent processInstanceEvent =
                    client
                            .newCreateInstanceCommand()
                            .bpmnProcessId("webinar")
                            .latestVersion()
                            .send()
                            .join();

            System.out.println(
                    "Process instance created with key: " + processInstanceEvent.getProcessInstanceKey());



            //
            Map<String, Object> variables = new HashMap<>();
            variables.put("orderId", processInstanceEvent.getProcessInstanceKey()+1);



            Long id = client.newCorrelateMessageCommand()
                    .messageName("abc")
                    .correlationKey("123")
                    .variables(variables)
                    .send()
                    .join().getProcessInstanceKey();

            System.out.println("Message correlated to instance: "+id);


        }


    }
}