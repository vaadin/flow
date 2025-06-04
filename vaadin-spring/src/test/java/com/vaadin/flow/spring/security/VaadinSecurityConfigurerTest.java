package com.vaadin.flow.spring.security;

import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class,
        ObjectPostProcessorConfiguration.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
class VaadinSecurityConfigurerTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectPostProcessor<Object> postProcessor;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private HttpSecurity http;

    private VaadinSecurityConfigurer configurer;

    @BeforeEach
    void setUp() {
        var authManagerBuilder = new AuthenticationManagerBuilder(postProcessor)
                .authenticationProvider(new TestingAuthenticationProvider());
        http = new HttpSecurity(postProcessor, authManagerBuilder,
                Map.of(ApplicationContext.class, applicationContext));
        configurer = VaadinSecurityConfigurer.vaadin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withDefaults_chainHasDefaultFilters() throws Exception {
        var filters = http.with(configurer, Customizer.withDefaults()).build()
                .getFilters();

        assertThat(filters).hasOnlyElementsOfTypes(CsrfFilter.class,
                LogoutFilter.class, AuthorizationFilter.class,
                RequestCacheAwareFilter.class,
                ExceptionTranslationFilter.class);
    }

    @Test
    void loginViewClass_chainHasAuthenticationFilter() throws Exception {
        var filters = http.with(configurer, c -> {
            c.loginView(TestLoginView.class);
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    void loginViewString_chainHasAuthenticationFilter() throws Exception {
        var filters = http.with(configurer, c -> {
            c.loginView("/login");
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    void oauth2LoginPage_chainHasAuthenticationFilter() throws Exception {
        var filters = http.with(configurer, c -> {
            c.oauth2LoginPage("/oauth2/login");
        }).build().getFilters();

        assertThat(filters).hasAtLeastOneElementOfType(
                OAuth2LoginAuthenticationFilter.class);
    }

    @Test
    void logoutSuccessHandler_handlerIsConfigured(
            @Mock LogoutSuccessHandler handler) throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(request.getMethod()).thenReturn("POST");
        when(request.getPathInfo()).thenReturn("/logout");

        var filters = http.with(configurer, c -> {
            c.logoutSuccessHandler(handler);
        }).build().getFilters();

        assertThat(filters).filteredOn(LogoutFilter.class::isInstance)
                .singleElement().satisfies(filter -> {
                    filter.doFilter(request, response, chain);
                    verify(handler).onLogoutSuccess(request, response, auth);
                });
    }

    @Test
    void addLogoutHandler_handlerIsAdded(@Mock LogoutHandler handler)
            throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(request.getMethod()).thenReturn("POST");
        when(request.getPathInfo()).thenReturn("/logout");

        var filters = http.with(configurer, c -> {
            c.addLogoutHandler(handler);
        }).build().getFilters();

        assertThat(filters).filteredOn(LogoutFilter.class::isInstance)
                .singleElement().satisfies(filter -> {
                    filter.doFilter(request, response, chain);
                    verify(handler).logout(request, response, auth);
                });
    }

    @Test
    void anyRequest_authorizeRuleIsConfigured() throws Exception {
        var auth = new AnonymousAuthenticationToken("key", "user",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(request.getPathInfo()).thenReturn("/any");

        var filters = http.with(configurer, c -> {
            c.anyRequest(AuthorizedUrl::anonymous);
        }).build().getFilters();

        assertThat(filters).filteredOn(AuthorizationFilter.class::isInstance)
                .singleElement()
                .satisfies(filter -> assertThatCode(
                        () -> filter.doFilter(request, response, chain))
                        .doesNotThrowAnyException());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void enableNavigationAccessControl_navigationAccessControlIsConfigured(
            boolean enableNavigationAccessControl) throws Exception {
        http.with(configurer, c -> {
            c.enableNavigationAccessControl(enableNavigationAccessControl);
        }).build();

        var nac = http.getSharedObject(NavigationAccessControl.class);
        assertThat(nac.isEnabled()).isEqualTo(enableNavigationAccessControl);
    }

    @Test
    @SuppressWarnings("unchecked")
    void disableDefaultConfigurers_configurersAreNotApplied() throws Exception {
        http.with(configurer, c -> {
            c.enableCsrfConfiguration(false);
            c.enableLogoutConfiguration(false);
            c.enableRequestCacheConfiguration(false);
            c.enableExceptionHandlingConfiguration(false);
            c.enableAuthorizedRequestsConfiguration(false);
        }).build();

        assertThat(http.getConfigurer(CsrfConfigurer.class)).isNull();
        assertThat(http.getConfigurer(LogoutConfigurer.class)).isNull();
        assertThat(http.getConfigurer(RequestCacheConfigurer.class)).isNull();
        assertThat(http.getConfigurer(ExceptionHandlingConfigurer.class))
                .isNull();
        assertThat(http.getConfigurer(AuthorizeHttpRequestsConfigurer.class))
                .isNull();
    }

    @Test
    void requestCache_customRulesAreApplied() throws Exception {
        VaadinDefaultRequestCache requestCache = applicationContext
                .getBean(VaadinDefaultRequestCache.class);
        requestCache.ignoreRequests(new AntPathRequestMatcher("/.my-path/**"));

        http.with(configurer, Customizer.withDefaults()).build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPathInfo("/.my-path/foo");
        requestCache.saveRequest(request, response);
        assertNull(requestCache.getRequest(request, response),
                "Request should not have been saved");
    }

    @Route
    static class TestLoginView extends Component {
    }
}
