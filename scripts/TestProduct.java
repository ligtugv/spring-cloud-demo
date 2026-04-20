import java.net.http.*;
import java.net.URI;

/**
 * Test script for Product API.
 * Usage: java TestProduct.java <username> <password>
 * Example: java TestProduct.java adm_1 adm_1
 */
public class TestProduct {
    public static void main(String[] args) throws Exception {
        String username = args.length > 0 ? args[0] : "adm_1";
        String password = args.length > 1 ? args[1] : "adm_1";
        String clientId = "public-client";

        System.out.println("=== Fetching token for user: " + username + " ===");

        HttpClient client = HttpClient.newHttpClient();

        String loginBody = String.format(
            "username=%s&password=%s&client_id=%s",
            username, password, clientId
        );

        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:7573/auth/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        if (loginResponse.statusCode() != 200) {
            System.out.println("FAILED: Could not get token. Status: " + loginResponse.statusCode());
            System.out.println("Response: " + loginResponse.body());
            System.exit(1);
        }

        String responseBody = loginResponse.body();
        String token = extractJsonString(responseBody, "access_token");

        if (token == null || token.isEmpty()) {
            System.out.println("FAILED: access_token not found in response.");
            System.out.println("Response: " + responseBody);
            System.exit(1);
        }

        System.out.println("Token obtained successfully.");
        System.out.println();

        // Test GET /api/products
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:7573/product/api/products"))
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
            .uri(URI.create("http://localhost:7573/product/api/products"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("POST /api/products");
        System.out.println("Status: " + postResponse.statusCode());
        System.out.println("Body: " + postResponse.body());
    }

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
