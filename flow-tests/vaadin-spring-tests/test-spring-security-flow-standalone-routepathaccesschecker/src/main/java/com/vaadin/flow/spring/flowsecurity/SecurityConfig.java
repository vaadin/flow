/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.spring.flowsecurity;

import jakarta.servlet.ServletContext;

import java.security.Principal;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AuthorizationManagerWebInvocationPrivilegeEvaluator;
import org.springframework.security.web.access.PathPatternRequestTransformer;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.service.UserInfoService;
import com.vaadin.flow.spring.flowsecurity.views.LoginView;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.SpringAccessPathChecker;
import com.vaadin.flow.spring.security.UidlRedirectStrategy;

import static com.vaadin.flow.spring.flowsecurity.service.UserInfoService.ROLE_ADMIN;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private VaadinConfigurationProperties vaadinConfigurationProperties;

    @Autowired
    private RequestUtil requestUtil;

    private final AuthenticationContext authenticationContext = new AuthenticationContext();

    @Bean
    public AuthenticationContext authenticationContext() {
        return authenticationContext;
    }

    @Bean
    static NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
        return new NavigationAccessControlConfigurer()
                .withLoginView(LoginView.class).withRoutePathAccessChecker();
    }

    @Bean
    AuthorizationManagerWebInvocationPrivilegeEvaluator.HttpServletRequestTransformer customRequestTransformer() {
        return SpringAccessPathChecker.principalAwareRequestTransformer(
                new PathPatternRequestTransformer());
    }

    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http,
            AuthenticationContext authenticationContext) throws Exception {
        // Setup
        http.csrf(AbstractHttpConfigurer::disable); // simple for testing
        // purpose

        // Homemade security for Vaadin application, not fully functional as the
        // configuration provided by VaadinSecurityConfigurer
        // @formatter:off
        http.authorizeHttpRequests(auth -> auth
                // Ensures that SpringPathAccessChecker does not fail when matchers get Principal from HTTP request
                .requestMatchers(request -> {
                    Principal principal = request.getUserPrincipal();
                    if (principal == null) {
                        // Do nothing, just avoid IDE complain about not used variable
                    }
                    return false; // no need to match rule, we just want to access principal.
                }).denyAll()
                // Permit access to static resources
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .permitAll()
                // Permit access to vaadin's internal communication
                .requestMatchers(request -> HandlerHelper
                        .isFrameworkInternalRequest("/*", request))
                .permitAll()
                .requestMatchers(requestUtil::isAnonymousRoute)
                .permitAll()
                // Permit technical access to vaadin's static files
                .requestMatchers("/VAADIN/**").permitAll()
                // custom request matchers. using 'routeAwareAntMatcher' to
                // allow checking route and alias paths against patterns
                .requestMatchers("/admin-only/**", "/admin")
                .hasAnyRole(ROLE_ADMIN)
                .requestMatchers("/private")
                .authenticated()
                .requestMatchers("/", "/public/**", "/another")
                .permitAll()

                .requestMatchers("/error")
                .permitAll()
                // routes aliases
                .requestMatchers("/alias-for-admin")
                .hasAnyRole(ROLE_ADMIN)
                .requestMatchers("/home", "/hey/**")
                .permitAll()
                .requestMatchers("/all-logged-in/**", "/passthrough/**")
                .authenticated()
        );
        // @formatter:on
        http.logout(cfg -> {
            SimpleUrlLogoutSuccessHandler logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
            logoutSuccessHandler.setDefaultTargetUrl(getLogoutSuccessUrl());
            logoutSuccessHandler
                    .setRedirectStrategy(new UidlRedirectStrategy());
            cfg.logoutSuccessHandler(logoutSuccessHandler);
            cfg.addLogoutHandler((request, response, authentication) -> {
                UI ui = UI.getCurrent();
                ui.accessSynchronously(() -> ui.getPage().setLocation(
                        UrlUtil.getServletPathRelative(getLogoutSuccessUrl(),
                                request)));
            });
        });
        // Custom login page with form authentication
        http.formLogin(cfg -> cfg.loginPage("/my/login/page").permitAll());
        DefaultSecurityFilterChain filterChain = http.build();
        // Test application uses AuthenticationContext, configure it with
        // the logout handlers
        AuthenticationContext.applySecurityConfiguration(http,
                authenticationContext);

        return filterChain;
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

}
