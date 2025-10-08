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
package com.vaadin.flow.internal.springcsrf;

import jakarta.servlet.ServletRequest;

import java.util.Optional;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.VaadinRequest;

/**
 * A util class for helping dealing with Spring CSRF token.
 */
public class SpringCsrfTokenUtil {
    private static final String CONTENT_ATTRIBUTE = "content";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String SPRING_CSRF_TOKEN_ATTRIBUTE_IN_REQUEST = "org.springframework.security.web.csrf.CsrfToken";
    private static final String SPRING_CSRF_HEADER_PROPERTY = "headerName";
    private static final String SPRING_CSRF_PARAMETER_PROPERTY = "parameterName";
    private static final String SPRING_CSRF_TOKEN_PROPERTY = "token";
    private static final String SPRING_CSRF_PARAMETER_NAME_ATTRIBUTE = "_csrf_parameter";
    private static final String SPRING_CSRF_HEADER_NAME_ATTRIBUTE = "_csrf_header";
    private static final String SPRING_CSRF_TOKEN_ATTRIBUTE = "_csrf";
    private static final String SPRING_CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String META_TAG = "meta";

    private SpringCsrfTokenUtil() {

    }

    public static Optional<SpringCsrfToken> getSpringCsrfToken(
            VaadinRequest request) {
        Object springCsrfToken = request
                .getAttribute(SPRING_CSRF_TOKEN_ATTRIBUTE_IN_REQUEST);
        return extractTokenFromBean(springCsrfToken);
    }

    public static Optional<SpringCsrfToken> getSpringCsrfToken(
            ServletRequest request) {
        Object springCsrfToken = request
                .getAttribute(SPRING_CSRF_TOKEN_ATTRIBUTE_IN_REQUEST);
        return extractTokenFromBean(springCsrfToken);
    }

    private static Optional<SpringCsrfToken> extractTokenFromBean(
            Object springCsrfToken) {
        if (springCsrfToken != null) {
            JsonNode springCsrfTokenJson = JacksonUtils
                    .beanToJson(springCsrfToken);
            if (springCsrfTokenJson != null
                    && springCsrfTokenJson.has(SPRING_CSRF_TOKEN_PROPERTY)
                    && springCsrfTokenJson.has(SPRING_CSRF_HEADER_PROPERTY)) {
                String token = springCsrfTokenJson
                        .get(SPRING_CSRF_TOKEN_PROPERTY).asString();
                String headerName = springCsrfTokenJson
                        .get(SPRING_CSRF_HEADER_PROPERTY).asString();
                String parameterName = springCsrfTokenJson
                        .get(SPRING_CSRF_PARAMETER_PROPERTY).asString();

                return Optional.of(
                        new SpringCsrfToken(headerName, parameterName, token));
            }
        }
        return Optional.empty();
    }

    public static void addTokenAsMetaTagsToHeadIfPresentInRequest(Element head,
            VaadinRequest request) {
        Optional<SpringCsrfToken> springCsrfToken = getSpringCsrfToken(request);
        springCsrfToken.ifPresent(csrfToken -> {
            addFormDataHandlerScriptToHead(head, csrfToken.getParameterName(),
                    SPRING_CSRF_COOKIE_NAME);
            addMetaTagToHead(head, SPRING_CSRF_TOKEN_ATTRIBUTE,
                    csrfToken.getToken());
            addMetaTagToHead(head, SPRING_CSRF_HEADER_NAME_ATTRIBUTE,
                    csrfToken.getHeaderName());
            addMetaTagToHead(head, SPRING_CSRF_PARAMETER_NAME_ATTRIBUTE,
                    csrfToken.getParameterName());
        });
    }

    private static void addMetaTagToHead(Element head, String name,
            String value) {
        Element meta = new Element(META_TAG);
        meta.attr(NAME_ATTRIBUTE, name);
        meta.attr(CONTENT_ATTRIBUTE, value);
        head.insertChildren(0, meta);
    }

    private static void addFormDataHandlerScriptToHead(Element head,
            String parameterName, String cookieName) {
        // Replace the CSRF form data parameter with the value from cookies for
        // compatibility with `CookieCsrfTokenRepository` in Spring Security.
        // Essential for the login form to work with stateless authentication.
        // See: https://github.com/vaadin/hilla/issues/910
        Element script = new Element("script");
        script.attr("type", "module");
        script.appendChild(new DataNode(
                """
                        const csrfParameterName = '%s';
                        const csrfCookieName = '%s';
                        window.addEventListener('formdata', (e) => {
                          if (!e.formData.has(csrfParameterName)) {
                            return;
                          }

                          const cookies = new URLSearchParams(document.cookie.replace(/;\\s*/, '&'));
                          if (!cookies.has(csrfCookieName)) {
                            return;
                          }

                          e.formData.set(csrfParameterName, cookies.get(csrfCookieName));
                        });
                        """
                        .formatted(parameterName, cookieName)));
        head.insertChildren(0, script);
    }
}
