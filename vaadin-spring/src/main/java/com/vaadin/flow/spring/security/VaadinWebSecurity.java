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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.SecretKey;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import com.vaadin.flow.spring.security.VaadinWebSecurityConfigurer.MarkerLogoutSuccessHandler;
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
    private RequestUtil requestUtil;

    @Value("#{servletContext.contextPath}")
    private String servletContextPath;

    @Autowired
    private NavigationAccessControl accessControl;

    private final AuthenticationContext authenticationContext = new AuthenticationContext();

    private final VaadinWebSecurityConfigurer configurer = new VaadinWebSecurityConfigurer();

    private boolean defaultsConfigured = false;

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
        http.with(configurer, vwsc -> {
            vwsc.urlMapper(this::applyUrlMapping);
            vwsc.authenticationContext(getAuthenticationContext());
            vwsc.navigationAccessControl(getNavigationAccessControl());
            vwsc.enableNavigationAccessControl(enableNavigationAccessControl());
            addLogoutHandlers(vwsc::addToLogoutHandlers);
        });
        // Set a "marker" handler to detect later if a custom one is set. This
        // is necessary since the VaadinWebSecurityConfigurer will later set
        // its own handler, which will override the custom one if set. This
        // marker will allow the configurer to detect the custom handler and
        // avoid overriding it.
        http.logout(logout -> logout
                .logoutSuccessHandler(new MarkerLogoutSuccessHandler()));
        configure(http);
        configurer.shouldConfigureDefaults = defaultsConfigured;
        return http.build();
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
        // Logic in this method before https://github.com/vaadin/flow/pull/21373
        // is now delegated to {@link VaadinWebSecurityConfigurer#init}, so it
        // is necessary to track if this method has been called or not since
        // subclasses might not call super.configure(http) at all.
        defaultsConfigured = true;
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
     * Utility to create {@link RequestMatcher}s from ant patterns.
     *
     * @param patterns
     *            ant patterns
     * @return an array or {@link RequestMatcher} instances for the given
     *         patterns.
     */
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
     */
    public RequestMatcher[] routeMatchers(String... patterns) {
        return RequestUtil.routeMatchers(Stream.of(patterns)
                .map(this::applyUrlMapping).toArray(String[]::new));
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
        configurer.loginView(hillaLoginViewPath, logoutSuccessUrl);
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
        configurer.loginView(flowLoginView, logoutSuccessUrl);
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
        configurer.oauth2LoginPage(oauth2LoginPage, postLogoutRedirectUri);
        // This is needed for backwards compatibility if the protected method
        // oidcLogoutSuccessHandler has been overridden by subclasses, to ensure
        // that the custom LogoutSuccessHandler is used by the configurer.
        if (postLogoutRedirectUri != null) {
            var logoutSuccessHandler = oidcLogoutSuccessHandler(
                    postLogoutRedirectUri);
            if (logoutSuccessHandler != null) {
                http.setSharedObject(LogoutSuccessHandler.class,
                        logoutSuccessHandler);
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
        return configurer.createOidcLogoutSuccessHandler(postLogoutRedirectUri)
                .orElse(null);
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
        VaadinStatelessSecurityConfigurer.apply(http,
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
     * @deprecated ViewAccessChecker is not used anymore by VaadinWebSecurity,
     *             and has been replaced by {@link NavigationAccessControl}.
     *             Calling this method will get a stub implementation that
     *             delegates to the {@link NavigationAccessControl} instance.
     */
    @Deprecated(forRemoval = true, since = "24.3")
    protected ViewAccessChecker getViewAccessChecker() {
        LoggerFactory.getLogger(getClass()).warn(
                "ViewAccessChecker is not used anymore by VaadinWebSecurity "
                        + "and has been replaced by NavigationAccessControl. "
                        + "'VaadinWebSecurity.getViewAccessChecker()' returns a stub instance that "
                        + "delegates calls to NavigationAccessControl. "
                        + "Usages of 'getViewAccessChecker()' should be replaced by calls to 'getNavigationAccessControl()'.");
        return new DeprecateViewAccessCheckerDelegator(accessControl);
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

    private String getDefaultLogoutUrl() {
        return servletContextPath.startsWith("/") ? servletContextPath
                : "/" + servletContextPath;
    }

    private static class DeprecateViewAccessCheckerDelegator
            extends ViewAccessChecker {

        private final NavigationAccessControl accessControl;

        public DeprecateViewAccessCheckerDelegator(
                NavigationAccessControl acc) {
            this.accessControl = acc;
        }

        @Override
        public void enable() {
            accessControl.setEnabled(true);
        }

        @Override
        public void setLoginView(Class<? extends Component> loginView) {
            accessControl.setLoginView(loginView);
        }

        @Override
        public void setLoginView(String loginUrl) {
            accessControl.setLoginView(loginUrl);
        }

        @Override
        public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
            accessControl.beforeEnter(beforeEnterEvent);
        }
    }
}
