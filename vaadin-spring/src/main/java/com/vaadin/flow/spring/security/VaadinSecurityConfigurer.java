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

import jakarta.servlet.ServletContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
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
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.auth.NavigationAccessControl;

import static com.vaadin.flow.spring.security.VaadinWebSecurity.getDefaultHttpSecurityPermitMatcher;
import static com.vaadin.flow.spring.security.VaadinWebSecurity.getDefaultWebSecurityIgnoreMatcher;

/**
 * A {@link SecurityConfigurer} specifically designed for Vaadin applications.
 * <p>
 * Provides built-in customizers to configure the security settings for Flow and
 * Hilla by integrating with Spring Security and specialized methods to handle
 * view access control and default security workflows in Vaadin applications.
 * <p>
 * Usage example:
 *
 * <pre>
 * <code>
 * &#64;Configuration
 * public class MyWebSecurity {
 *
 *     &#64;Bean
 *     SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         return http.with(new VaadinSecurityConfigurer(), configurer -&gt; {
 *             configurer.loginView(MyLoginView.class);
 *         }).build();
 *     }
 * }
 * </code>
 * </pre>
 */
public final class VaadinSecurityConfigurer
        extends AbstractHttpConfigurer<VaadinSecurityConfigurer, HttpSecurity> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(VaadinSecurityConfigurer.class);

    private final List<LogoutHandler> logoutHandlers = new ArrayList<>();

    private Class<? extends Component> loginView;

    private String formLoginPage;

    private String oauth2LoginPage;

    private String logoutSuccessUrl;

    private String postLogoutRedirectUri;

    private Consumer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> anyRequestAuthorizeRule = AuthorizedUrl::authenticated;

    private boolean enableNavigationAccessControl = true;

    private boolean alreadyInitializedOnce = false;

    /**
     * Configures the login view for use in a Flow application.
     * <p>
     * This method ensures that the provided login view class is annotated with
     * {@code @Route}, retrieves the route path for the login view, and sets up
     * the necessary configurations for login and logout paths.
     *
     * @param loginView
     *            the component class to be used as the login view
     * @return the current configurer instance for method chaining
     * @throws IllegalArgumentException
     *             if the provided class is not annotated with {@code @Route}
     */
    public VaadinSecurityConfigurer loginView(
            Class<? extends Component> loginView) {
        return loginView(loginView, getDefaultLogoutSuccessUrl());
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
     *            the component class to be used as the login view
     * @param logoutSuccessUrl
     *            the URL to redirect to upon a successful logout
     * @return the current configurer instance for method chaining
     * @throws IllegalArgumentException
     *             if the provided class is not annotated with {@code @Route}
     */
    public VaadinSecurityConfigurer loginView(
            Class<? extends Component> loginView, String logoutSuccessUrl) {
        this.loginView = loginView;
        var loginViewPath = getLoginViewPath(loginView);
        this.formLoginPage = getRequestUtil().applyUrlMapping(loginViewPath);
        this.logoutSuccessUrl = logoutSuccessUrl;
        return this;
    }

    /**
     * Configures the login view for use in a Hilla application.
     *
     * @param loginView
     *            the path to the login view
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer loginView(String loginView) {
        return loginView(loginView, getDefaultLogoutSuccessUrl());
    }

    /**
     * Configures the login view for use in a Hilla application and the logout
     * success URL.
     *
     * @param loginView
     *            the path to the login view
     * @param logoutSuccessUrl
     *            the URL to redirect to upon a successful logout
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer loginView(String loginView,
            String logoutSuccessUrl) {
        this.formLoginPage = getRequestUtil().applyUrlMapping(loginView);
        this.logoutSuccessUrl = logoutSuccessUrl;
        return this;
    }

    /**
     * Configures the login page for OAuth2 authentication.
     *
     * @param oauth2LoginPage
     *            the login page for OAuth2 authentication
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer oauth2LoginPage(String oauth2LoginPage) {
        return oauth2LoginPage(oauth2LoginPage, "{baseUrl}");
    }

    /**
     * Configures the login page for OAuth2 authentication and the post-logout
     * redirect URI.
     *
     * @param oauth2LoginPage
     *            the login page for OAuth2 authentication
     * @param postLogoutRedirectUri
     *            the URI to redirect to after the user logs out
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer oauth2LoginPage(String oauth2LoginPage,
            String postLogoutRedirectUri) {
        this.oauth2LoginPage = oauth2LoginPage;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        return this;
    }

    /**
     * Configures the handler for a successful logout.
     * <p>
     * This overrides the default handler configured automatically with either
     * {@link #loginView(Class)} or {@link #oauth2LoginPage(String)} (and their
     * overloads).
     *
     * @param logoutSuccessHandler
     *            the logout success handler
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer logoutSuccessHandler(
            LogoutSuccessHandler logoutSuccessHandler) {
        setSharedObject(LogoutSuccessHandler.class, logoutSuccessHandler);
        return this;
    }

    /**
     * Adds a {@link LogoutHandler} to the list of logout handlers.
     *
     * @param logoutHandler
     *            the logout handler to be added
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer addLogoutHandler(
            LogoutHandler logoutHandler) {
        logoutHandlers.add(logoutHandler);
        return this;
    }

    /**
     * Configures the access rule for any request not matching other configured
     * rules.
     * <p>
     * The default rule is to require authentication, which is the equivalent of
     * passing {@link AuthorizedUrl#authenticated()} to this method.
     *
     * @param anyRequestAuthorizeRule
     *            the access rule for any request not matching other rules
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer anyRequest(
            Consumer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> anyRequestAuthorizeRule) {
        this.anyRequestAuthorizeRule = anyRequestAuthorizeRule;
        return this;
    }

    /**
     * Enables or disables configuration of {@link NavigationAccessControl}.
     * <p>
     * {@link NavigationAccessControl} is enabled by default.
     *
     * @param enableNavigationAccessControl
     *            a boolean flag indicating whether
     *            {@link NavigationAccessControl} should be enabled or disabled
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer enableNavigationAccessControl(
            boolean enableNavigationAccessControl) {
        this.enableNavigationAccessControl = enableNavigationAccessControl;
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
    public RequestMatcher defaultPermitMatcher() {
        var urlMapping = getRequestUtil().getUrlMapping();
        var baseMatcher = RequestMatchers.anyOf(
                getRequestUtil()::isFrameworkInternalRequest,
                getRequestUtil()::isAnonymousRoute,
                getRequestUtil()::isCustomWebIcon,
                getDefaultWebSecurityIgnoreMatcher(urlMapping),
                getDefaultHttpSecurityPermitMatcher(urlMapping));
        if (EndpointRequestUtil.isHillaAvailable()) {
            return RequestMatchers.anyOf(baseMatcher,
                    getRequestUtil()::isAllowedHillaView,
                    getRequestUtil()::isAnonymousEndpoint);
        }
        return baseMatcher;
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
        http.csrf(this::customizeCsrf);
        http.logout(this::customizeLogout);
        http.requestCache(this::customizeRequestCache);
        http.exceptionHandling(this::customizeExceptionHandling);
        if (!alreadyInitializedOnce) {
            http.authorizeHttpRequests(this::customizeAuthorizeHttpRequests);
        }
        // The init method might be called multiple times if the configurer is
        // added during initialization of another configurer. This flag allows
        // tracking whether initialization has already happened once to avoid
        // redundant configuration (e.g., adding request matchers twice).
        alreadyInitializedOnce = true;
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
        // Configuring the authorized requests here allows other configurers to
        // customize the authorized requests during their own initialization.
        // Also, it ensures that the anyRequest authorize-rule is configured as
        // late as possible, since it must be the last authorize-rule to be set.
        http.authorizeHttpRequests(registry -> {
            if (anyRequestAuthorizeRule != null) {
                anyRequestAuthorizeRule.accept(registry.anyRequest());
            }
        });
    }

    private String getLoginViewPath(Class<? extends Component> loginView) {
        var route = AnnotationReader.getAnnotationFor(loginView, Route.class);
        if (route.isEmpty()) {
            throw new IllegalArgumentException("Unable find a @Route annotation"
                    + " on the login view " + loginView.getName());
        }
        if (getApplicationContext() instanceof WebApplicationContext wac) {
            var vaadinCtx = new VaadinServletContext(wac.getServletContext());
            var loginPath = RouteUtil.getRoutePath(vaadinCtx, loginView);
            if (!loginPath.startsWith("/")) {
                loginPath = "/" + loginPath;
            }
            return loginPath;
        }
        throw new IllegalStateException("VaadinWebSecurityConfigurer cannot be "
                + "used without WebApplicationContext.");
    }

    private String getServletContextPath() {
        return getSharedObjectOrBean(ServletContext.class).getContextPath();
    }

    private String getDefaultLogoutSuccessUrl() {
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
        handler.setDefaultTargetUrl(getRequestUtil().applyUrlMapping(""));
        getSharedObject(RequestCache.class).ifPresent(handler::setRequestCache);
        getBuilder().setSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class,
                handler);
        return handler;
    }

    private void customizeCsrf(CsrfConfigurer<HttpSecurity> configurer) {
        if (!alreadyInitializedOnce) {
            configurer.ignoringRequestMatchers(
                    getRequestUtil()::isFrameworkInternalRequest);
        }
        if (formLoginPage != null) {
            configurer.ignoringRequestMatchers(
                    new AntPathRequestMatcher(formLoginPage));
        }
    }

    private void customizeLogout(LogoutConfigurer<HttpSecurity> configurer) {
        configurer.invalidateHttpSession(true);
        getSharedObject(LogoutSuccessHandler.class).or(() -> {
            if (logoutSuccessUrl != null) {
                return createSimpleUrlLogoutSuccessHandler(logoutSuccessUrl);
            } else if (postLogoutRedirectUri != null) {
                return createOidcLogoutSuccessHandler(postLogoutRedirectUri);
            }
            return Optional.empty();
        }).ifPresent(configurer::logoutSuccessHandler);
        logoutHandlers.forEach(configurer::addLogoutHandler);
        logoutHandlers.clear();
        if (!alreadyInitializedOnce) {
            // Allows setting logout handlers on the AuthenticationContext at
            // the
            // right time, i.e., during the logout configuration lifecycle
            // phase.
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
    }

    Optional<LogoutSuccessHandler> createSimpleUrlLogoutSuccessHandler(
            String logoutSuccessUrl) {
        var handler = new VaadinSimpleUrlLogoutSuccessHandler();
        handler.setRedirectStrategy(new UidlRedirectStrategy());
        handler.setDefaultTargetUrl(logoutSuccessUrl);
        return Optional.of(handler);
    }

    Optional<LogoutSuccessHandler> createOidcLogoutSuccessHandler(
            String postLogoutRedirectUri) {
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
        // If there is an existing RequestCache shared object, use that instead
        // of creating a new one. Prevents the configurer from overriding any
        // custom RequestCache that might already been set.
        var requestCache = getSharedObject(RequestCache.class)
                .orElseGet(this::getVaadinDefaultRequestCache);
        configurer.requestCache(requestCache);
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
        var exceptionHandlers = new LinkedHashMap<Class<? extends AccessDeniedException>, AccessDeniedHandler>();
        exceptionHandlers.put(CsrfException.class, (req, res, exc) -> res
                .setStatus(HttpStatus.UNAUTHORIZED.value()));
        var requestHandlers = new LinkedHashMap<RequestMatcher, AccessDeniedHandler>();
        requestHandlers.put(getRequestUtil()::isEndpointRequest,
                new DelegatingAccessDeniedHandler(exceptionHandlers,
                        new AccessDeniedHandlerImpl()));
        return new RequestMatcherDelegatingAccessDeniedHandler(requestHandlers,
                new AccessDeniedHandlerImpl());
    }

    private void customizeAuthorizeHttpRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers(defaultPermitMatcher()).permitAll();
    }

    private ApplicationContext getApplicationContext() {
        return getBuilder().getSharedObject(ApplicationContext.class);
    }

    private <T> void setSharedObject(Class<T> type, T object) {
        getBuilder().setSharedObject(type, object);
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
