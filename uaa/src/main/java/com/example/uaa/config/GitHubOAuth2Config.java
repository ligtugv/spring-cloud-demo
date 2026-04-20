package com.example.uaa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
@ConditionalOnClass(ClientRegistration.class)
public class GitHubOAuth2Config {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        OAuth2ClientProperties.Registration github = properties.getRegistration().get("github");
        if (github != null) {
            registrations.add(
                    ClientRegistration.withRegistrationId("github")
                            .clientId(github.getClientId())
                            .clientSecret(github.getClientSecret())
                            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                            .scope("read:user", "user:email")
                            .authorizationUri("https://github.com/login/oauth/authorize")
                            .tokenUri("https://github.com/login/oauth/access_token")
                            .userInfoUri("https://api.github.com/user")
                            .userNameAttributeName("login")
                            .clientName("GitHub")
                            .build()
            );
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizedClientService.class)
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate =
                new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();

        return userRequest -> {
            OAuth2User oauth2User = delegate.loadUser(userRequest);

            Set<GrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities());
            authorities.add(new SimpleGrantedAuthority("ROLE_EDITOR"));
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();

            return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), userNameAttributeName);
        };
    }
}
