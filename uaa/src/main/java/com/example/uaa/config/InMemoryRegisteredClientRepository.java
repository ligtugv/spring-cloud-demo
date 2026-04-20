package com.example.uaa.config;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.*;

public class InMemoryRegisteredClientRepository implements RegisteredClientRepository {

    private final Map<String, RegisteredClient> clients = new LinkedHashMap<>();

    public InMemoryRegisteredClientRepository(List<RegisteredClient> clients) {
        clients.forEach(c -> this.clients.put(c.getId(), c));
    }

    @Override
    public RegisteredClient findById(String id) {
        return clients.get(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clients.values().stream()
                .filter(c -> c.getClientId().equals(clientId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        clients.put(registeredClient.getId(), registeredClient);
    }
}
