package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class GetPrice {

    private Double price; // Declare the price variable as Double

    // Method to retrieve the price from the API
    public void fetchPrice() {
        try {
            // Base API endpoint
            String baseUrl = "https://635684e19243cf412f86c4ec.mockapi.io/api/v1/totalPrice/";

            // Generate a random number between 1 and 50 (assuming there are 50 total prices)
            Random random = new Random();
            int randomNumber = random.nextInt(50) + 1;  // Random number between 1 and 50

            // Complete API URL with the random number
            String apiUrl = baseUrl + randomNumber;

            // Create HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // Create an HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if response is successful
            if (response.statusCode() == 200) {
                // Parse the JSON response
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(response.body());

                // Get the "price" field from the JSON response and convert it to Double
                String priceString = jsonResponse.get("price").asText();
                this.price = Double.valueOf(priceString.split("\\.")[0]); // Extracts the integer part of price

                System.out.println("Price (from ID " + randomNumber + "): " + this.price);
            } else {
                System.out.println("Failed to retrieve data. HTTP Status: " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Getter for the price variable
    public Double getPrice() {
        return price;
    }
}