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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
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
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.RoutePathAccessChecker;

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
 * &#64;EnableWebSecurity
 * public class MyWebSecurity {
 *
 *     &#64;Bean
 *     SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         return http.with(VaadinSecurityConfigurer.vaadin(), configurer -&gt; {
 *             configurer.loginView(MyLoginView.class);
 *         }).build();
 *     }
 * }
 * </code>
 * </pre>
 *
 * <h2>Configurers</h2>
 * <p>
 * This configurer applies other configurers to set up the security filter chain
 * properly for Vaadin applications. The following configurers may be applied:
 * <ul>
 * <li>{@link FormLoginConfigurer} if a login view is set with
 * {@link #loginView(Class)} (or overloads)</li>
 * <li>{@link OAuth2LoginConfigurer} if a login page for OAuth2 authentication
 * is set with {@link #oauth2LoginPage(String)} (or overloads)</li>
 * <li>{@link CsrfConfigurer} to allow internal framework requests (can be
 * disabled with {@link #enableCsrfConfiguration(boolean)})</li>
 * <li>{@link LogoutConfigurer} to configure logout handlers for Vaadin
 * applications (can be disabled with
 * {@link #enableLogoutConfiguration(boolean)})</li>
 * <li>{@link RequestCacheConfigurer} to set a request cache designed for Vaadin
 * applications (can be disabled with
 * {@link #enableRequestCacheConfiguration(boolean)})</li>
 * <li>{@link ExceptionHandlingConfigurer} to configure proper exception
 * handling for Vaadin applications (can be disabled with
 * {@link #enableExceptionHandlingConfiguration(boolean)})</li>
 * <li>{@link AuthorizeHttpRequestsConfigurer} to permit internal framework
 * requests and other public endpoints (can be disabled with
 * {@link #enableAuthorizedRequestsConfiguration(boolean)})</li>
 * </ul>
 *
 * <h2>Shared Objects</h2>
 * <p>
 * The following beans are shared by this configurer (if not already shared):
 * <ul>
 * <li>{@link RequestUtil}</li>
 * <li>{@link AuthenticationContext}</li>
 * <li>{@link NavigationAccessControl}</li>
 * <li>{@link VaadinRolePrefixHolder}</li>
 * <li>{@link VaadinDefaultRequestCache}</li>
 * <li>{@link VaadinSavedRequestAwareAuthenticationSuccessHandler}</li>
 * <li>{@link ClientRegistrationRepository}</li>
 * </ul>
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

    private boolean enableCsrfConfiguration = true;

    private boolean enableLogoutConfiguration = true;

    private boolean enableRequestCacheConfiguration = true;

    private boolean enableExceptionHandlingConfiguration = true;

    private boolean enableAuthorizedRequestsConfiguration = true;

    private Consumer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl> anyRequestAuthorizeRule = AuthorizedUrl::denyAll;

    private boolean enableNavigationAccessControl = true;

    private boolean alreadyInitializedOnce = false;

    /**
     * Creates a new instance the {@code VaadinSecurityConfigurer} that can be
     * used to configure security settings for Vaadin applications.
     *
     * @return a new instance of {@code VaadinSecurityConfigurer}
     */
    public static VaadinSecurityConfigurer vaadin() {
        return new VaadinSecurityConfigurer();
    }

    private VaadinSecurityConfigurer() {
        // Instance creation is handled by the static factory method.
    }

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
     * <p>
     * This is used when your application uses a Hilla-based login view that is
     * available at the given path.
     * <p>
     * If the path points to a Flow view, the corresponding Java class must be
     * annotated with {@link com.vaadin.flow.server.auth.AnonymousAllowed} to
     * ensure that the view is always accessible.
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
     * <p>
     * This is used when your application uses a Hilla-based login view that is
     * available at the given path.
     * <p>
     * If the path points to a Flow view, the corresponding Java class must be
     * annotated with {@link com.vaadin.flow.server.auth.AnonymousAllowed} to
     * ensure that the view is always accessible.
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
     * <p>
     * If using Spring's OAuth2 client, this should be set to Spring's internal
     * redirect endpoint {@code /oauth2/authorization/{registrationId}} where
     * {@code registrationId} is the ID of the OAuth2 client registration.
     * <p>
     * This method also configures a logout success handler that redirects to
     * the application base URL after logout.
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
     * <p>
     * If using Spring's OAuth2 client, this should be set to Spring's internal
     * redirect endpoint {@code /oauth2/authorization/{registrationId}} where
     * {@code registrationId} is the ID of the OAuth2 client registration.
     * <p>
     * The {@code {baseUrl}} placeholder is also supported, which is the same as
     * {@code {baseScheme}://{baseHost}{basePort}{basePath}}.
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
     * Enables or disables automatic CSRF configuration (enabled by default).
     * <p>
     * This configurer will automatically configure Spring's CSRF filter to
     * allow Vaadin internal framework requests to be properly processed.
     *
     * @param enableCsrfConfiguration
     *            whether CSRF configuration should be enabled
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer enableCsrfConfiguration(
            boolean enableCsrfConfiguration) {
        this.enableCsrfConfiguration = enableCsrfConfiguration;
        return this;
    }

    /**
     * Enables or disables automatic logout configuration (enabled by default).
     * <p>
     * This configurer will automatically configure logout behavior to work
     * properly with Flow and Hilla.
     *
     * @param enableLogoutConfiguration
     *            whether logout configuration should be enabled
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer enableLogoutConfiguration(
            boolean enableLogoutConfiguration) {
        this.enableLogoutConfiguration = enableLogoutConfiguration;
        return this;
    }

    /**
     * Enables or disables automatic configuration of the request cache (enabled
     * by default).
     * <p>
     * This configurer will automatically configure the request cache to work
     * properly with Vaadin's internal framework requests.
     *
     * @param enableRequestCacheConfiguration
     *            whether configuration of the request cache should be enabled
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer enableRequestCacheConfiguration(
            boolean enableRequestCacheConfiguration) {
        this.enableRequestCacheConfiguration = enableRequestCacheConfiguration;
        return this;
    }

    /**
     * Enables or disables automatic configuration of exception handling
     * (enabled by default).
     * <p>
     * This configurer will automatically configure exception handling to work
     * properly with Flow and Hilla.
     *
     * @param enableExceptionHandlingConfiguration
     *            whether configuration of exception handling should be enabled
     * @return the current configurer instance for method chaining
     */
    public VaadinSecurityConfigurer enableExceptionHandlingConfiguration(
            boolean enableExceptionHandlingConfiguration) {
        this.enableExceptionHandlingConfiguration = enableExceptionHandlingConfiguration;
        return this;
    }

    /**
     * Enables or disables automatic configuration of authorized requests
     * (enabled by default).
     * <p>
     * This configurer will automatically configure authorized requests to
     * permit requests to anonymous Flow and Hilla views, and static assets.
     *
     * @param enableAuthorizedRequestsConfiguration
     *            whether configuration of authorized requests should be enabled
     * @return the current configurer instance for method chaining
     * @see #defaultPermitMatcher()
     */
    public VaadinSecurityConfigurer enableAuthorizedRequestsConfiguration(
            boolean enableAuthorizedRequestsConfiguration) {
        this.enableAuthorizedRequestsConfiguration = enableAuthorizedRequestsConfiguration;
        return this;
    }

    /**
     * Configures the access rule for any request not matching other configured
     * rules.
     * <p>
     * The default rule is to restrict access, which is the equivalent of
     * passing {@link AuthorizedUrl#denyAll()} to this method.
     *
     * @param anyRequestAuthorizeRule
     *            the access rule for any request not matching other rules, or
     *            {@code null} to disable automatic configuration
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
                // Vaadin internal requests must always be permitted to allow
                // public Flow assets and/or the Flow login view
                getRequestUtil()::isFrameworkInternalRequest,
                // Public routes are permitted
                getRequestUtil()::isAnonymousRoute,
                // Custom web icons (and favicons) are permitted
                getRequestUtil()::isCustomWebIcon,
                // Matchers for Vaadin static resources
                getDefaultHttpSecurityPermitMatcher(urlMapping),
                getDefaultWebSecurityIgnoreMatcher(urlMapping));
        if (EndpointRequestUtil.isHillaAvailable()) {
            return toRequestPrincipalAwareMatcher(
                    RequestMatchers.anyOf(baseMatcher,
                            // Matchers for known Hilla views
                            getRequestUtil()::isAllowedHillaView,
                            // Matcher for public Hilla endpoints
                            getRequestUtil()::isAnonymousEndpoint));
        }
        return toRequestPrincipalAwareMatcher(baseMatcher);
    }

    private RequestMatcher toRequestPrincipalAwareMatcher(
            RequestMatcher matcher) {
        if (enableNavigationAccessControl && getNavigationAccessControl()
                .hasAccessChecker(RoutePathAccessChecker.class)) {
            return RequestUtil.principalAwareRequestMatcher(matcher);
        }
        return matcher;
    }

    @Override
    public void init(HttpSecurity http) {
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
        if (enableCsrfConfiguration) {
            http.csrf(this::customizeCsrf);
        }
        if (enableLogoutConfiguration) {
            http.logout(this::customizeLogout);
        }
        if (enableRequestCacheConfiguration) {
            http.requestCache(this::customizeRequestCache);
        }
        if (enableExceptionHandlingConfiguration) {
            http.exceptionHandling(this::customizeExceptionHandling);
        }
        if (enableAuthorizedRequestsConfiguration && !alreadyInitializedOnce) {
            http.authorizeHttpRequests(this::customizeAuthorizeHttpRequests);
        }

        // The init method might be called multiple times if the configurer is
        // added during initialization of another configurer. This flag allows
        // tracking whether initialization has already happened once to avoid
        // redundant configuration (e.g., adding request matchers twice).
        alreadyInitializedOnce = true;

    }

    @Override
    public void configure(HttpSecurity http) {
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
        if (enableAuthorizedRequestsConfiguration
                && anyRequestAuthorizeRule != null) {
            http.authorizeHttpRequests(registry -> {
                anyRequestAuthorizeRule.accept(registry.anyRequest());
            });
        }
    }

    /**
     * Matcher for framework internal requests.
     *
     * Assumes Vaadin servlet to be mapped on root path ({@literal /*}).
     *
     * @return default {@link HttpSecurity} bypass matcher
     */
    public static RequestMatcher getDefaultHttpSecurityPermitMatcher() {
        return getDefaultHttpSecurityPermitMatcher("/*");
    }

    /**
     * Matcher for framework internal requests, with Vaadin servlet mapped on
     * the given path.
     *
     * @param urlMapping
     *            url mapping for the Vaadin servlet.
     * @return default {@link HttpSecurity} bypass matcher
     */
    public static RequestMatcher getDefaultHttpSecurityPermitMatcher(
            String urlMapping) {
        Objects.requireNonNull(urlMapping,
                "Vaadin servlet url mapping is required");
        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher
                .withDefaults();
        String[] paths = HandlerHelper
                .getPublicResourcesRequiringSecurityContext();
        assert paths.length > 0;
        return new OrRequestMatcher(Stream.of(paths)
                .map(path -> RequestUtil.applyUrlMapping(urlMapping, path))
                .map(builder::matcher).toArray(RequestMatcher[]::new));
    }

    /**
     * Matcher for Vaadin static (public) resources.
     *
     * Assumes Vaadin servlet to be mapped on root path ({@literal /*}).
     *
     * @return default matcher for Vaadin static (public) resources.
     */
    public static RequestMatcher getDefaultWebSecurityIgnoreMatcher() {
        return getDefaultWebSecurityIgnoreMatcher("/*");
    }

    /**
     * Matcher for Vaadin static (public) resources, with Vaadin servlet mapped
     * on the given path.
     *
     * Assumes Vaadin servlet to be mapped on root path ({@literal /*}).
     *
     * @param urlMapping
     *            the url mapping for the Vaadin servlet
     * @return default matcher for Vaadin static (public) resources.
     */
    public static RequestMatcher getDefaultWebSecurityIgnoreMatcher(
            String urlMapping) {
        Objects.requireNonNull(urlMapping,
                "Vaadin servlet url mapping is required");

        List<RequestMatcher> matchers = new ArrayList<>();
        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher
                .withDefaults();

        String[] publicResources = HandlerHelper.getPublicResources();
        assert publicResources.length > 0;

        Stream.of(publicResources)
                .map(path -> RequestUtil.applyUrlMapping(urlMapping, path))
                .map(builder::matcher).forEach(matchers::add);

        String[] publicResourcesRoot = HandlerHelper.getPublicResourcesRoot();
        assert publicResourcesRoot.length > 0;

        Stream.of(publicResourcesRoot).map(builder::matcher)
                .forEach(matchers::add);

        return new OrRequestMatcher(matchers);
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
        throw new IllegalStateException("VaadinSecurityConfigurer cannot be "
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
            configurer.ignoringRequestMatchers(PathPatternRequestMatcher
                    .withDefaults().matcher(formLoginPage));
        }
    }

    private void customizeLogout(LogoutConfigurer<HttpSecurity> configurer) {
        getSharedObject(LogoutSuccessHandler.class).or(() -> {
            if (logoutSuccessUrl != null) {
                return createSimpleUrlLogoutSuccessHandler(logoutSuccessUrl);
            } else if (postLogoutRedirectUri != null) {
                return createOidcLogoutSuccessHandler(postLogoutRedirectUri);
            }
            return Optional.empty();
        }).ifPresent(configurer::logoutSuccessHandler);
        var existingHandlers = configurer.getLogoutHandlers();
        logoutHandlers.stream()
                .filter(handler -> !existingHandlers.contains(handler))
                .forEach(configurer::addLogoutHandler);
        if (!alreadyInitializedOnce) {
            // Allows setting logout handlers on the AuthenticationContext at
            // the right time, i.e., during the logout configuration phase.
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

    private Optional<LogoutSuccessHandler> createSimpleUrlLogoutSuccessHandler(
            String logoutSuccessUrl) {
        var handler = new VaadinSimpleUrlLogoutSuccessHandler();
        handler.setRedirectStrategy(new UidlRedirectStrategy());
        handler.setDefaultTargetUrl(logoutSuccessUrl);
        return Optional.of(handler);
    }

    private Optional<LogoutSuccessHandler> createOidcLogoutSuccessHandler(
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
        var vaadinDefaultRequestCache = getVaadinDefaultRequestCache();
        if (vaadinDefaultRequestCache == null) {
            throw new IllegalStateException("No VaadinDefaultRequestCache bean "
                    + "or shared object found. Please make sure that either a "
                    + "bean or shared object of type VaadinDefaultRequestCache "
                    + "is available.");
        }
        // If there is an existing RequestCache shared object, use that as the
        // delegate cache for requests not saved by VaadinDefaultRequestCache.
        getSharedObject(RequestCache.class)
                .filter(cache -> !(cache instanceof VaadinDefaultRequestCache))
                .ifPresent(vaadinDefaultRequestCache::setDelegateRequestCache);
        // VaadinSavedRequestAwareAuthenticationSuccessHandler
        // uses RequestCache for client-side redirects after TypeScript login
        getSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class)
                .ifPresent(
                        vaadinSavedRequestAwareAuthenticationSuccessHandler -> {
                            vaadinSavedRequestAwareAuthenticationSuccessHandler
                                    .setRequestCache(vaadinDefaultRequestCache);
                        });
        configurer.requestCache(vaadinDefaultRequestCache);
    }

    private void customizeExceptionHandling(
            ExceptionHandlingConfigurer<HttpSecurity> configurer) {
        if (EndpointRequestUtil.isHillaAvailable()) {
            // Respond with 401 Unauthorized HTTP status code for unauthorized
            // requests for protected Hilla endpoints, so that the response
            // could be handled on the client side using, for example, Hilla's
            // `InvalidSessionMiddleware`.
            configurer.accessDeniedHandler(createAccessDeniedHandler())
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            getRequestUtil()::isEndpointRequest);
        }
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
        registry.requestMatchers(defaultPermitMatcher()).permitAll()
                .requestMatchers(toRequestPrincipalAwareMatcher(
                        getRequestUtil()::isSecuredFlowRoute))
                .authenticated();
        if (EndpointRequestUtil.isHillaAvailable()) {
            registry.requestMatchers(toRequestPrincipalAwareMatcher(
                    getRequestUtil()::isEndpointRequest)).authenticated();
        }
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
