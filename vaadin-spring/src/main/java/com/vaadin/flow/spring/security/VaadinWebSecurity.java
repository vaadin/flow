/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
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
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import com.vaadin.flow.spring.security.stateless.VaadinStatelessSecurityConfigurer;

/**
 * Provides basic Vaadin component-based security configuration for the project.
 * <p>
 * Sets up security rules for a Vaadin application and restricts all URLs except
 * for public resources and internal Vaadin URLs to authenticated user.
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
 */
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public abstract class VaadinWebSecurity {

    @Autowired
    private VaadinDefaultRequestCache vaadinDefaultRequestCache;

    @Autowired
    private RequestUtil requestUtil;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ViewAccessChecker viewAccessChecker;

    @Value("#{servletContext.contextPath}")
    private String servletContextPath;

    private final AuthenticationContext authenticationContext = new AuthenticationContext();

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
        LogoutConfigurer<?> logoutConfigurer = http
                .getConfigurer(LogoutConfigurer.class);
        authenticationContext.setLogoutHandlers(
                logoutConfigurer.getLogoutSuccessHandler(),
                logoutConfigurer.getLogoutHandlers());
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
            urlRegistry.requestMatchers(requestUtil::isFrameworkInternalRequest)
                    .permitAll();
            // Public endpoints are OK to access
            urlRegistry.requestMatchers(requestUtil::isAnonymousEndpoint)
                    .permitAll();
            // Public routes are OK to access
            urlRegistry.requestMatchers(requestUtil::isAnonymousRoute)
                    .permitAll();
            urlRegistry.requestMatchers(getDefaultHttpSecurityPermitMatcher(
                    requestUtil.getUrlMapping())).permitAll();

            // matcher for Vaadin static (public) resources
            urlRegistry.requestMatchers(getDefaultWebSecurityIgnoreMatcher(
                    requestUtil.getUrlMapping())).permitAll();

            // all other requests require authentication
            urlRegistry.anyRequest().authenticated();
        });

        // Enable view access control
        viewAccessChecker.enable();
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
        Stream.Builder<String> paths = Stream.builder();
        Stream.of(HandlerHelper.getPublicResourcesRequiringSecurityContext())
                .map(path -> RequestUtil.applyUrlMapping(urlMapping, path))
                .forEach(paths::add);

        return new OrRequestMatcher(paths.build()
                .map(AntPathRequestMatcher::new).collect(Collectors.toList()));
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
        Stream<String> mappingRelativePaths = Stream
                .of(HandlerHelper.getPublicResources())
                .map(path -> RequestUtil.applyUrlMapping(urlMapping, path));
        Stream<String> rootPaths = Stream
                .of(HandlerHelper.getPublicResourcesRoot());
        return new OrRequestMatcher(Stream
                .concat(mappingRelativePaths, rootPaths)
                .map(AntPathRequestMatcher::new).collect(Collectors.toList()));
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
        viewAccessChecker.setLoginView(hillaLoginViewPath);
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
        http.csrf(cfg -> cfg.ignoringRequestMatchers(
                new AntPathRequestMatcher(completeLoginPath)));
        configureLogout(http, logoutSuccessUrl);
        http.exceptionHandling(cfg -> cfg.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(completeLoginPath),
                AnyRequestMatcher.INSTANCE));
        viewAccessChecker.setLoginView(flowLoginView);
    }

    /**
     * Sets up the login page URI of the OAuth2 provider on the specified
     * HttpSecurity instance.
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
        http.oauth2Login(cfg -> cfg.loginPage(oauth2LoginPage).successHandler(
                getVaadinSavedRequestAwareAuthenticationSuccessHandler(http))
                .permitAll());
        viewAccessChecker.setLoginView(servletContextPath + oauth2LoginPage);
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
        VaadinStatelessSecurityConfigurer<HttpSecurity> vaadinStatelessSecurityConfigurer = new VaadinStatelessSecurityConfigurer<>();
        vaadinStatelessSecurityConfigurer.setSharedObjects(http);
        http.apply(vaadinStatelessSecurityConfigurer);

        // Workaround
        // https://github.com/spring-projects/spring-security/issues/12579 until
        // it is released
        SessionManagementConfigurer sessionManagementConfigurer = http
                .getConfigurer(SessionManagementConfigurer.class);
        Field f = SessionManagementConfigurer.class
                .getDeclaredField("sessionManagementSecurityContextRepository");
        f.setAccessible(true);
        f.set(sessionManagementConfigurer,
                http.getSharedObject(SecurityContextRepository.class));

        vaadinStatelessSecurityConfigurer.withSecretKey().secretKey(secretKey)
                .and().issuer(issuer).expiresIn(expiresIn);
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
     * Vaadin views access checker bean.
     * <p>
     * This getter can be used in implementing class to override logic of
     * <code>VaadinWebSecurity.setLoginView</code> methods and call
     * {@link ViewAccessChecker} methods explicitly.
     * <p>
     * Note that this bean is a field-autowired, thus this getter returns
     * <code>null</code> when called from the constructor of implementing class.
     *
     * @return {@link ViewAccessChecker} bean used by this VaadinWebSecurity
     *         configuration.
     */
    protected ViewAccessChecker getViewAccessChecker() {
        return viewAccessChecker;
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
        SimpleUrlLogoutSuccessHandler logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
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
}
