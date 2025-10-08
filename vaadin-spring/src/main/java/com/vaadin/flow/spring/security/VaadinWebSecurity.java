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

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.DelegatingAccessDeniedHandler;
import org.springframework.security.web.access.RequestMatcherDelegatingAccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
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
import com.vaadin.flow.spring.security.stateless.VaadinStatelessSecurityConfigurer;

/**
 * Provides basic Vaadin component-based security configuration for the project.
 * <p>
 * Sets up security rules for a Vaadin application and fully restricts all URLs
 * except for public resources and internal Vaadin URLs.
 * <p>
 * The default behavior can be altered by extending the public/protected methods
 * in the class.
 * <p>
 * Provides default bean implementations for {@link SecurityFilterChain} and
 * {@link WebSecurityCustomizer}.
 * <p>
 * To use this, create your own web security class by extending this class and
 * annotate it with <code>@EnableWebSecurity</code> and
 * <code>@Configuration</code>.
 * <p>
 * For example:
 *
 * <pre>
 * <code>
 * &#64;EnableWebSecurity
 * &#64;Configuration
 * public class MyWebSecurity extends VaadinWebSecurity {
 * }
 * </code>
 * </pre>
 *
 * @deprecated Use {@link VaadinSecurityConfigurer} instead. It follows the
 *             Spring's SecurityConfigurer pattern and we recommend use it to
 *             configure Spring Security with Vaadin:
 *
 *             <pre>
 * <code>&#64;Configuration
 * &#64;EnableWebSecurity
 * &#64;Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
 * public class SecurityConfig {
 *     &#64;Bean
 *     SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {}).build();
 *     }
 * }
 * </code>
 *             </pre>
 *
 *             Read more details in <a href=
 *             "https://vaadin.com/docs/latest/flow/security/vaadin-security-configurer">Security
 *             Configurer documentation.</a>
 */
@Deprecated(since = "24.9", forRemoval = true)
public abstract class VaadinWebSecurity {

    @Autowired
    private VaadinDefaultRequestCache vaadinDefaultRequestCache;

    @Autowired
    private RequestUtil requestUtil;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private VaadinRolePrefixHolder vaadinRolePrefixHolder;

    @Value("#{servletContext.contextPath}")
    private String servletContextPath;

    @Autowired
    private ObjectProvider<NavigationAccessControl> accessControlProvider;

    private NavigationAccessControl accessControl;

    @Autowired
    private SecurityContextHolderStrategy securityContextHolderStrategy;

    private AuthenticationContext authenticationContext;

    @PostConstruct
    void afterPropertiesSet() {
        accessControl = accessControlProvider.getIfAvailable();
        authenticationContext = new AuthenticationContext(
                securityContextHolderStrategy);
        authenticationContext.setRolePrefixHolder(vaadinRolePrefixHolder);
        SecurityContextHolder
                .setContextHolderStrategy(securityContextHolderStrategy);
    }

    /**
     * Registers default {@link SecurityFilterChain} bean.
     * <p>
     * Defines a filter chain which is capable of being matched against an
     * {@code HttpServletRequest}. in order to decide whether it applies to that
     * request.
     * <p>
     * {@link HttpSecurity} configuration can be customized by overriding
     * {@link VaadinWebSecurity#configure(HttpSecurity)}.
     */
    @Bean(name = "VaadinSecurityFilterChainBean")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configure(http);
        http.logout(cfg -> {
            cfg.invalidateHttpSession(true);
            addLogoutHandlers(cfg::addLogoutHandler);
        });

