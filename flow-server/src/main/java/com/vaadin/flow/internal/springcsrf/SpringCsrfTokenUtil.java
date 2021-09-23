package com.vaadin.flow.internal.springcsrf;

import java.util.Optional;
import org.jsoup.nodes.Element;

import javax.servlet.ServletRequest;

import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.VaadinRequest;

import elemental.json.JsonObject;

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
    private static final String META_TAG = "meta";

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
            JsonObject springCsrfTokenJson = JsonUtils
                    .beanToJson(springCsrfToken);
            if (springCsrfTokenJson != null
                    && springCsrfTokenJson.hasKey(SPRING_CSRF_TOKEN_PROPERTY)
                    && springCsrfTokenJson
                            .hasKey(SPRING_CSRF_HEADER_PROPERTY)) {
                String token = springCsrfTokenJson
                        .getString(SPRING_CSRF_TOKEN_PROPERTY);
                String headerName = springCsrfTokenJson
                        .getString(SPRING_CSRF_HEADER_PROPERTY);
                String parameterName = springCsrfTokenJson
                        .getString(SPRING_CSRF_PARAMETER_PROPERTY);

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
}
