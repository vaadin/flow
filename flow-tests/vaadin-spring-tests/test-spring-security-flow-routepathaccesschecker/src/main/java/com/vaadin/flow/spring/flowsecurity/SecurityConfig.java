package com.vaadin.flow.spring.flowsecurity;

import jakarta.servlet.ServletContext;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.service.UserInfoService;
import com.vaadin.flow.spring.flowsecurity.views.LoginView;
import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import com.vaadin.flow.spring.security.VaadinWebSecurity;

import static com.vaadin.flow.spring.flowsecurity.service.UserInfoService.ROLE_ADMIN;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private VaadinConfigurationProperties vaadinConfigurationProperties;

    @Bean
    static NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
        return new NavigationAccessControlConfigurer()
                .withRoutePathAccessChecker();
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

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http.authorizeHttpRequests(cfg -> cfg
                .requestMatchers(antMatchers("/admin-only/**", "/admin"))
                    .hasAnyRole(ROLE_ADMIN)
                .requestMatchers(antMatchers("/private"))
                    .authenticated()
                .requestMatchers(antMatchers("/", "/public/**", "/another", "/menu-list"))
                    .permitAll()
                .requestMatchers(antMatchers("/error"))
                    .permitAll()
                // routes aliases
                .requestMatchers(antMatchers("/alias-for-admin"))
                    .hasAnyRole(ROLE_ADMIN)
                .requestMatchers(antMatchers("/home", "/hey/**"))
                    .permitAll()
                .requestMatchers(antMatchers("/all-logged-in/**"))
                    .authenticated()
        );
        // @formatter:on

        super.configure(http);
        if (getLogoutSuccessUrl().equals("/")) {
            // Test the default url with empty context path
            setLoginView(http, LoginView.class);
        } else {
            setLoginView(http, LoginView.class, getLogoutSuccessUrl());
        }
        http.logout(cfg -> cfg
                .addLogoutHandler((request, response, authentication) -> {
                    UI ui = UI.getCurrent();
                    ui.accessSynchronously(() -> ui.getPage()
                            .setLocation(UrlUtil.getServletPathRelative(
                                    getLogoutSuccessUrl(), request)));
                }));
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