        DefaultSecurityFilterChain securityFilterChain = http.build();
        Optional.ofNullable(vaadinRolePrefixHolder)
                .ifPresent(vaadinRolePrefixHolder -> vaadinRolePrefixHolder
                        .resetRolePrefix(securityFilterChain));
        AuthenticationContext.applySecurityConfiguration(http,
                authenticationContext);
        return securityFilterChain;
    }

    /**
     * Gets the default authentication-context bean.
     *
     * @return the authentication-context bean
     */
    @Bean(name = "VaadinAuthenticationContext")
    public AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    /**
     * Applies Vaadin default configuration to {@link HttpSecurity}.
     *
     * Typically, subclasses should call super to apply default Vaadin
     * configuration in addition to custom rules.
     *
     * @param http
     *            the {@link HttpSecurity} to modify
     * @throws Exception
     *             if an error occurs
     */
    protected void configure(HttpSecurity http) throws Exception {
        // Respond with 401 Unauthorized HTTP status code for unauthorized
        // requests for protected Hilla endpoints, so that the response could
        // be handled on the client side using e.g. `InvalidSessionMiddleware`.
        http.exceptionHandling(
                cfg -> cfg.accessDeniedHandler(createAccessDeniedHandler())
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED),
                                requestUtil::isEndpointRequest));

        // Vaadin has its own CSRF protection.
        // Spring CSRF is not compatible with Vaadin internal requests
        http.csrf(cfg -> cfg.ignoringRequestMatchers(
                requestUtil::isFrameworkInternalRequest));

        // Ensure automated requests to e.g. closing push channels, service
        // workers,
        // endpoints are not counted as valid targets to redirect user to on
        // login
        http.requestCache(cfg -> cfg.requestCache(vaadinDefaultRequestCache));

        http.authorizeHttpRequests(urlRegistry -> {
            // Vaadin internal requests must always be allowed to allow public
            // Flow pages and/or login page implemented using Flow.
            urlRegistry
                    .requestMatchers(toRequestPrincipalAwareMatcher(
                            requestUtil::isFrameworkInternalRequest))
                    .permitAll();
            if (EndpointRequestUtil.isHillaAvailable()) {
                // Public endpoints are OK to access
                urlRegistry.requestMatchers(toRequestPrincipalAwareMatcher(
                        requestUtil::isAnonymousEndpoint)).permitAll();
                // Checks for known Hilla views
                urlRegistry.requestMatchers(toRequestPrincipalAwareMatcher(
                        requestUtil::isAllowedHillaView)).permitAll();
            }
            // Public routes are OK to access
            urlRegistry.requestMatchers(toRequestPrincipalAwareMatcher(
                    requestUtil::isAnonymousRoute)).permitAll();
            urlRegistry.requestMatchers(toRequestPrincipalAwareMatcher(
                    getDefaultHttpSecurityPermitMatcher(
                            requestUtil.getUrlMapping())))
                    .permitAll();
            // matcher for Vaadin static (public) resources
            urlRegistry.requestMatchers(toRequestPrincipalAwareMatcher(
                    getDefaultWebSecurityIgnoreMatcher(
                            requestUtil.getUrlMapping())))
                    .permitAll();
            // matcher for custom PWA icons and favicon
            urlRegistry.requestMatchers(requestUtil::isCustomWebIcon)
                    .permitAll();
            if (EndpointRequestUtil.isHillaAvailable()) {
                // Authenticated endpoints
                urlRegistry
                        .requestMatchers(toRequestPrincipalAwareMatcher(
                                requestUtil::isEndpointRequest))
                        .authenticated();
            }
            // private routes require authentication
            urlRegistry.requestMatchers(toRequestPrincipalAwareMatcher(
                    requestUtil::isSecuredFlowRoute)).authenticated();

            // all other requests are denied
        });

        accessControl.setEnabled(enableNavigationAccessControl());
    }

    /**
     * Registers default {@link WebSecurityCustomizer} bean.
     * <p>
     * Beans of this type will automatically be used by
     * {@link WebSecurityConfiguration} to customize {@link WebSecurity}.
     * <p>
     * {@link WebSecurity} configuration can be customized by overriding
     * {@link VaadinWebSecurity#configure(WebSecurity)}
     * <p>
     * Default no {@link WebSecurity} customization is performed.
     */
    @Bean(name = "VaadinWebSecurityCustomizerBean")
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> {
            try {
                configure(web);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected void configure(WebSecurity web) throws Exception {
        // no-operation
    }

    /**
     * Gets if navigation access control should be enabled.
     *
     * Navigation access control is enabled by default. This method can be
     * overridden returning {@literal false} to disable it.
     *
     * @return {@literal true} if navigation access control should be enabled,
     *         {@literal false} to disable it.
     */
    protected boolean enableNavigationAccessControl() {
        return true;
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
     * @return default {@link WebSecurity} ignore matcher
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
     * @return default {@link WebSecurity} ignore matcher
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

    /**
     * Utility to create {@link RequestMatcher}s from ant patterns.
     *
     * @param patterns
     *            ant patterns
     * @return an array or {@link RequestMatcher} instances for the given
     *         patterns.
     * @deprecated AntPathRequestMatcher is deprecated and will be removed, use
     *             {@link #pathMatchers(String...)} instead.
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public RequestMatcher[] antMatchers(String... patterns) {
        return RequestUtil.antMatchers(patterns);
    }

    /**
     * Utility to create {@link RequestMatcher}s for a Vaadin routes, using ant
     * patterns and HTTP get method.
     *
     * @param patterns
     *            ant patterns
     * @return an array or {@link RequestMatcher} instances for the given
     *         patterns.
     * @deprecated AntPathRequestMatcher is deprecated and will be removed, use
     *             {@link #routePathMatchers(String...)} instead.
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public RequestMatcher[] routeMatchers(String... patterns) {
        return RequestUtil.routeMatchers(Stream.of(patterns)
                .map(this::applyUrlMapping).toArray(String[]::new));
    }

    /**
     * Utility to create {@link RequestMatcher}s from path patterns.
     *
     * @param patterns
     *            path patterns, as described in
     *            {@link org.springframework.web.util.pattern.PathPattern}
     *            javadoc.
     * @return an array or {@link RequestMatcher} instances for the given
     *         patterns.
     * @see org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher#matcher(HttpServletRequest)
     * @see org.springframework.web.util.pattern.PathPattern
     */
    public RequestMatcher[] pathMatchers(String... patterns) {
        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher
                .withDefaults();
        return Stream.of(patterns).map(builder::matcher)
                .toArray(RequestMatcher[]::new);
    }

    /**
     * Utility to create {@link RequestMatcher}s for a Vaadin routes, using ant
     * patterns and HTTP get method.
     *
     * @param patterns
     *            path patterns, as described in
     *            {@link org.springframework.web.util.pattern.PathPattern}
     *            javadoc.
     * @return an array or {@link RequestMatcher} instances for the given
     *         patterns.
     * @see org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher#matcher(HttpServletRequest)
     * @see org.springframework.web.util.pattern.PathPattern
     */
    public RequestMatcher[] routePathMatchers(String... patterns) {
        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher
                .withDefaults();
        return Stream.of(patterns).map(p -> builder.matcher(HttpMethod.GET, p))
                .toArray(RequestMatcher[]::new);
    }

    /**
     * Sets up login for the application using form login with the given path
     * for the login view.
     * <p>
     * This is used when your application uses a Hilla based login view
     * available at the given path.
     *
     * NOTE: if the login path points to a Flow view, the corresponding java
     * class must be annotated
     * with @{@link com.vaadin.flow.server.auth.AnonymousAllowed} to ensure that
     * the view is always accessible.
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param hillaLoginViewPath
     *            the path to the login view
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http, String hillaLoginViewPath)
            throws Exception {
        setLoginView(http, hillaLoginViewPath, getDefaultLogoutUrl());
    }

    /**
     * Sets up login for the application using form login with the given path
     * for the login view.
     * <p>
     * This is used when your application uses a Hilla based login view
     * available at the given path.
     *
     * NOTE: if the login path points to a Flow view, the corresponding java
     * class must be annotated
     * with @{@link com.vaadin.flow.server.auth.AnonymousAllowed} to ensure that
     * the view is always accessible.
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param hillaLoginViewPath
     *            the path to the login view
     * @param logoutSuccessUrl
     *            the URL to redirect the user to after logging out
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http, String hillaLoginViewPath,
            String logoutSuccessUrl) throws Exception {
        String completeHillaLoginViewPath = applyUrlMapping(hillaLoginViewPath);
        http.formLogin(formLogin -> {
            formLogin.loginPage(completeHillaLoginViewPath).permitAll();
            formLogin.successHandler(
                    getVaadinSavedRequestAwareAuthenticationSuccessHandler(
                            http));
        });
        configureLogout(http, logoutSuccessUrl);
        http.exceptionHandling(cfg -> cfg.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(
                        completeHillaLoginViewPath),
                AnyRequestMatcher.INSTANCE));
        accessControl.setLoginView(hillaLoginViewPath);
    }

    /**
     * Sets up login for the application using the given Flow login view.
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param flowLoginView
     *            the login view to use
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http,
            Class<? extends Component> flowLoginView) throws Exception {
        setLoginView(http, flowLoginView, getDefaultLogoutUrl());
    }

    /**
     * Sets up login for the application using the given Flow login view.
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param flowLoginView
     *            the login view to use
     * @param logoutSuccessUrl
     *            the URL to redirect the user to after logging out
     *
     * @throws Exception
     *             if something goes wrong
     */
    protected void setLoginView(HttpSecurity http,
            Class<? extends Component> flowLoginView, String logoutSuccessUrl)
            throws Exception {
        Optional<Route> route = AnnotationReader.getAnnotationFor(flowLoginView,
                Route.class);

        if (!route.isPresent()) {
            throw new IllegalArgumentException(
                    "Unable find a @Route annotation on the login view "
                            + flowLoginView.getName());
        }

        if (!(applicationContext instanceof WebApplicationContext)) {
            throw new RuntimeException(
                    "VaadinWebSecurity cannot be used without WebApplicationContext.");
        }

        VaadinServletContext vaadinServletContext = new VaadinServletContext(
                ((WebApplicationContext) applicationContext)
                        .getServletContext());
        String loginPath = RouteUtil.getRoutePath(vaadinServletContext,
                flowLoginView);
        if (!loginPath.startsWith("/")) {
            loginPath = "/" + loginPath;
        }
        String completeLoginPath = applyUrlMapping(loginPath);

        // Actually set it up
        http.formLogin(formLogin -> {
            formLogin.loginPage(completeLoginPath).permitAll();
            formLogin.successHandler(
                    getVaadinSavedRequestAwareAuthenticationSuccessHandler(
                            http));
        });
        http.csrf(cfg -> cfg.ignoringRequestMatchers(PathPatternRequestMatcher
                .withDefaults().matcher(completeLoginPath)));
        configureLogout(http, logoutSuccessUrl);
        http.exceptionHandling(cfg -> cfg.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(completeLoginPath),
                AnyRequestMatcher.INSTANCE));
        accessControl.setLoginView(flowLoginView);
    }

    /**
     * Sets up the login page URI of the OAuth2 provider on the specified
     * HttpSecurity instance.
     * <p>
     * </p>
     * This method also configures a logout success handler that redirects to
     * the application base URL after logout.
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param oauth2LoginPage
     *            the login page of the OAuth2 provider. This Specifies the URL
     *            to send users to if login is required.
     * @throws Exception
     *             Re-throws the possible exceptions while activating
     *             OAuth2LoginConfigurer
     */
    protected void setOAuth2LoginPage(HttpSecurity http, String oauth2LoginPage)
            throws Exception {
        setOAuth2LoginPage(http, oauth2LoginPage, "{baseUrl}");
    }

    /**
     * Sets up the login page URI of the OAuth2 provider and the post logout URI
     * on the specified HttpSecurity instance.
     * <p>
     * </p>
     * The post logout redirect uri can be relative or absolute URI or a
     * template. The supported uri template variables are: {baseScheme},
     * {baseHost}, {basePort} and {basePath}.
     * <p>
     * </p>
     * NOTE: "{baseUrl}" is also supported, which is the same as
     * "{baseScheme}://{baseHost}{basePort}{basePath}" handler.
     * setPostLogoutRedirectUri("{baseUrl}");
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param oauth2LoginPage
     *            the login page of the OAuth2 provider. This Specifies the URL
     *            to send users to if login is required.
     * @param postLogoutRedirectUri
     *            the post logout redirect uri. Can be a template.
     * @throws Exception
     *             Re-throws the possible exceptions while activating
     *             OAuth2LoginConfigurer
     */
    protected void setOAuth2LoginPage(HttpSecurity http, String oauth2LoginPage,
            String postLogoutRedirectUri) throws Exception {
        http.oauth2Login(cfg -> cfg.loginPage(oauth2LoginPage).successHandler(
                getVaadinSavedRequestAwareAuthenticationSuccessHandler(http))
                .permitAll());
        accessControl.setLoginView(servletContextPath + oauth2LoginPage);
        if (postLogoutRedirectUri != null) {
            applicationContext
                    .getBeanProvider(ClientRegistrationRepository.class)
                    .getIfAvailable();
            var logoutSuccessHandler = oidcLogoutSuccessHandler(
                    postLogoutRedirectUri);
            if (logoutSuccessHandler != null) {
                http.logout(
                        cfg -> cfg.logoutSuccessHandler(logoutSuccessHandler));
            }
        }
    }

    /**
     * Gets a {@code OidcClientInitiatedLogoutSuccessHandler} instance that
     * redirects to the given URL after logout.
     * <p>
     * </p>
     * If a {@code ClientRegistrationRepository} bean is not registered in the
     * application context, the method returns {@literal null}.
     *
     * @param postLogoutRedirectUri
     *            the post logout redirect uri
     * @return a {@code OidcClientInitiatedLogoutSuccessHandler}, or
     *         {@literal null} if a {@code ClientRegistrationRepository} bean is
     *         not registered in the application context.
     */
    // Using base interface as return type to avoid potential
    // ClassNotFoundException when Spring Boot introspect configuration class
    // during startup, if spring-security-oauth2-client is not on classpath
    protected LogoutSuccessHandler oidcLogoutSuccessHandler(
            String postLogoutRedirectUri) {
        var clientRegistrationRepository = applicationContext
                .getBeanProvider(ClientRegistrationRepository.class)
                .getIfAvailable();
        if (clientRegistrationRepository != null) {
            var logoutHandler = new OidcClientInitiatedLogoutSuccessHandler(
                    clientRegistrationRepository);
            logoutHandler.setRedirectStrategy(new UidlRedirectStrategy());
            logoutHandler.setPostLogoutRedirectUri(postLogoutRedirectUri);
            return logoutHandler;
        }
        LoggerFactory.getLogger(VaadinWebSecurity.class).warn(
                "Cannot create OidcClientInitiatedLogoutSuccessHandler because ClientRegistrationRepository bean is not available.");
        return null;
    }

    /**
     * Sets up stateless JWT authentication using cookies.
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param secretKey
     *            the secret key for encoding and decoding JWTs, must use a
     *            {@link MacAlgorithm} algorithm name
     * @param issuer
     *            the issuer JWT claim
     * @throws Exception
     *             if something goes wrong
     */
    protected void setStatelessAuthentication(HttpSecurity http,
            SecretKey secretKey, String issuer) throws Exception {
        setStatelessAuthentication(http, secretKey, issuer, 1800L);
    }

    /**
     * Sets up stateless JWT authentication using cookies.
     *
     * @param http
     *            the http security from {@link #filterChain(HttpSecurity)}
     * @param secretKey
     *            the secret key for encoding and decoding JWTs, must use a
     *            {@link MacAlgorithm} algorithm name
     * @param issuer
     *            the issuer JWT claim
     * @param expiresIn
     *            lifetime of the JWT and cookies, in seconds
     * @throws Exception
     *             if something goes wrong
     */
    protected void setStatelessAuthentication(HttpSecurity http,
            SecretKey secretKey, String issuer, long expiresIn)
            throws Exception {
        http.with(new VaadinStatelessSecurityConfigurer<>(),
                cfg -> cfg.withSecretKey().secretKey(secretKey).and()
                        .issuer(issuer).expiresIn(expiresIn));
    }

    /**
     * Helper method to prepend configured servlet path to the given path.
     *
     * Path will always be considered as relative to servlet path, even if it
     * starts with a slash character.
     *
     * @param path
     *            path to be prefixed with servlet path
     * @return the input path prepended by servlet path.
     */
    protected String applyUrlMapping(String path) {
        return requestUtil.applyUrlMapping(path);
    }

    /**
     * Vaadin navigation access control bean.
     * <p>
     * This getter can be used in implementing class to override logic of
     * <code>VaadinWebSecurity.setLoginView</code> methods and call
     * {@link NavigationAccessControl} methods explicitly.
     * <p>
     * Note that this bean is a field-autowired, thus this getter returns
     * <code>null</code> when called from the constructor of implementing class.
     *
     * @return {@link NavigationAccessControl} bean used by this
     *         VaadinWebSecurity configuration.
     */
    protected NavigationAccessControl getNavigationAccessControl() {
        return accessControl;
    }

    /**
     * Sets additional {@link LogoutHandler}s that will participate in logout
     * process.
     *
     * @param registry
     *            used to add custom handlers.
     */
    protected void addLogoutHandlers(Consumer<LogoutHandler> registry) {

    }

    private void configureLogout(HttpSecurity http, String logoutSuccessUrl)
            throws Exception {
        VaadinSimpleUrlLogoutSuccessHandler logoutSuccessHandler = new VaadinSimpleUrlLogoutSuccessHandler();
        logoutSuccessHandler.setDefaultTargetUrl(logoutSuccessUrl);
        logoutSuccessHandler.setRedirectStrategy(new UidlRedirectStrategy());
        http.logout(cfg -> cfg.logoutSuccessHandler(logoutSuccessHandler));
    }

    private String getDefaultLogoutUrl() {
        return servletContextPath.startsWith("/") ? servletContextPath
                : "/" + servletContextPath;
    }

    private VaadinSavedRequestAwareAuthenticationSuccessHandler getVaadinSavedRequestAwareAuthenticationSuccessHandler(
            HttpSecurity http) {
        VaadinSavedRequestAwareAuthenticationSuccessHandler vaadinSavedRequestAwareAuthenticationSuccessHandler = new VaadinSavedRequestAwareAuthenticationSuccessHandler();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .setDefaultTargetUrl(applyUrlMapping(""));
        RequestCache requestCache = http.getSharedObject(RequestCache.class);
        if (requestCache != null) {
            vaadinSavedRequestAwareAuthenticationSuccessHandler
                    .setRequestCache(requestCache);
        }
        http.setSharedObject(
                VaadinSavedRequestAwareAuthenticationSuccessHandler.class,
                vaadinSavedRequestAwareAuthenticationSuccessHandler);
        return vaadinSavedRequestAwareAuthenticationSuccessHandler;
    }

    private AccessDeniedHandler createAccessDeniedHandler() {
        final AccessDeniedHandler defaultHandler = new AccessDeniedHandlerImpl();

        final AccessDeniedHandler http401UnauthorizedHandler = new Http401UnauthorizedAccessDeniedHandler();

        final LinkedHashMap<Class<? extends AccessDeniedException>, AccessDeniedHandler> exceptionHandlers = new LinkedHashMap<>();
        exceptionHandlers.put(CsrfException.class, http401UnauthorizedHandler);

        final LinkedHashMap<RequestMatcher, AccessDeniedHandler> matcherHandlers = new LinkedHashMap<>();
        matcherHandlers.put(requestUtil::isEndpointRequest,
                new DelegatingAccessDeniedHandler(exceptionHandlers,
                        new AccessDeniedHandlerImpl()));

        return new RequestMatcherDelegatingAccessDeniedHandler(matcherHandlers,
                defaultHandler);
    }

    private static class Http401UnauthorizedAccessDeniedHandler
            implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request,
                HttpServletResponse response,
                AccessDeniedException accessDeniedException)
                throws IOException, ServletException {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    private RequestMatcher toRequestPrincipalAwareMatcher(
            RequestMatcher matcher) {
        if (enableNavigationAccessControl() && getNavigationAccessControl()
                .hasAccessChecker(RoutePathAccessChecker.class)) {
            return RequestUtil.principalAwareRequestMatcher(matcher);
        }
        return matcher;
    }

}
