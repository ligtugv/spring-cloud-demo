package com.example.uaa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

@Configuration
public class LdapConfig {

    private static final Logger log = LoggerFactory.getLogger(LdapConfig.class);

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ldap.urls")
    public LdapContextSource ldapContextSource(Environment env) {
        String url = env.getProperty("spring.ldap.urls", "ldap://localhost:389");
        String base = env.getProperty("spring.ldap.base", "dc=luban-cae,dc=com");
        String managerDn = env.getProperty("spring.ldap.manager-dn", "cn=admin,dc=luban-cae,dc=com");
        String managerPassword = env.getProperty("spring.ldap.manager-password", "admin_secret");

        log.info("Configuring LDAP ContextSource: url={}, base={}, managerDn={}", url, base, managerDn);

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(url);
        contextSource.setBase(base);
        contextSource.setUserDn(managerDn);
        contextSource.setPassword(managerPassword);

        contextSource.setPooled(true);
        contextSource.afterPropertiesSet();

        log.info("LDAP ContextSource configured successfully");
        return contextSource;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ldap.urls")
    public LdapAuthenticationProvider ldapAuthenticationProvider(LdapContextSource contextSource) {
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
                "ou=users",
                "(uid={0})",
                contextSource
        );

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);

        DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(
                contextSource,
                "ou=groups"
        );
        populator.setGroupSearchFilter("(member={0})");
        populator.setRolePrefix("ROLE_");
        populator.setSearchSubtree(true);
        populator.setConvertToUpperCase(true);

        log.info("LDAP AuthenticationProvider configured with userSearch base=ou=users, filter=(uid={{0}}), groupSearch base=ou=groups");

        return new LdapAuthenticationProvider(authenticator, populator);
    }
}
