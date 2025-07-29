package com.vaadin.flow.spring.flowsecurity;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.flowsecurity.data.UserInfo;
import com.vaadin.flow.spring.flowsecurity.service.UserInfoService;
import com.vaadin.flow.spring.flowsecurity.views.LoginView;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.UidlRedirectStrategy;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;

import static com.vaadin.flow.spring.flowsecurity.service.UserInfoService.ROLE_ADMIN;
import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@EnableWebSecurity
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

    public String getRootUrl() {
        return getRootUrl(true);
    }

    public String getRootUrl(boolean includeContextPath) {
        String rootUrl;
        String mapping = vaadinConfigurationProperties.getUrlMapping();
        if (RootMappedCondition.isRootMapping(mapping)) {
            rootUrl = "/";
        } else {
            rootUrl = mapping.replaceFirst("/\\*$", "/");
        }
        String contextPath = servletContext.getContextPath();
        if (includeContextPath && !"".equals(contextPath)) {
            rootUrl = contextPath + rootUrl;
        }
        return rootUrl;
    }

    public String getLogoutSuccessUrl() {
        return getRootUrl();
    }

    @Bean
    SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin-only/**").hasAnyRole(ROLE_ADMIN)
                .requestMatchers("/public/**", "/error").permitAll());

        http.authorizeHttpRequests(auth -> auth.requestMatchers("/switchUser")
                .hasAnyRole("ADMIN", "PREVIOUS_ADMINISTRATOR"));
        http.authorizeHttpRequests(
                auth -> auth.requestMatchers("/impersonate/exit")
                        .hasRole("PREVIOUS_ADMINISTRATOR"));
        http.logout(cfg -> cfg.logoutRequestMatcher(PathPatternRequestMatcher
                .pathPattern(HttpMethod.GET, getRootUrl(false) + "doLogout")));
        http.with(vaadin(), cfg -> {
            cfg.loginView(LoginView.class)
                    .logoutSuccessHandler(this::onLogoutOnNonVaadinUrl)
                    .addLogoutHandler((request, response, authentication) -> {
                        if (!request.getRequestURI().endsWith("doLogout")) {
                            UI ui = UI.getCurrent();
                            ui.accessSynchronously(() -> ui.getPage()
                                    .setLocation(UrlUtil.getServletPathRelative(
                                            getLogoutSuccessUrl(), request)));
                        }
                    });
        });
        return http.build();
    }

    public void onLogoutOnNonVaadinUrl(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!request.getRequestURI().endsWith("doLogout")) {
            return;
        }
        try {
            // Simulate long processing time
            Thread.currentThread().sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SimpleUrlLogoutSuccessHandler urlLogoutHandler = new SimpleUrlLogoutSuccessHandler();
        urlLogoutHandler.setDefaultTargetUrl(getRootUrl(false) + "logout");
        urlLogoutHandler.setRedirectStrategy(new UidlRedirectStrategy());
        urlLogoutHandler.onLogoutSuccess(request, response, authentication);
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
    @DependsOn("VaadinSecurityContextHolderStrategy")
    public SwitchUserFilter switchUserFilter() {
        SwitchUserFilter filter = new SwitchUserFilter();
        filter.setUserDetailsService(userDetailsService());
        filter.setSwitchUserMatcher(PathPatternRequestMatcher
                .pathPattern(HttpMethod.GET, "/impersonate"));
        filter.setSwitchFailureUrl("/switchUser");
        filter.setExitUserMatcher(PathPatternRequestMatcher
                .pathPattern(HttpMethod.GET, "/impersonate/exit"));
        filter.setTargetUrl("/");
        return filter;
    }

}
