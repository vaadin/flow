package com.vaadin.flow.spring.flowsecurity;

import jakarta.servlet.ServletContext;

import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.service.UserInfoService;
import com.vaadin.flow.spring.flowsecurity.views.LoginView;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;

import static com.vaadin.flow.spring.flowsecurity.service.UserInfoService.ROLE_ADMIN;
import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = false, jsr250Enabled = true, securedEnabled = true)
@Configuration
@Profile("default")
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfig {

    private final UserInfoService userInfoService;

    private final ServletContext servletContext;

    private final VaadinConfigurationProperties vaadinConfigurationProperties;

    public SecurityConfig(UserInfoService userInfoService,
            ServletContext servletContext,
            VaadinConfigurationProperties vaadinConfigurationProperties) {
        this.userInfoService = userInfoService;
        this.servletContext = servletContext;
        this.vaadinConfigurationProperties = vaadinConfigurationProperties;
    }

    public String getLogoutSuccessUrl() {
        String logoutSuccessUrl;
        String mapping = vaadinConfigurationProperties.getUrlMapping();
        if (RootMappedCondition.isRootMapping(mapping)) {
            logoutSuccessUrl = "/";
        } else {
            logoutSuccessUrl = mapping.replaceFirst("/\\*$", "/");
        }
        String contextPath = servletContext.getContextPath();
        if (!"".equals(contextPath)) {
            logoutSuccessUrl = contextPath + logoutSuccessUrl;
        }
        return logoutSuccessUrl;
    }

    @Bean
    SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http.authorizeHttpRequests(auth -> auth

                .requestMatchers(
                        PathPatternRequestMatcher.pathPattern("/admin-only/**"))
                .hasAnyRole(ROLE_ADMIN)
                .requestMatchers(
                        PathPatternRequestMatcher.pathPattern("/public/**"))
                .permitAll());
        http.with(vaadin(), cfg -> {
            String logoutSuccessUrl = getLogoutSuccessUrl();
            if (logoutSuccessUrl.equals("/")) {
                cfg.loginView(LoginView.class);
            } else {
                cfg.loginView(LoginView.class, logoutSuccessUrl);
            }
            cfg.addLogoutHandler((request, response, authentication) -> {
                UI ui = UI.getCurrent();
                ui.accessSynchronously(() -> ui.getPage().setLocation(
                        UrlUtil.getServletPathRelative(getLogoutSuccessUrl(),
                                request)));
            });
        });
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager() {
            @Override
            public UserDetails loadUserByUsername(String username)
                    throws UsernameNotFoundException {
                UserInfo userInfo = userInfoService.findByUsername(username);
                if (userInfo == null) {
                    throw new UsernameNotFoundException(
                            "No user present with username: " + username);
                } else {
                    return new User(userInfo.getUsername(),
                            userInfo.getEncodedPassword(),
                            userInfo.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(
                                            "ROLE_" + role))
                                    .collect(Collectors.toList()));
                }
            }
        };
    }

    @Bean
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        return new DefaultMethodSecurityExpressionHandler();
    }

}
