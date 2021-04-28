package com.vaadin.flow.spring.security;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.connect.EndpointUtil;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Contains utility methods related to request handling.
 */
@Component
public class RequestUtil {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private VaadinConfigurationProperties configurationProperties;

    private Object endpointUtil;

    @PostConstruct
    public void init() {
        try {
            endpointUtil = applicationContext.getBean(EndpointUtil.class);
        } catch (NoClassDefFoundError e) {
            // Presumable Fusion is not on the classpath
        } catch (Exception e) {
            // Presumable Fusion is not on the classpath
        }

    }

    /**
     * Checks whether the request is an internal request.
     *
     * An internal request is one that is needed for all Vaadin applications to
     * function, e.g. UIDL or init requests.
     *
     * Note that bootstrap requests for any route or static resource requests are
     * not internal, neither are resource requests for the JS bundle.
     *
     * @param request the servlet request
     * @return {@code true} if the request is Vaadin internal, {@code false}
     *         otherwise
     */
    public boolean isFrameworkInternalRequest(HttpServletRequest request) {
        String vaadinMapping = configurationProperties.getUrlMapping();
        return HandlerHelper.isFrameworkInternalRequest(vaadinMapping, request);
    }

    /**
     * Checks whether the request targets an endpoint.
     *
     * @param request the servlet request
     * @return {@code true} if the request is targeting an enpoint, {@code false}
     *         otherwise
     */
    public boolean isEndpointRequest(HttpServletRequest request) {
        if (endpointUtil != null) {
            return ((EndpointUtil) endpointUtil).isEndpointRequest(request);
        }
        return false;
    }

    /**
     * Checks whether the request targets an endpoint that is public, i.e. marked
     * as @{@link AnonymousAllowed}.
     *
     * @param request the servlet request
     * @return {@code true} if the request is targeting an anonymous enpoint,
     *         {@code false} otherwise
     */
    public boolean isAnonymousEndpoint(HttpServletRequest request) {
        if (endpointUtil != null) {
            return ((EndpointUtil) endpointUtil).isAnonymousEndpoint(request);
        }
        return false;
    }
}
