/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.spring.security;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.auth.ViewAccessChecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Provides basic Vaadin security configuration for the project.
 * <p>
 * Sets up security rules for a Vaadin application and restricts all URLs except
 * for public resources and internal Vaadin URLs to authenticated user.
 * <p>
 * The default behavior can be altered by extending the public/protected methods
 * in the class.
 * <p>
 * To use this, create your own web security configurer adapter class by
 * extending this class instead of <code>WebSecurityConfigurerAdapter</code> and
 * annotate it with <code>@EnableWebSecurity</code> and
 * <code>@Configuration</code>.
 * <p>
 * For example <code>
&#64;EnableWebSecurity
&#64;Configuration
public class MySecurityConfigurerAdapter extends VaadinWebSecurityConfigurerAdapter {
    
} 
 * </code>
 * 
 */
public abstract class VaadinWebSecurityConfigurerAdapter
        extends WebSecurityConfigurerAdapter {

    @Autowired
    private VaadinDefaultRequestCache vaadinDefaultRequestCache;

    @Autowired
    private RequestUtil requestUtil;

    @Autowired
    private ViewAccessChecker viewAccessChecker;

    /**
     * The paths listed as "ignoring" in this method are handled without any
     * Spring Security involvement. They have no access to any security context
     * etc.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().requestMatchers(getDefaultWebSecurityIgnoreMatcher());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Vaadin has its own CSRF protection.
        // Spring CSRF is not compatible with Vaadin internal requests
        http.csrf().ignoringRequestMatchers(
                requestUtil::isFrameworkInternalRequest);
        // nor with endpoints
        http.csrf().ignoringRequestMatchers(requestUtil::isEndpointRequest);

        // Ensure automated requests to e.g. closing push channels, service
        // workers,
        // endpoints are not counted as valid targets to redirect user to on
        // login
        http.requestCache().requestCache(vaadinDefaultRequestCache);

        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry urlRegistry = http
                .authorizeRequests();
        // Vaadin internal requests must always be allowed to allow public Flow
        // pages
        // and/or login page implemented using Flow.
        urlRegistry.requestMatchers(requestUtil::isFrameworkInternalRequest)
                .permitAll();
        // Public endpoints are OK to access
        urlRegistry.requestMatchers(requestUtil::isAnonymousEndpoint)
                .permitAll();
        // Public routes are OK to access
        urlRegistry.requestMatchers(requestUtil::isAnonymousRoute).permitAll();
        urlRegistry.requestMatchers(getDefaultHttpSecurityPermitMatcher())
                .permitAll();

        // all other requests require authentication
        urlRegistry.anyRequest().authenticated();

        // Enable view access control
        viewAccessChecker.enable();
    }

    /**
     * Matcher for framework internal requests.
     *
     * @return default {@link HttpSecurity} bypass matcher
     */
    public static RequestMatcher getDefaultHttpSecurityPermitMatcher() {
        Stream<String> flowProvided = Stream
                .of(HandlerHelper.getPublicResourcesRequiringSecurityContext());
        Stream<String> other = Stream.of("/vaadinServlet/**");

        return new OrRequestMatcher(Stream.concat(flowProvided, other)
                .map(AntPathRequestMatcher::new).collect(Collectors.toList()));
    }

    /**
     * Matcher for Vaadin static (public) resources.
     *
     * @return default {@link WebSecurity} ignore matcher
     */
    public static RequestMatcher getDefaultWebSecurityIgnoreMatcher() {
        return new OrRequestMatcher(Stream
                .of(HandlerHelper.getPublicResources())
                .map(AntPathRequestMatcher::new).collect(Collectors.toList()));
    }

    /**
     * Sets up login for the application using form login with the given path
     * for the login view.
     * <p>
     * This is used when your application uses a Fusion based login view
     * available at the given path.
     * 
     * @param http
     *            the http security from {@link #configure(HttpSecurity)}
     * @param fusionLoginViewPath
     *            the path to the login view
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http, String fusionLoginViewPath)
            throws Exception {
        setLoginView(http, fusionLoginViewPath, "/");
    }

    /**
     * Sets up login for the application using form login with the given path
     * for the login view.
     * <p>
     * This is used when your application uses a Fusion based login view
     * available at the given path.
     * 
     * @param http
     *            the http security from {@link #configure(HttpSecurity)}
     * @param fusionLoginViewPath
     *            the path to the login view
     * @param logoutUrl
     *            the URL to redirect the user to after logging out
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http, String fusionLoginViewPath,
            String logoutUrl) throws Exception {
        FormLoginConfigurer<HttpSecurity> formLogin = http.formLogin();
        formLogin.loginPage(fusionLoginViewPath).permitAll();
        formLogin.successHandler(
                new VaadinSavedRequestAwareAuthenticationSuccessHandler());
        http.logout().logoutSuccessUrl(logoutUrl);
        viewAccessChecker.setLoginView(fusionLoginViewPath);
    }

    /**
     * Sets up login for the application using the given Flow login view.
     * 
     * @param http
     *            the http security from {@link #configure(HttpSecurity)}
     * @param flowLoginView
     *            the login view to use
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http,
            Class<? extends Component> flowLoginView) throws Exception {
        setLoginView(http, flowLoginView, "/");
    }

    /**
     * Sets up login for the application using the given Flow login view.
     * 
     * @param http
     *            the http security from {@link #configure(HttpSecurity)}
     * @param flowLoginView
     *            the login view to use
     * @param logoutUrl
     *            the URL to redirect the user to after logging out
     * 
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http,
            Class<? extends Component> flowLoginView, String logoutUrl)
            throws Exception {
        Optional<Route> route = AnnotationReader.getAnnotationFor(flowLoginView,
                Route.class);

        if (!route.isPresent()) {
            throw new IllegalArgumentException(
                    "Unable find a @Route annotation on the login view "
                            + flowLoginView.getName());
        }

        String loginPath = RouteUtil.getRoutePath(flowLoginView, route.get());
        if (!loginPath.startsWith("/")) {
            loginPath = "/" + loginPath;
        }

        // Actually set it up
        FormLoginConfigurer<HttpSecurity> formLogin = http.formLogin();
        formLogin.loginPage(loginPath).permitAll();
        formLogin.successHandler(
                new VaadinSavedRequestAwareAuthenticationSuccessHandler());
        http.csrf().ignoringAntMatchers(loginPath);
        http.logout().logoutSuccessUrl(logoutUrl);
        viewAccessChecker.setLoginView(flowLoginView);
    }

}
