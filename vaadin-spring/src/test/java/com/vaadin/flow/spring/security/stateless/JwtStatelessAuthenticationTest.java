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
package com.vaadin.flow.spring.security.stateless;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;
import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinWebSecurity;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@WebMvcTest
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class,
        JwtStatelessAuthenticationTest.WorkaroundConfig.class,
        JwtStatelessAuthenticationTest.SecurityConfig.class })
@TestPropertySource(properties = """
        spring.main.allow-bean-definition-overriding=true
        """)
class JwtStatelessAuthenticationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    @Qualifier("VaadinSecurityFilterChainBean")
    SecurityFilterChain securityFilterChain;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(
                springSecurity(new FilterChainProxy(securityFilterChain)))
                .build();
    }

    @Test
    void publicResource_notAuthenticated_jwtCookieNotSet() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("jwt.headerAndPayload"))
                .andExpect(cookie().doesNotExist("jwt.signature"));
    }

    @Test
    @WithMockUser
    void publicResource_authenticated_jwtCookieSet() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(
                        cookie().maxAge("jwt.headerAndPayload", greaterThan(0)))
                .andExpect(cookie().maxAge("jwt.signature", greaterThan(0)));
    }

    @Test
    void successfulAuthentication_jwtCookieSet() throws Exception {
        doLogin();
    }

    @Test
    void authenticated_protected_jwtCookieUpdatedAtEveryRequest()
            throws Exception {
        MvcResult result = doLogin();

        Cookie jwtCookie = result.getResponse()
                .getCookie("jwt.headerAndPayload");
        Cookie jwtSignature = result.getResponse().getCookie("jwt.signature");

        // Wait for a while, to be sure expire time is different
        Thread.sleep(1500);
        result = this.mvc
                .perform(get("/protected").with(csrfCookie()).cookie(jwtCookie,
                        jwtSignature))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("user"))
                .andExpect(cookie().exists("jwt.signature"))
                .andExpect(cookie().exists("jwt.headerAndPayload")).andReturn();

        assertCookiesUpdated(result, jwtCookie, jwtSignature);
    }

    @Test
    void authenticated_restricted_jwtCookieUpdatedAtEveryRequest()
            throws Exception {
        MvcResult result = doLogin();

        Cookie jwtCookie = result.getResponse()
                .getCookie("jwt.headerAndPayload");
        Cookie jwtSignature = result.getResponse().getCookie("jwt.signature");

        // Wait for a while, to be sure expire time is different
        Thread.sleep(1500);
        result = this.mvc
                .perform(get("/restricted").with(csrfCookie()).cookie(jwtCookie,
                        jwtSignature))
                .andExpect(status().isForbidden())
                .andExpect(authenticated().withUsername("user"))
                .andExpect(cookie().exists("jwt.signature"))
                .andExpect(cookie().exists("jwt.headerAndPayload")).andReturn();

        assertCookiesUpdated(result, jwtCookie, jwtSignature);
    }

    @Test
    void logout_jwtCookieExpired() throws Exception {
        MvcResult loginResult = doLogin();
        this.mvc.perform(post("/logout").with(csrfCookie())
                .cookie(jwtCookiesFromResult(loginResult)))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated())
                .andExpect(cookie().maxAge("jwt.signature", 0))
                .andExpect(cookie().maxAge("jwt.headerAndPayload", 0));
    }

    private Cookie[] jwtCookiesFromResult(MvcResult result) {
        Cookie jwtCookie = result.getResponse()
                .getCookie("jwt.headerAndPayload");
        Cookie jwtSignature = result.getResponse().getCookie("jwt.signature");
        return new Cookie[] { jwtCookie, jwtSignature };
    }

    private MvcResult doLogin() throws Exception {
        return this.mvc
                .perform(post("/login").param("username", "user")
                        .param("password", "password").with(csrfCookie()))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("user"))
                .andExpect(cookie().maxAge("jwt.signature", greaterThan(0)))
                .andExpect(
                        cookie().maxAge("jwt.headerAndPayload", greaterThan(0)))
                .andReturn();
    }

    private static RequestPostProcessor csrfCookie() {
        return request -> {
            CsrfTokenRepository repository = WebTestUtils
                    .getCsrfTokenRepository(request);
            CsrfTokenRequestHandler handler = WebTestUtils
                    .getCsrfTokenRequestHandler(request);
            MockHttpServletResponse response = new MockHttpServletResponse();
            DeferredCsrfToken deferredCsrfToken = repository
                    .loadDeferredToken(request, response);
            handler.handle(request, response, deferredCsrfToken::get);
            CsrfToken token = (CsrfToken) request
                    .getAttribute(CsrfToken.class.getName());
            token.getToken();
            Cookie csrfCookie = response.getCookie("XSRF-TOKEN");
            request.addHeader("X-XSRF-TOKEN", csrfCookie.getValue());

            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                cookies = Arrays.copyOf(cookies, cookies.length + 1);
                cookies[cookies.length - 1] = csrfCookie;
            } else {
                cookies = new Cookie[] { csrfCookie };
            }

            request.setCookies(cookies);

            return request;
        };
    }

    private static void assertCookiesUpdated(MvcResult result, Cookie jwtCookie,
            Cookie jwtSignature) {
        Cookie jwtCookie2 = result.getResponse()
                .getCookie("jwt.headerAndPayload");
        Assertions.assertNotEquals(jwtCookie.getValue(), jwtCookie2.getValue());
        Cookie jwtSignature2 = result.getResponse().getCookie("jwt.signature");
        Assertions.assertNotEquals(jwtSignature.getValue(),
                jwtSignature2.getValue());
    }

    @TestConfiguration
    @Import(FakeController.class)
    public static class SecurityConfig extends VaadinWebSecurity {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(
                    auth -> auth.requestMatchers(antMatchers("/")).permitAll()
                            .requestMatchers("/protected").authenticated());
            super.configure(http);
            setLoginView(http, "login");
            setStatelessAuthentication(http,
                    new SecretKeySpec(Base64.getDecoder().decode(
                            "YOc+XUfRA/cPGNTEsHfU897W0VYF1nrLNWrsGEI1rBw="),
                            JwsAlgorithms.HS256),
                    "someone", 2000);
        }

        @Bean
        UserDetailsService userDetailsService() {
            @SuppressWarnings("deprecation")
            UserDetails user = User.withDefaultPasswordEncoder()
                    .username("user").password("password").roles("USER")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }

    }

    @TestConfiguration
    static class WorkaroundConfig {

        // Workaround for https://github.com/vaadin/flow/issues/18965
        // Needs 'spring.main.allow-bean-definition-overriding=true' to be set
        @Bean("requestUtil")
        RequestUtil requestUtilWorkAround() {
            return new RequestUtil() {
                @Override
                public boolean isCustomWebIcon(HttpServletRequest request) {
                    return false;
                }
            };
        }

    }

    @Controller
    static class FakeController {

        @GetMapping
        public String index() {
            return "OK";
        }

        @GetMapping("/protected")
        public String protectedView() {
            return "PROTECTED";
        }

        @GetMapping("/restricted")
        public String restrictedView() {
            return "RESTRICTED";
        }

    }
}
