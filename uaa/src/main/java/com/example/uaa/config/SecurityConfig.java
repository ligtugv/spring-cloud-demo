package com.example.uaa.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String FIXED_KID = "uaa-signing-key-v1";

    @Bean
    public RSAKeyHolder rsaKeyHolder() throws Exception {
        return new RSAKeyHolder(loadRsaKey());
    }

    private RSAKey loadRsaKey() throws Exception {
        String privatePem = readResource("/private.pem");
        String publicPem = readResource("/public.pem");

        RSAPrivateKey privateKey = parsePrivateKey(privatePem);
        RSAPublicKey publicKey = parsePublicKey(publicPem);

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(FIXED_KID)
                .build();

        System.out.println("RSA key loaded. kid=" + FIXED_KID);
        return rsaKey;
    }

    private String readResource(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
        String content = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(content));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String content = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(content));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var user = User.builder()
                .username("user_1")
                .password(encoder.encode("user_1"))
                .roles("USER")
                .build();
        var editor = User.builder()
                .username("editor_1")
                .password(encoder.encode("editor_1"))
                .roles("EDITOR", "USER")
                .build();
        var admin = User.builder()
                .username("adm_1")
                .password(encoder.encode("adm_1"))
                .roles("PRODUCT_ADMIN", "EDITOR", "USER")
                .build();
        return new InMemoryUserDetailsManager(user, editor, admin);
    }

    @Bean
    @Primary
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder encoder,
            ObjectProvider<org.springframework.security.ldap.authentication.LdapAuthenticationProvider> ldapAuthProvider) {

        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider(userDetailsService);
        daoProvider.setPasswordEncoder(encoder);

        CompositeAuthenticationProvider composite = new CompositeAuthenticationProvider();
        composite.addProvider(daoProvider);

        org.springframework.security.ldap.authentication.LdapAuthenticationProvider ldapProvider = ldapAuthProvider.getIfAvailable();
        if (ldapProvider != null) {
            composite.addProvider(ldapProvider);
        }

        return composite;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new org.springframework.security.authentication.ProviderManager(authenticationProvider);
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKeyHolder holder) {
        JWKSet jwkSet = new JWKSet(holder.getRsaKey());
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http.exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
        );
        http.oauth2ResourceServer(res -> res.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login/**", "/oauth2/**", "/assets/**", "/error", "/error.html", "/", "/auth/**", "/.well-known/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/oauth2/**")
                        .ignoringRequestMatchers("/auth/**")
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(u -> u
                                .userService(customOAuth2UserService)
                        )
                        .authorizationEndpoint(a -> a
                                .authorizationRequestRepository(
                                        new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository()
                                )
                        )
                        .defaultSuccessUrl("/", true)
                );
        return http.build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                var principal = context.getPrincipal();
                Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
                List<String> roles = authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_"))
                        .map(a -> a.substring(5))
                        .collect(Collectors.toList());

                context.getClaims().claim("sub", principal.getName());
                context.getClaims().claim("username", principal.getName());
                context.getClaims().claim("roles", roles);
                context.getClaims().claim("authorities", authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
            }
        };
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder encoder) {
        List<RegisteredClient> clients = new ArrayList<>();

        RegisteredClient publicClient = RegisteredClient.withId("public-client")
                .clientId("public-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.JWT_BEARER)
                .redirectUri("http://localhost:18080/login/oauth2/code/public-client")
                .redirectUri("http://localhost:18080/")
                .redirectUri("http://localhost:7573/login/oauth2/code/public-client")
                .redirectUri("http://localhost:7573/")
                .redirectUri("http://localhost:7573/web/login/oauth2/code/public-client")
                .redirectUri("http://localhost:7573/web/")
                .postLogoutRedirectUri("http://localhost:7573/")
                .postLogoutRedirectUri("http://localhost:7573/web/")
                .scope("openid")
                .scope("profile")
                .scope("email")
                .scope("read")
                .scope("write")
                .scope("user")
                .scope("product:read")
                .scope("product:write")
                .scope("product:delete")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        .refreshTokenTimeToLive(Duration.ofHours(24))
                        .build())
                .build();

        RegisteredClient webApp = RegisteredClient.withId("web-app")
                .clientId("web-app")
                .clientSecret(encoder.encode("web-app-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .redirectUri("http://localhost:18080/login/oauth2/code/web-app")
                .redirectUri("http://localhost:18080/")
                .postLogoutRedirectUri("http://localhost:18080/")
                .scope("openid")
                .scope("profile")
                .scope("email")
                .scope("read")
                .scope("write")
                .scope("user")
                .scope("product:read")
                .scope("product:write")
                .scope("product:delete")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(2))
                        .refreshTokenTimeToLive(Duration.ofHours(24))
                        .build())
                .build();

        clients.add(publicClient);
        clients.add(webApp);
        return new InMemoryRegisteredClientRepository(clients);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @org.springframework.beans.factory.annotation.Value("${app.issuer:http://localhost:9000}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    public static class RSAKeyHolder {
        private final RSAKey rsaKey;

        public RSAKeyHolder(RSAKey rsaKey) {
            this.rsaKey = rsaKey;
        }

        public RSAKey getRsaKey() {
            return rsaKey;
        }

        public com.nimbusds.jose.jwk.JWK getPublicJWK() {
            return rsaKey.toPublicJWK();
        }
    }
}
