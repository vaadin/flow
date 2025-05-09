package com.vaadin.flow.spring.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.internal.hilla.FileRouterRequestUtil;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.router.internal.RouteTarget;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.auth.AccessCheckDecision;
import com.vaadin.flow.server.auth.AccessCheckResult;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.NavigationContext;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

/**
 * Contains utility methods related to request handling.
 */
@Component
public class RequestUtil {

    private static final ThreadLocal<Boolean> ROUTE_PATH_MATCHER_RUNNING = new ThreadLocal<>();

    @Autowired
    private ObjectProvider<NavigationAccessControl> accessControl;

    @Autowired
    private VaadinConfigurationProperties configurationProperties;

    @Autowired(required = false)
    private EndpointRequestUtil endpointRequestUtil;

    @Autowired(required = false)
    private FileRouterRequestUtil fileRouterRequestUtil;

    @Autowired
    private ServletRegistrationBean<SpringServlet> springServletRegistration;

    private WebIconsRequestMatcher webIconsRequestMatcher;

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
        if (endpointRequestUtil != null) {
            return endpointRequestUtil.isEndpointRequest(request);
        }
        return false;
    }

    /**
     * Checks whether the request targets an endpoint that is public, i.e.
     * marked as @{@link AnonymousAllowed}.
     *
     * @param request
     *            the servlet request
     * @return {@code true} if the request is targeting an anonymous endpoint,
     *         {@code false} otherwise
     */
    public boolean isAnonymousEndpoint(HttpServletRequest request) {
        if (endpointRequestUtil != null) {
            return endpointRequestUtil.isAnonymousEndpoint(request);
        }
        return false;
    }

    /**
     * Checks if the request targets a Hilla view that is allowed according to
     * its configuration and the current user.
     *
     * @param request
     *            the HTTP request to check
     * @return {@code true} if the request corresponds to an accessible Hilla
     *         view, {@code false} otherwise
     */
    public boolean isAllowedHillaView(HttpServletRequest request) {
        if (fileRouterRequestUtil != null) {
            return fileRouterRequestUtil.isRouteAllowed(request);
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
        if (ROUTE_PATH_MATCHER_RUNNING.get() == null) {
            ROUTE_PATH_MATCHER_RUNNING.set(Boolean.TRUE);
            try {
                return isAnonymousRouteInternal(request);
            } finally {
                ROUTE_PATH_MATCHER_RUNNING.remove();
            }
        }
        // A route path check is already in progress for the current request
        // this matcher should be considered only once, since for alias check
        // we are interested only in the other matchers
        return false;
    }

    /**
     * Checks whether the request targets a custom PWA icon or Favicon path.
     *
     * @param request
     *            the servlet request
     * @return {@code true} if the request is targeting a custom PWA icon or a
     *         custom favicon path, {@code false} otherwise
     */
    public boolean isCustomWebIcon(HttpServletRequest request) {
        if (webIconsRequestMatcher == null) {
            VaadinServletService vaadinService = springServletRegistration
                    .getServlet().getService();
            if (vaadinService != null) {
                webIconsRequestMatcher = new WebIconsRequestMatcher(
                        vaadinService, configurationProperties.getUrlMapping());
            } else {
                getLogger().debug(
                        "WebIconsRequestMatcher cannot be created because VaadinService is not yet available. "
                                + "This may happen after a hot-reload, and can cause requests for icons to be blocked by Spring Security.");
                return false;
            }
        }
        return webIconsRequestMatcher.matches(request);
    }

    /**
     * Utility to create {@link RequestMatcher}s from ant patterns.
     *
     * @param patterns
     *            and patterns
     * @return an array or {@link RequestMatcher} instances for the given
     *         patterns.
     */
    public static RequestMatcher[] antMatchers(String... patterns) {
        return Stream.of(patterns).map(AntPathRequestMatcher::new)
                .toArray(RequestMatcher[]::new);
    }

    /**
     * Utility to create {@link RequestMatcher}s for a Vaadin routes, using ant
     * patterns and HTTP get method.
     *
     * @param patterns
     *            and patterns
     * @return an array or {@link RequestMatcher} instances for the given
     *         patterns.
     */
    public static RequestMatcher[] routeMatchers(String... patterns) {
        return Stream.of(patterns)
                .map(p -> AntPathRequestMatcher.antMatcher(HttpMethod.GET, p))
                .toArray(RequestMatcher[]::new);
    }

    private boolean isAnonymousRouteInternal(HttpServletRequest request) {
        String vaadinMapping = configurationProperties.getUrlMapping();
        String requestedPath = HandlerHelper
                .getRequestPathInsideContext(request);
        Optional<String> maybePath = HandlerHelper
                .getPathIfInsideServlet(vaadinMapping, requestedPath);
        if (maybePath.isEmpty()) {
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
        if (service == null) {
            // The service has not yet been initialized. We cannot know if this
            // is an anonymous route, so better say it is not.
            return false;
        }
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

        boolean productionMode = service.getDeploymentConfiguration()
                .isProductionMode();
        NavigationAccessControl navigationAccessControl = accessControl
                .getObject();
        if (!navigationAccessControl.isEnabled()) {
            String message = "Navigation Access Control is disabled. Cannot determine if {} refers to a public view, thus access is denied. Please add an explicit request matcher rule for this URL.";
            if (productionMode) {
                getLogger().debug(message, path);
            } else {
                getLogger().info(message, path);
            }
            return false;
        }

        NavigationContext navigationContext = new NavigationContext(router,
                targetView,
                new Location(path,
                        QueryParameters.full(request.getParameterMap())),
                target.getRouteParameters(), null, role -> false, false);

        AccessCheckResult result = navigationAccessControl
                .checkAccess(navigationContext, productionMode);
        boolean isAllowed = result.decision() == AccessCheckDecision.ALLOW;
        if (isAllowed) {
            getLogger().debug("{} refers to a public view", path);
        } else {
            getLogger().debug(
                    "Access to {} denied by Flow navigation access control. {}",
                    path, result.reason());
        }
        return isAllowed;
    }

    String getUrlMapping() {
        return configurationProperties.getUrlMapping();
    }

    /**
     * Prepends to the given {@code path} with the configured url mapping.
     *
     * A {@literal null} path is treated as empty string; the same applies for
     * url mapping.
     *
     * @return the path with prepended url mapping.
     * @see VaadinConfigurationProperties#getUrlMapping()
     */
    public String applyUrlMapping(String path) {
        return applyUrlMapping(configurationProperties.getUrlMapping(), path);
    }

    /**
     * Prepends to the given {@code path} with the servlet path prefix from
     * input url mapping.
     *
     * A {@literal null} path is treated as empty string; the same applies for
     * url mapping.
     *
     * @return the path with prepended url mapping.
     * @see VaadinConfigurationProperties#getUrlMapping()
     */
    static String applyUrlMapping(String urlMapping, String path) {
        if (urlMapping == null) {
            urlMapping = "";
        } else {
            // remove potential / or /* at the end of the mapping
            urlMapping = urlMapping.replaceFirst("/\\*?$", "");
        }
        if (path == null) {
            path = "";
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return urlMapping + "/" + path;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

}
