package com.example.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Controller
public class LoginController {

    @Value("${app.gateway-url:http://localhost:7573}")
    private String gatewayUrl;

    @Value("${app.uaa-url:http://localhost:9000}")
    private String uaaUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SecurityContextRepository securityContextRepository =
            new org.springframework.security.web.context.HttpSessionSecurityContextRepository();

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        String username = (String) session.getAttribute("username");
        String roles = (String) session.getAttribute("roles");

        System.out.println("=== home() called ===");
        System.out.println("  session.getId() = " + session.getId());
        System.out.println("  token = " + (token != null ? token.substring(0, 30) + "..." : "NULL"));
        System.out.println("  username = " + username);
        System.out.println("  roles = " + roles);
        System.out.println("  isLoggedIn = " + (token != null));

        model.addAttribute("username", username != null ? username : "");
        model.addAttribute("roles", roles != null ? roles : "[]");
        model.addAttribute("token", token != null ? token : "");
        model.addAttribute("gatewayUrl", gatewayUrl);
        model.addAttribute("isLoggedIn", token != null);

        return "index";
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "tab", required = false, defaultValue = "password") String tab,
            Model model) {
        model.addAttribute("error", error != null ? "Invalid username or password" : null);
        model.addAttribute("logout", logout != null);
        model.addAttribute("activeTab", tab);
        model.addAttribute("gatewayUrl", gatewayUrl);
        return "login";
    }

    public static class LoginForm {
        private String username;
        private String password;
        private String redirect;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRedirect() { return redirect; }
        public void setRedirect(String redirect) { this.redirect = redirect; }
    }

    @PostMapping("/login/password")
    public String loginPassword(
            @ModelAttribute LoginForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Model model) {

        String username = form.getUsername();
        String password = form.getPassword();

        System.out.println("=== loginPassword() called ===");
        System.out.println("  session.getId() = " + session.getId());
        System.out.println("  username = " + username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Username and password are required");
            model.addAttribute("activeTab", "password");
            model.addAttribute("gatewayUrl", gatewayUrl);
            return "login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("username", username);
            params.add("password", password);
            params.add("client_id", "public-client");

            String loginUrl = uaaUrl + "/auth/login";
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    loginUrl, new HttpEntity<>(params, headers), Map.class);

            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<String, Object> body = resp.getBody();
                String token = (String) body.get("access_token");
                List<String> roles = (List<String>) body.get("roles");

                System.out.println("  Got JWT! token = " + token.substring(0, 30) + "...");
                System.out.println("  roles from UAA = " + roles);

                session.setAttribute("access_token", token);
                session.setAttribute("refresh_token", body.get("refresh_token"));
                session.setAttribute("username", username);

                String rolesJson = "[\"" + String.join("\",\"", roles) + "\"]";
                session.setAttribute("roles", rolesJson);
                System.out.println("  Session attributes SET. sessionId = " + session.getId());

                List<GrantedAuthority> authorities = extractAuthorities(body);
                Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth);
                SecurityContextHolder.setContext(context);
                securityContextRepository.saveContext(context, request, response);

                String redirectTo = form.getRedirect() != null ? form.getRedirect() : "/";
                System.out.println("  Redirecting to: " + redirectTo);
                return "redirect:" + redirectTo;
            }
        } catch (HttpClientErrorException e) {
            System.out.println("  Login failed: " + e.getStatusCode());
            model.addAttribute("error", "Invalid credentials");
            model.addAttribute("activeTab", "password");
            model.addAttribute("gatewayUrl", gatewayUrl);
            return "login";
        }

        model.addAttribute("error", "Login failed");
        model.addAttribute("activeTab", "password");
        model.addAttribute("gatewayUrl", gatewayUrl);
        return "login";
    }

    @PostMapping("/login/ldap")
    public String loginLdap(
            @ModelAttribute LoginForm form,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            Model model) {

        String username = form.getUsername();
        String password = form.getPassword();

        System.out.println("=== loginLdap() called ===");
        System.out.println("  session.getId() = " + session.getId());
        System.out.println("  username = " + username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Username and password are required");
            model.addAttribute("activeTab", "ldap");
            model.addAttribute("gatewayUrl", gatewayUrl);
            return "login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("username", username);
            params.add("password", password);
            params.add("client_id", "public-client");

            String loginUrl = uaaUrl + "/auth/login";
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    loginUrl, new HttpEntity<>(params, headers), Map.class);

            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                Map<String, Object> body = resp.getBody();
                String token = (String) body.get("access_token");
                List<String> roles = (List<String>) body.get("roles");

                System.out.println("  Got JWT! token = " + token.substring(0, 30) + "...");
                System.out.println("  roles from UAA = " + roles);

                session.setAttribute("access_token", token);
                session.setAttribute("refresh_token", body.get("refresh_token"));
                session.setAttribute("username", username);

                String rolesJson = "[\"" + String.join("\",\"", roles) + "\"]";
                session.setAttribute("roles", rolesJson);
                System.out.println("  Session attributes SET. sessionId = " + session.getId());

                List<GrantedAuthority> authorities = extractAuthorities(body);
                Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth);
                SecurityContextHolder.setContext(context);
                securityContextRepository.saveContext(context, request, response);

                String redirectTo = form.getRedirect() != null ? form.getRedirect() : "/";
                System.out.println("  Redirecting to: " + redirectTo);
                return "redirect:" + redirectTo;
            }
        } catch (HttpClientErrorException e) {
            System.out.println("  LDAP login failed: " + e.getStatusCode());
            model.addAttribute("error", "LDAP login failed");
            model.addAttribute("activeTab", "ldap");
            model.addAttribute("gatewayUrl", gatewayUrl);
            return "login";
        }

        model.addAttribute("error", "Login failed");
        model.addAttribute("activeTab", "ldap");
        model.addAttribute("gatewayUrl", gatewayUrl);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/error")
    public String errorPage(HttpServletRequest request, Model model) {
        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        String error = (String) request.getAttribute("jakarta.servlet.error.exception_type");

        model.addAttribute("status", status);
        model.addAttribute("message", message != null ? message : "Unknown error");
        model.addAttribute("error", error != null ? error : (status != null ? "HTTP " + status : "Error"));
        model.addAttribute("gatewayUrl", gatewayUrl);

        return "error";
    }

    @PostMapping("/api/products")
    @ResponseBody
    public ResponseEntity<?> createProduct(
            @RequestBody Map<String, String> product,
            HttpSession session) {

        String token = (String) session.getAttribute("access_token");
        System.out.println("=== createProduct() called ===");
        System.out.println("  session.getId() = " + session.getId());
        System.out.println("  token = " + (token != null ? token.substring(0, 30) + "..." : "NULL"));

        if (token == null) {
            System.out.println("  -> 401 NO TOKEN");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.parseMediaType("application/json; charset=utf-8"));

            String url = gatewayUrl + "/product/api/products";
            System.out.println("  Calling: " + url);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(product, headers), Map.class);
            System.out.println("  Response: " + response.getStatusCode());
            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            System.out.println("  -> " + e.getStatusCode() + " " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to create product"));
        }
    }

    @GetMapping("/api/products")
    @ResponseBody
    public ResponseEntity<?> listProducts(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        System.out.println("=== listProducts() called ===");
        System.out.println("  session.getId() = " + session.getId());
        System.out.println("  token = " + (token != null ? token.substring(0, 30) + "..." : "NULL"));

        if (token == null) {
            System.out.println("  -> 401 NO TOKEN");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/product/api/products";
            System.out.println("  Calling: " + url);
            ResponseEntity<?> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
            System.out.println("  Response: " + response.getStatusCode());
            return response;
        } catch (HttpClientErrorException e) {
            System.out.println("  -> " + e.getStatusCode() + " " + e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to fetch products"));
        }
    }

    @DeleteMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        System.out.println("=== deleteProduct(" + id + ") ===");
        System.out.println("  token = " + (token != null ? "present" : "NULL"));

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/product/api/products/" + id;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to delete product"));
        }
    }

    @PostMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<?> cartAdd(@RequestBody Map<String, Object> body, HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = gatewayUrl + "/cart/api/cart";
            return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to add to cart"));
        }
    }

    @GetMapping("/api/cart")
    @ResponseBody
    public ResponseEntity<?> cartList(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/cart/api/cart";
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to fetch cart"));
        }
    }

    @DeleteMapping("/api/cart/{productId}")
    @ResponseBody
    public ResponseEntity<?> cartRemove(@PathVariable Long productId, HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/cart/api/cart/" + productId;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to remove from cart"));
        }
    }

    @PostMapping("/api/cart/checkout")
    @ResponseBody
    public ResponseEntity<?> cartCheckout(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/cart/api/cart/checkout";
            return restTemplate.postForEntity(url, new HttpEntity<>(headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Checkout failed"));
        }
    }

    @GetMapping("/api/cart/purchases")
    @ResponseBody
    public ResponseEntity<?> cartPurchases(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/cart/api/cart/purchases";
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to fetch purchases"));
        }
    }

    @PostMapping("/api/products/{id}/add-stock")
    @ResponseBody
    public ResponseEntity<?> addStock(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = gatewayUrl + "/product/api/products/" + id + "/add-stock";
            return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/cart/{productId}")
    @ResponseBody
    public ResponseEntity<?> cartUpdate(@PathVariable Long productId, @RequestBody Map<String, Object> body, HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = gatewayUrl + "/cart/api/cart/" + productId;
            return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to update cart"));
        }
    }

    @GetMapping("/api/stats/products/by-day")
    @ResponseBody
    public ResponseEntity<?> statsByDay(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/product/api/stats/products/by-day";
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to fetch stats"));
        }
    }

    @GetMapping("/api/stats/products/by-creator")
    @ResponseBody
    public ResponseEntity<?> statsByCreator(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/product/api/stats/products/by-creator";
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to fetch stats"));
        }
    }

    @GetMapping("/api/cart/stats/users")
    @ResponseBody
    public ResponseEntity<?> cartStatsUsers(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/cart/api/cart/stats/users";
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to fetch cart stats"));
        }
    }

    @GetMapping("/api/cart/stats/products")
    @ResponseBody
    public ResponseEntity<?> cartStatsProducts(HttpSession session) {
        String token = (String) session.getAttribute("access_token");
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            String url = gatewayUrl + "/cart/api/cart/stats/products";
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Failed to fetch cart stats"));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractRoles(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                if (payload.contains("\"roles\"")) {
                    int start = payload.indexOf("\"roles\"") + 8;
                    int end = payload.indexOf("]", start) + 1;
                    return payload.substring(start, end);
                }
            }
        } catch (Exception ignored) {}
        return "[]";
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractAuthorities(Map<String, Object> body) {
        List<String> roles = (List<String>) body.get("roles");
        if (roles == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return roles.stream()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }
}
