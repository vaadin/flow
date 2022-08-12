package com.vaadin.flow.spring.flowsecurity;

import javax.servlet.ServletContext;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.data.UserInfoRepository;
import com.vaadin.flow.spring.flowsecurity.views.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends VaadinWebSecurity {

    public static String ROLE_USER = "user";
    public static String ROLE_ADMIN = "admin";
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private VaadinConfigurationProperties vaadinConfigurationProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
    @Override
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/admin-only/**")
                .hasAnyRole(ROLE_ADMIN);
        http.authorizeRequests().antMatchers("/public/**").permitAll();
        setLoginView(http, LoginView.class, getLogoutSuccessUrl());
        return super.filterChain(http);
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager() {
            @Override
            public UserDetails loadUserByUsername(String username)
                    throws UsernameNotFoundException {
                UserInfo userInfo = userInfoRepository.findByUsername(username);
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
