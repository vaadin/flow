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
package com.vaadin.flow.spring.security;

import static com.vaadin.flow.spring.security.VaadinWebSecurity.getDefaultHttpSecurityPermitMatcher;
import static com.vaadin.flow.spring.security.VaadinWebSecurity.getDefaultWebSecurityIgnoreMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.DelegatingAccessDeniedHandler;
import org.springframework.security.web.access.RequestMatcherDelegatingAccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.auth.NavigationAccessControl;

import jakarta.servlet.ServletContext;

/**
 * A {@link SecurityConfigurer} specifically designed for Vaadin applications.
 * <p>
 * Provides built-in customizers to configure the security settings for Flow and
 * Hilla by integrating with Spring Security and specialized methods to handle
 * view access control and default security workflows in Vaadin applications.
 * <p>
 * For example:
 *
 * <pre>
 * <code>
 * &#64;Configuration
 * public class MyWebSecurity {
 *
 *     &#64;Bean
 *     SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         return http.with(new VaadinWebSecurityConfigurer(), configurer -&gt; {
 *             configurer.loginView(MyLoginView.class);
 *         }).build();
 *     }
 * }
 * </code>
 * </pre>
 */
public final class VaadinWebSecurityConfigurer extends
        AbstractHttpConfigurer<VaadinWebSecurityConfigurer, HttpSecurity> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(VaadinWebSecurityConfigurer.class);

    private final List<LogoutHandler> logoutHandlers = new ArrayList<>();

    private Class<? extends Component> loginView;

    private String formLoginPage;

    private String oauth2LoginPage;

    private String logoutSuccessUrl;

    private String postLogoutRedirectUri;

    private String oidcBackChannelLogoutUri;

    private boolean enableNavigationAccessControl = true;

    private UnaryOperator<String> urlMapping = url -> getRequestUtil()
            .applyUrlMapping(url);

    private Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> anyRequestCustomizer = AuthorizeHttpRequestsConfigurer.AuthorizedUrl::authenticated;

    /**
     * Configures the login view for use in Flow applications.
     * <p>
     * This method ensures that the provided login view class is annotated with
     * {@code @Route}, retrieves the route path for the login view, and sets up
     * the necessary configurations for login and logout paths.
     *
     * @param loginView
     *            the {@code Class} instance of the component to be used as the
     *            login view
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     * @throws IllegalArgumentException
     *             if the provided login view class does not contain a
     *             {@code @Route} annotation
     */
    public VaadinWebSecurityConfigurer loginView(
            Class<? extends Component> loginView) {
        return loginView(loginView, getDefaultLogoutUrl());
    }

    /**
     * Configures the login view for use in a Flow application and the logout
     * success URL.
     * <p>
     * This method ensures that the provided login view class is annotated with
     * {@code @Route}, retrieves the route path for the login view, and sets up
     * the necessary configurations for login and logout paths.
     *
     * @param loginView
     *            the class of the component to be used as the login view, which
     *            must be annotated with {@code @Route}
     * @param logoutSuccessUrl
     *            the URL to redirect to upon a successful logout
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     * @throws IllegalArgumentException
     *             if the provided login view class does not contain a
     *             {@code @Route} annotation
     */
    public VaadinWebSecurityConfigurer loginView(
            Class<? extends Component> loginView, String logoutSuccessUrl) {
        this.loginView = loginView;
        this.formLoginPage = urlMapping.apply(getLoginViewPath(loginView));
        this.logoutSuccessUrl = logoutSuccessUrl;
        return this;
    }

    /**
     * Configures the login view for use in a Hilla application.
     * <p>
     * This method sets the specified login view path and default logout URL,
     * returning the current configuration for method chaining.
     *
     * @param loginView
     *            the path to be used as the Hilla login view
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer loginView(String loginView) {
        return loginView(loginView, getDefaultLogoutUrl());
    }

    /**
     * Configures the login view for use in a Hilla application and sets the
     * logout success URL.
     * <p>
     * This method sets the specified login view path and the logout success
     * URL, returning the current configuration for method chaining.
     *
     * @param loginView
     *            the path to be used as the Hilla login view
     * @param logoutSuccessUrl
     *            the URL to redirect to upon a successful logout
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer loginView(String loginView,
            String logoutSuccessUrl) {
        this.formLoginPage = urlMapping.apply(loginView);
        this.logoutSuccessUrl = logoutSuccessUrl;
        return this;
    }

    /**
     * Configures the OAuth2 login page.
     *
     * @param oauth2LoginPage
     *            the URL to be used as the OAuth2 login page
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer oauth2LoginPage(String oauth2LoginPage) {
        return oauth2LoginPage(oauth2LoginPage, "{baseUrl}");
    }

    /**
     * Configures the OAuth2 login page and post-logout redirection URI.
     *
     * @param oauth2LoginPage
     *            the URL of the OAuth2 login page
     * @param postLogoutRedirectUri
     *            the URI to redirect to after the user logs out
     * @return the updated instance of {@code VaadinWebSecurityConfigurer} for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer oauth2LoginPage(String oauth2LoginPage,
            String postLogoutRedirectUri) {
        this.oauth2LoginPage = oauth2LoginPage;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        return this;
    }

    /**
     * Configures the OpenID Connect Back-Channel Logout URI.
     * <p>
     * This URI is used to handle Back-Channel Logout requests in an OpenID
     * Connect compliant setup.
     *
     * @param oidcBackChannelLogoutUri
     *            the URI to be used for OIDC back-channel logout.
     * @return the current instance of {@code VaadinWebSecurityConfigurer} for
     *         method chaining.
     */
    public VaadinWebSecurityConfigurer oidcBackChannelLogoutUri(
            String oidcBackChannelLogoutUri) {
        this.oidcBackChannelLogoutUri = oidcBackChannelLogoutUri;
        return this;
    }

    /**
     * Configures the {@link RequestUtil} instance for this
     * {@code VaadinWebSecurityConfigurer}.
     * <p>
     * This method allows setting a custom {@code RequestUtil} to be shared
     * within the security configuration process.
     *
     * @param requestUtil
     *            the {@code RequestUtil} instance to be configured
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer requestUtil(RequestUtil requestUtil) {
        getBuilder().setSharedObject(RequestUtil.class, requestUtil);
        return this;
    }

    /**
     * Configures a customizer for handling any request authorization.
     *
     * @param anyRequestCustomizer
     *            a customizer for configuring the handling of any requests
     * @return the current {@link VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer anyRequest(
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> anyRequestCustomizer) {
        this.anyRequestCustomizer = anyRequestCustomizer;
        return this;
    }

    /**
     * Configures the {@code AuthenticationContext} instance for this
     * {@code VaadinWebSecurityConfigurer}.
     * <p>
     * This method allows setting a custom {@code AuthenticationContext} to be
     * shared within the security configuration process.
     *
     * @param authenticationContext
     *            the {@code AuthenticationContext} instance to be configured
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer authenticationContext(
            AuthenticationContext authenticationContext) {
        getBuilder().setSharedObject(AuthenticationContext.class,
                authenticationContext);
        return this;
    }

    /**
     * Configures the {@code NavigationAccessControl} instance for this
     * {@code VaadinWebSecurityConfigurer}.
     * <p>
     * This method allows setting a custom {@code NavigationAccessControl} to be
     * shared within the security configuration process.
     *
     * @param navigationAccessControl
     *            the {@code NavigationAccessControl} instance to be configured
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer navigationAccessControl(
            NavigationAccessControl navigationAccessControl) {
        getBuilder().setSharedObject(NavigationAccessControl.class,
                navigationAccessControl);
        return this;
    }

    /**
     * Configures the {@code VaadinRolePrefixHolder} instance for this
     * {@code VaadinWebSecurityConfigurer}.
     * <p>
     * This method allows setting a custom {@code VaadinRolePrefixHolder} to be
     * shared within the security configuration process, enabling consistent
     * role prefix management across the application.
     *
     * @param vaadinRolePrefixHolder
     *            the {@code VaadinRolePrefixHolder} instance to be configured
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer vaadinRolePrefixHolder(
            VaadinRolePrefixHolder vaadinRolePrefixHolder) {
        getBuilder().setSharedObject(VaadinRolePrefixHolder.class,
                vaadinRolePrefixHolder);
        return this;
    }

    /**
     * Configures the {@code VaadinDefaultRequestCache} instance for this
     * {@code VaadinWebSecurityConfigurer}.
     * <p>
     * This method allows setting a custom {@code VaadinDefaultRequestCache} to
     * be shared within the security configuration process, enabling custom
     * request caching behavior tailored to Vaadin applications.
     *
     * @param vaadinDefaultRequestCache
     *            the {@code VaadinDefaultRequestCache} instance to be
     *            configured
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer vaadinDefaultRequestCache(
            VaadinDefaultRequestCache vaadinDefaultRequestCache) {
        getBuilder().setSharedObject(VaadinDefaultRequestCache.class,
                vaadinDefaultRequestCache);
        return this;
    }

    /**
     * Enables or disables navigation access control for the configuration.
     * <p>
     * This can be used to specify whether navigation access control should be
     * applied during the security configuration process.
     *
     * @param enableNavigationAccessControl
     *            a boolean flag indicating whether navigation access control
     *            should be enabled or disabled
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer enableNavigationAccessControl(
            boolean enableNavigationAccessControl) {
        this.enableNavigationAccessControl = enableNavigationAccessControl;
        return this;
    }

    /**
     * Configures a custom URL mapping function for this
     * {@code VaadinWebSecurityConfigurer}.
     * <p>
     * This method allows setting a {@code UnaryOperator<String>} that defines
     * URL mappings used during the security configuration process.
     *
     * @param urlMapping
     *            the {@code UnaryOperator<String>} to be configured,
     *            representing a mapping function for URLs
     * @return the current {@code VaadinWebSecurityConfigurer} instance for
     *         method chaining
     */
    public VaadinWebSecurityConfigurer urlMapping(
            UnaryOperator<String> urlMapping) {
        this.urlMapping = urlMapping;
        return this;
    }

    public VaadinWebSecurityConfigurer addToLogoutHandlers(
            LogoutHandler logoutHandler) {
        logoutHandlers.add(logoutHandler);
        return this;
    }

    /**
     * Creates and returns a composite {@link RequestMatcher} for identifying
     * requests that should be permitted without authentication within a Vaadin
     * application.
     * <p>
     * This matcher combines multiple specific matchers, including those for
     * framework internal requests, anonymous endpoints, allowed Hilla views,
     * anonymous routes, custom web icons, and default security configurations.
     *
     * @return a {@link RequestMatcher} that matches requests to be allowed
     *         without authentication
     */
    public RequestMatcher vaadinPermitAllMatcher() {
        return RequestMatchers.anyOf(
                getRequestUtil()::isFrameworkInternalRequest,
                getRequestUtil()::isAnonymousEndpoint,
                getRequestUtil()::isAllowedHillaView,
                getRequestUtil()::isAnonymousRoute,
                getRequestUtil()::isCustomWebIcon,
                getDefaultHttpSecurityPermitMatcher(
                        getRequestUtil().getUrlMapping()),
                getDefaultWebSecurityIgnoreMatcher(
                        getRequestUtil().getUrlMapping()));
    }

    @Override
    public void init(HttpSecurity http) throws Exception {
        if (formLoginPage != null) {
            http.formLogin(configurer -> {
                configurer.loginPage(formLoginPage).permitAll();
                configurer.successHandler(getAuthenticationSuccessHandler());
            });
        } else if (oauth2LoginPage != null) {
            http.oauth2Login(configurer -> {
                configurer.loginPage(oauth2LoginPage).permitAll();
                configurer.successHandler(getAuthenticationSuccessHandler());
            });
        }
        if (oidcBackChannelLogoutUri != null) {
            http.oidcLogout(configurer -> configurer
                    .backChannel(bc -> bc.logoutUri(oidcBackChannelLogoutUri)));
        }
        http.csrf(this::customizeCsrf);
        http.logout(this::customizeLogout);
        http.requestCache(this::customizeRequestCache);
        http.exceptionHandling(this::customizeExceptionHandling);
        http.authorizeHttpRequests(registry -> {
            registry.requestMatchers(vaadinPermitAllMatcher()).permitAll();
            if (anyRequestCustomizer != null) {
                anyRequestCustomizer.customize(registry.anyRequest());
            }
        });
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        var rolePrefixHolder = getVaadinRolePrefixHolder();
        if (rolePrefixHolder != null) {
            getAuthenticationContext().setRolePrefixHolder(rolePrefixHolder);
            getSharedObject(SecurityContextHolderAwareRequestFilter.class)
                    .ifPresent(rolePrefixHolder::resetRolePrefix);
        }
        getNavigationAccessControl().setEnabled(enableNavigationAccessControl);
        if (enableNavigationAccessControl) {
            if (loginView != null) {
                getNavigationAccessControl().setLoginView(loginView);
            } else if (formLoginPage != null) {
                getNavigationAccessControl().setLoginView(formLoginPage);
            } else if (oauth2LoginPage != null) {
                getNavigationAccessControl().setLoginView(oauth2LoginPage);
            }
        }
    }

    private String getLoginViewPath(Class<? extends Component> loginView) {
        if (AnnotationReader.getAnnotationFor(loginView, Route.class)
                .isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable find a @Route annotation on the login view "
                            + loginView.getName());
        }
        var applicationContext = getApplicationContext();
        if (!(applicationContext instanceof WebApplicationContext)) {
            throw new RuntimeException(
                    "VaadinWebSecurity cannot be used without WebApplicationContext.");
        }
        var vaadinContext = new VaadinServletContext(
                ((WebApplicationContext) applicationContext)
                        .getServletContext());
        var loginPath = RouteUtil.getRoutePath(vaadinContext, loginView);
        if (!loginPath.startsWith("/")) {
            loginPath = "/" + loginPath;
        }
        return loginPath;
    }

    private String getServletContextPath() {
        return getSharedObjectOrBean(ServletContext.class).getContextPath();
    }

    private String getDefaultLogoutUrl() {
        var servletContextPath = getServletContextPath();
        if (!servletContextPath.startsWith("/")) {
            servletContextPath = "/" + servletContextPath;
        }
        return servletContextPath;
    }

    private RequestUtil getRequestUtil() {
        return getSharedObjectOrBean(RequestUtil.class);
    }

    private AuthenticationContext getAuthenticationContext() {
        return getSharedObjectOrBean(AuthenticationContext.class);
    }

    private NavigationAccessControl getNavigationAccessControl() {
        return getSharedObjectOrBean(NavigationAccessControl.class);
    }

    private VaadinRolePrefixHolder getVaadinRolePrefixHolder() {
        return getSharedObjectOrBean(VaadinRolePrefixHolder.class);
    }

    private VaadinDefaultRequestCache getVaadinDefaultRequestCache() {
        return getSharedObjectOrBean(VaadinDefaultRequestCache.class);
    }

    private VaadinSavedRequestAwareAuthenticationSuccessHandler getAuthenticationSuccessHandler() {
        return getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class)
                .orElseGet(this::createAuthenticationSuccessHandler);
    }

    private VaadinSavedRequestAwareAuthenticationSuccessHandler createAuthenticationSuccessHandler() {
        var handler = new VaadinSavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl(urlMapping.apply(""));
        getSharedObject(RequestCache.class).ifPresent(handler::setRequestCache);
        getBuilder().setSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class,
                handler);
        return handler;
    }

    private void customizeCsrf(CsrfConfigurer<HttpSecurity> configurer) {
        configurer.ignoringRequestMatchers(
                getRequestUtil()::isFrameworkInternalRequest);
        if (formLoginPage != null) {
            configurer.ignoringRequestMatchers(
                    new AntPathRequestMatcher(formLoginPage));
        }
    }

    private void customizeLogout(LogoutConfigurer<HttpSecurity> configurer) {
        if (logoutSuccessUrl != null) {
            getSharedObject(LogoutSuccessHandler.class)
                    .or(this::createSimpleUrlLogoutSuccessHandler)
                    .ifPresent(configurer::logoutSuccessHandler);
        } else if (postLogoutRedirectUri != null) {
            getSharedObject(LogoutSuccessHandler.class)
                    .or(this::createOidcClientInitiatedLogoutSuccessHandler)
                    .ifPresent(configurer::logoutSuccessHandler);
        }
        logoutHandlers.forEach(configurer::addLogoutHandler);
        configurer.invalidateHttpSession(true);
        // Allows setting logout handlers on the AuthenticationContext at the
        // right time, i.e., during the logout configuration lifecycle phase.
        var postProcessor = new ObjectPostProcessor<LogoutFilter>() {
            @Override
            public <O extends LogoutFilter> O postProcess(O filter) {
                getAuthenticationContext().setLogoutHandlers(
                        configurer.getLogoutSuccessHandler(),
                        configurer.getLogoutHandlers());
                return filter;
            }
        };
        configurer.withObjectPostProcessor(postProcessor);
    }

    private Optional<LogoutSuccessHandler> createSimpleUrlLogoutSuccessHandler() {
        var handler = new VaadinSimpleUrlLogoutSuccessHandler();
        handler.setRedirectStrategy(new UidlRedirectStrategy());
        handler.setDefaultTargetUrl(logoutSuccessUrl);
        return Optional.of(handler);
    }

    private Optional<LogoutSuccessHandler> createOidcClientInitiatedLogoutSuccessHandler() {
        var crr = getSharedObjectOrBean(ClientRegistrationRepository.class);
        if (crr != null) {
            var handler = new OidcClientInitiatedLogoutSuccessHandler(crr);
            handler.setRedirectStrategy(new UidlRedirectStrategy());
            handler.setPostLogoutRedirectUri(postLogoutRedirectUri);
            return Optional.of(handler);
        }
        LOGGER.warn("Cannot create OidcClientInitiatedLogoutSuccessHandler "
                + "because ClientRegistrationRepository bean is not available.");
        return Optional.empty();
    }

    private void customizeRequestCache(
            RequestCacheConfigurer<HttpSecurity> configurer) {
        configurer.requestCache(getVaadinDefaultRequestCache());
    }

    private void customizeExceptionHandling(
            ExceptionHandlingConfigurer<HttpSecurity> configurer) {
        // Respond with 401 Unauthorized HTTP status code for unauthorized
        // requests for protected Hilla endpoints, so that the response could
        // be handled on the client side using e.g. `InvalidSessionMiddleware`.
        configurer.accessDeniedHandler(createAccessDeniedHandler())
                .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        getRequestUtil()::isEndpointRequest);
        if (formLoginPage != null) {
            configurer.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint(formLoginPage),
                    AnyRequestMatcher.INSTANCE);
        }
    }

    private AccessDeniedHandler createAccessDeniedHandler() {
        LinkedHashMap<Class<? extends AccessDeniedException>, AccessDeniedHandler> exceptionHandlers = new LinkedHashMap<>();
        exceptionHandlers.put(CsrfException.class, (req, res, exc) -> res
                .setStatus(HttpStatus.UNAUTHORIZED.value()));
        LinkedHashMap<RequestMatcher, AccessDeniedHandler> matcherHandlers = new LinkedHashMap<>();
        matcherHandlers.put(getRequestUtil()::isEndpointRequest,
                new DelegatingAccessDeniedHandler(exceptionHandlers,
                        new AccessDeniedHandlerImpl()));
        return new RequestMatcherDelegatingAccessDeniedHandler(matcherHandlers,
                new AccessDeniedHandlerImpl());
    }

    private ApplicationContext getApplicationContext() {
        return getBuilder().getSharedObject(ApplicationContext.class);
    }

    private <T> Optional<T> getSharedObject(Class<T> type) {
        return Optional.ofNullable(getBuilder().getSharedObject(type));
    }

    private <T> T getSharedObjectOrBean(Class<T> type) {
        return getSharedObject(type).orElseGet(() -> {
            var provider = getApplicationContext().getBeanProvider(type);
            T bean = provider.getIfAvailable();
            if (bean != null) {
                getBuilder().setSharedObject(type, bean);
            }
            return bean;
        });
    }
}
