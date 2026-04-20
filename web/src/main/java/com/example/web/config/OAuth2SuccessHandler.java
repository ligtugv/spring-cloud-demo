package com.example.web.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.uaa-url:http://localhost:9000}")
    private String uaaUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        System.out.println("=== OAuth2SuccessHandler.onAuthenticationSuccess ===");
        System.out.println("uaaUrl = " + uaaUrl);
        System.out.println("Authentication: " + authentication);

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            System.out.println("Not OAuth2AuthenticationToken, redirecting without JWT");
            response.sendRedirect("/");
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        String username = oauth2User.getAttribute("login");
        System.out.println("GitHub user: " + username);

        String rolesParam = "EDITOR,USER";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("username", username);
            params.add("roles", rolesParam);

            String url = uaaUrl + "/auth/oauth2/success";
            System.out.println("Calling UAA: " + url);
            System.out.println("Roles sent to UAA: " + rolesParam);

            ResponseEntity<Map> resp = restTemplate.postForEntity(url, new HttpEntity<>(params, headers), Map.class);
            System.out.println("UAA response: " + resp.getStatusCode());

            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<String, Object> body = resp.getBody();
                String token = (String) body.get("access_token");
                List<String> roles = (List<String>) body.get("roles");

                System.out.println("Got JWT token, roles: " + roles);

                request.getSession().setAttribute("access_token", token);
                request.getSession().setAttribute("refresh_token", body.get("refresh_token"));
                request.getSession().setAttribute("username", username);

                String rolesJson = "[\"" + String.join("\",\"", roles) + "\"]";
                request.getSession().setAttribute("roles", rolesJson);
                System.out.println("Session attributes set. Redirecting to /");
            }
        } catch (Exception e) {
            System.err.println("Failed to get JWT from UAA: " + e.getMessage());
            request.getSession().setAttribute("username", username);
            request.getSession().setAttribute("oauth2_fallback", "true");
        }

        response.sendRedirect("/");
    }
}
