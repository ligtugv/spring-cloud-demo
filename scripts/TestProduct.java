import java.net.http.*;
import java.net.URI;
import java.nio.file.*;

public class TestProduct {
    public static void main(String[] args) throws Exception {
        String token = "eyJraWQiOiIwYjUwYmZmNC00NmJjLTRiZTYtOTE4OC00NWYwZDIzNjZjNDAiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1fMSIsImF1ZCI6InB1YmxpYy1jbGllbnQiLCJyb2xlcyI6WyJFRElUT1IiLCJQUk9EVUNUX0FETUlOIiwiVVNFUiJdLCJzY29wZSI6InJlYWQgd3JpdGUgdXNlciBwcm9kdWN0OnJlYWQgcHJvZHVjdDp3cml0ZSBwcm9kdWN0OmRlbGV0ZSIsImlzcyI6Imh0dHA6Ly91YWE6OTAwMCIsImV4cCI6MTc3NjUxMDc2NSwiaWF0IjoxNzc2NTAzNTY1LCJqdGkiOiI5OWMyM2YzMy0zY2EyLTQ4OGUtYjEwNS05Nzc4ZTUxM2JkZTQiLCJhdXRob3JpdGllcyI6WyJST0xFX0VESVRPUiIsIlJPTEVfUFJPRFVDVF9BRE1JTiIsIlJPTEVfVVNFUiJdfQ.PFcFs8PJhB7pbBDATTQxjYt1Ez4etAQrnww7FQmk3zODnAsxfCK6x5_NWCQtf_RZBhpaBuGkmwsP7l2rH0B4t9BAl9Wvk4btTJIAqTdhIGsG0C3KsMBoAtCkdcihcCYDZ6CsdFoMmgRnF-bwPdAho36nnmHkHCFT000K3CTwL1VL7aZNIrjfsWGpKrF7eqtbqng0Outk4YE863fVRXyS0OiTJ5nJthWqgwUhHWEf8zxwaqb_FK_cLU6CfE9PvBNy7ZjXxARSpUdfo5qRSQR4C8qCcOpExqqX313wMg9sd7cveljGDG25uzEK1lSKYo1y4dJ11jpE_ZJ5T0kYUslY8Q";

        // Test GET /api/products
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8082/api/products"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("GET /api/products");
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
        System.out.println();

        // Test POST /api/products
        String json = "{\"name\":\"Test Laptop\"}";
        HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8082/api/products"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("POST /api/products");
        System.out.println("Status: " + postResponse.statusCode());
        System.out.println("Body: " + postResponse.body());
    }
}
