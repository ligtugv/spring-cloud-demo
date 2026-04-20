package com.example.uaa.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.naming.CommunicationException;
import java.util.ArrayList;
import java.util.List;

public class CompositeAuthenticationProvider implements AuthenticationProvider {

    private final List<AuthenticationProvider> providers = new ArrayList<>();

    public CompositeAuthenticationProvider addProvider(AuthenticationProvider provider) {
        providers.add(provider);
        return this;
    }

    private static boolean isCommunicationException(Throwable t) {
        while (t != null) {
            if (t instanceof CommunicationException) return true;
            t = t.getCause();
        }
        return false;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AuthenticationException lastException = null;

        for (AuthenticationProvider provider : providers) {
            try {
                Authentication result = provider.authenticate(authentication);
                if (result != null) {
                    return result;
                }
            } catch (AuthenticationException e) {
                lastException = e;
            } catch (Exception e) {
                // LDAP server unreachable or any other unexpected exception — skip this provider
                // so in-memory DAO provider can still authenticate
                if (isCommunicationException(e)) {
                    // LDAP unreachable — silently skip
                } else {
                    // Unexpected non-auth exception — log and skip
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new AuthenticationException("No authentication provider could authenticate the request") {};
    }

    @Override
    public boolean supports(Class<?> authentication) {
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(authentication)) {
                return true;
            }
        }
        return false;
    }
}
