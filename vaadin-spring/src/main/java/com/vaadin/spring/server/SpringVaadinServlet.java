/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.server.DefaultUIProvider;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.UIProvider;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.internal.UIScopeImpl;
import com.vaadin.spring.internal.VaadinSessionScope;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin
 * servlet} that adds a {@link SpringUIProvider} to every new Vaadin session and
 * allows the use of a custom service URL on the bootstrap page.
 * <p>
 * If you need a custom Vaadin servlet, you can either extend this servlet
 * directly, or extend another subclass of {@link VaadinServlet} and just add
 * the UI provider.
 * <p>
 * This servlet also implements a hack to get around the behavior of Spring
 * ServletForwardingController/ServletWrappingController. Those controllers
 * return null as the pathInfo of requests forwarded to the Vaadin servlet, and
 * use the mapping as the servlet path whereas with Vaadin the mapping typically
 * corresponds to a UI, not a virtual servlet. Thus, there is an option to clear
 * the servlet path in requests and compute pathInfo accordingly. This is used
 * by Vaadin Spring Boot to make it easier to use Vaadin and Spring MVC
 * applications together in the same global "namespace".
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Josh Long (josh@joshlong.com)
 */
public class SpringVaadinServlet extends VaadinServlet {

    private static final long serialVersionUID = 5371983676318947478L;

    private String serviceUrlPath = null;

    @Override
    protected void servletInitialized() throws ServletException {
        VaadinServletService service = getService();
        service.addSessionInitListener(new SessionInitListener() {

            private static final long serialVersionUID = -6307820453486668084L;

            @Override
            public void sessionInit(SessionInitEvent sessionInitEvent)
                    throws ServiceException {
                WebApplicationContext webApplicationContext = WebApplicationContextUtils
                        .getWebApplicationContext(getServletContext());

                // remove DefaultUIProvider instances to avoid mapping
                // extraneous UIs if e.g. a servlet is declared as a nested
                // class in a UI class
                VaadinSession session = sessionInitEvent.getSession();
                List<UIProvider> uiProviders = new ArrayList<UIProvider>(
                        session.getUIProviders());
                for (UIProvider provider : uiProviders) {
                    // use canonical names as these may have been loaded with
                    // different classloaders
                    if (DefaultUIProvider.class.getCanonicalName().equals(
                            provider.getClass().getCanonicalName())) {
                        session.removeUIProvider(provider);
                    }
                }

                // add Spring UI provider
                SpringUIProvider uiProvider = new SpringUIProvider(session);
                session.addUIProvider(uiProvider);
            }
        });
        service.addSessionDestroyListener(new SessionDestroyListener() {
            @Override
            public void sessionDestroy(SessionDestroyEvent event) {
                VaadinSession session = event.getSession();

                UIScopeImpl.cleanupSession(session);
                VaadinSessionScope.cleanupSession(session);
            }
        });
    }

    /**
     * Return the path of the service URL (URL for all client-server
     * communication) relative to the context path. A value of null means that
     * the default service path of Vaadin should be used. The path should start
     * with a slash.
     *
     * @return service URL path relative to context path (starting with slash)
     *         or null to use the default
     */
    public String getServiceUrlPath() {
        return serviceUrlPath;
    }

    /**
     * Set the path of the service URL (URL for all client-server communication)
     * to use, relative to the context path. The value null means that the
     * default service URL of Vaadin should be used. The service URL path must
     * be set before servlet service instances are created, i.e. before the
     * servlet is placed into service by the servlet container.
     *
     * @param serviceUrlPath
     *            service URL path relative to the context path (starting with a
     *            slash) or null for default
     */
    public void setServiceUrlPath(String serviceUrlPath) {
        this.serviceUrlPath = serviceUrlPath;
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        // this is needed when using a custom service URL
        SpringVaadinServletService service = new SpringVaadinServletService(
                this, deploymentConfiguration, getServiceUrlPath());
        service.init();
        return service;
    }

    @Override
    protected VaadinServletRequest createVaadinRequest(
            HttpServletRequest request) {
        if (serviceUrlPath != null) {
            return new SpringVaadinServletRequest(request, getService(), true);
        } else {
            return new VaadinServletRequest(request, getService());
        }
    }

    /**
     * Check if this is a request for a static resource and, if it is, return the
     * resource path.
     *
     * @param request http client request
     * @return static file path or null if the request is not for a static resource.
     *
     */
    @Override
    protected String getStaticFilePath(HttpServletRequest request) {
    /*
      Under spring environment, all requests are intercepted with DispatcherServlet, and then mapped to
      SpringVaadinServlet with VaadinServletConfiguration, and a static resource prefix (/VAADIN) is not present nor in
      getPathInfo(), nor in getServletPath() after that. Here request URL is manually decoded to detect the prefix and
      handle static resources properly.

      This method is a copy of a hack from VaadinServlet.java of version 7 to support static resource handling in any case.
      TODO fix static resource request mapping for spring application
    */
        String staticFilePath = super.getStaticFilePath(request);
        if (staticFilePath == null) {

            try {
                String decodedRequestURI = URLDecoder.decode(
                        request.getRequestURI(), StandardCharsets.UTF_8.name());
                if (decodedRequestURI.startsWith("/VAADIN/")) {
                    return decodedRequestURI;
                }

                String decodedContextPath = URLDecoder.decode(
                        request.getContextPath(), StandardCharsets.UTF_8.name());
                if (decodedRequestURI.startsWith(decodedContextPath + "/VAADIN/")) {
                    return decodedRequestURI.substring(decodedContextPath.length());
                }
            } catch (UnsupportedEncodingException exception) {
                // cannot happen since UTF8 is always supported
                throw new RuntimeException(exception);
            }
        }
        return staticFilePath;
    }
}
