package org.example;

import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;

import java.io.FileInputStream;
import java.util.Properties;

public class ConfigLoader {
    private final Properties properties;

    public ConfigLoader(String filePath) {
        properties = loadProperties(filePath);
    }

    private Properties loadProperties(String filePath) {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(filePath)) {
            props.load(input);
        } catch (Exception e) {
            System.err.println("Failed to load properties: " + e.getMessage());
        }
        return props;
    }

    public OAuthCredentialsProvider getCredentialsProvider() {
        return new OAuthCredentialsProviderBuilder()
                .authorizationServerUrl(properties.getProperty("OAUTH_API"))
                .audience(properties.getProperty("AUDIENCE"))
                .clientId(properties.getProperty("CLIENT_ID"))
                .clientSecret(properties.getProperty("CLIENT_SECRET"))
                .build();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
