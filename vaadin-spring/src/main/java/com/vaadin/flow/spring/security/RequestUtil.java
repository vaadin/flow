package com.vaadin.flow.spring.security;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.router.internal.RouteTarget;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.EndpointUtil;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
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
    private AccessAnnotationChecker accessAnnotationChecker;

    @Autowired
    private VaadinConfigurationProperties configurationProperties;

    private Object endpointUtil;

    @Autowired
    private ServletRegistrationBean<SpringServlet> springServletRegistration;

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
     * Note that bootstrap requests for any route or static resource requests
     * are not internal, neither are resource requests for the JS bundle.
     *
     * @param request
     *            the servlet request
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
     * @param request
     *            the servlet request
     * @return {@code true} if the request is targeting an enpoint,
     *         {@code false} otherwise
     */
    public boolean isEndpointRequest(HttpServletRequest request) {
        if (endpointUtil != null) {
            return ((EndpointUtil) endpointUtil).isEndpointRequest(request);
        }
        return false;
    }

    /**
     * Checks whether the request targets an endpoint that is public, i.e.
     * marked as @{@link AnonymousAllowed}.
     *
     * @param request
     *            the servlet request
     * @return {@code true} if the request is targeting an anonymous enpoint,
     *         {@code false} otherwise
     */
    public boolean isAnonymousEndpoint(HttpServletRequest request) {
        if (endpointUtil != null) {
            return ((EndpointUtil) endpointUtil).isAnonymousEndpoint(request);
        }
        return false;
    }

    /**
     * Checks whether the request targets a Flow route that is public, i.e.
     * marked as @{@link AnonymousAllowed}.
     *
     * @param request
     *            the servlet request
     * @return {@code true} if the request is targeting an anonymous route,
     *         {@code false} otherwise
     */
    public boolean isAnonymousRoute(HttpServletRequest request) {
        String vaadinMapping = configurationProperties.getUrlMapping();
        String requestedPath = getRequestPathInsideContext(request);
        Optional<String> maybePath = HandlerHelper
                .getPathIfInsideServlet(vaadinMapping, requestedPath);
        if (!maybePath.isPresent()) {
            return false;
        }
        String path = maybePath.get();
        if (path.startsWith("/")) {
            // Requested path includes a beginning "/" but route mapping is done
            // without one
            path = path.substring(1);
        }

        SpringServlet servlet = springServletRegistration.getServlet();
        VaadinService service = servlet.getService();
        Router router = service.getRouter();
        RouteRegistry routeRegistry = router.getRegistry();

        NavigationRouteTarget target = routeRegistry
                .getNavigationRouteTarget(path);
        if (target == null) {
            return false;
        }
        RouteTarget routeTarget = target.getRouteTarget();
        if (routeTarget == null) {
            return false;
        }
        Class<? extends com.vaadin.flow.component.Component> targetView = routeTarget
                .getTarget();
        if (targetView == null) {
            return false;
        }

        // Check if a not authenticated user can access the view
        boolean result = accessAnnotationChecker.hasAccess(targetView, null,
                role -> false);
        if (result) {
            getLogger().debug(path + " refers to a public view");
        }
        return result;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    private static String getRequestPathInsideContext(
            HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        String url = "";
        if (servletPath != null) {
            url += servletPath;
        }
        if (pathInfo != null) {
            url += pathInfo;
        }
        return url;
    }

}
