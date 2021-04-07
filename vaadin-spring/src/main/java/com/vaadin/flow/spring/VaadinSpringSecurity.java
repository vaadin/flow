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
package com.vaadin.flow.spring;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import com.vaadin.flow.server.HandlerHelper;

/**
 * Helpers for Spring Security configuration of Vaadin applications.
 *
 * Applications that use Spring Security typically need to bypass security
 * checks for static resources. This class contains helper methods for setting
 * up a {@link WebSecurityConfigurerAdapter} that allows static resources and
 * the app shell to retrieved without authentication while requiring
 * authentication for routes.
 *
 * @see EnableWebSecurity
 * @see WebSecurityConfigurerAdapter
 * @see WebSecurityConfigurerAdapter#configure(WebSecurity)
 *
 */
public class VaadinSpringSecurity {

    private VaadinSpringSecurity() {
    }

    /**
     * Matcher for app shell (index page) and framework internal requests.
     *
     * @return default {@link HttpSecurity} bypass matcher
     */
    public static RequestMatcher getDefaultHttpSecurityPermitMatcher() {

        return new OrRequestMatcher(Stream.of(
                "/vaadinServlet/**",
                "/VAADIN/**")
                .map(AntPathRequestMatcher::new)
                .collect(Collectors.toList()));
    }

    /**
     * Matcher for Vaadin static resources.
     *
     * @return default {@link WebSecurity} ignore matcher
     */
    public static RequestMatcher getDefaultWebSecurityIgnoreMatcher() {
        return new OrRequestMatcher(Stream
                .of(HandlerHelper.getPublicResources())
                .map(AntPathRequestMatcher::new)
                .collect(Collectors.toList()));
    }

    /**
     * Configure Spring Boot to bypass security for framework internal urls
     * the app shell (index page), but require it for all other routes.
     *
     * @param http the {@link HttpSecurity} instance
     *
     * @throws Exception thrown by {@link HttpSecurity#authorizeRequests()}
     */
    public static void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .requestMatchers(getDefaultHttpSecurityPermitMatcher()).permitAll()
                // all other requests require authentication
                .anyRequest().authenticated();
    }

    /**
     * Configure Spring Boot to bypass security for default static assets
     * (favicon, content in {@code icons} and {@code images} folders, default
     * offline page, and service worker.
     *
     * @param web the {@link WebSecurity} instance
     */
    public static void configure(WebSecurity web)  {
        web.ignoring().requestMatchers(getDefaultWebSecurityIgnoreMatcher());
    }
}
