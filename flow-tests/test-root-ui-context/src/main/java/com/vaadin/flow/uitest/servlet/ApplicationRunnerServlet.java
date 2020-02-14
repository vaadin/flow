/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.Attributes;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.SystemMessagesProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.uitest.servlet.CustomDeploymentConfiguration.Conf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(asyncSupported = true, urlPatterns = { "/*" })
public class ApplicationRunnerServlet extends VaadinServlet {

    public static String CUSTOM_SYSTEM_MESSAGES_PROPERTY = "custom-"
            + SystemMessages.class.getName();

    /**
     * The name of the application class currently used. Only valid within one
     * request.
     */
    private Set<String> defaultPackages = new LinkedHashSet<>();

    private transient final ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String initParameter = servletConfig
                .getInitParameter("defaultPackages");
        if (initParameter != null) {
            Collections.addAll(defaultPackages, initParameter.split(","));
        }
        URL url = getService().getClassLoader().getResource(".");
        if ("file".equals(url.getProtocol())) {
            try {
                new URI(url.getPath()).getPath();
            } catch (URISyntaxException e) {
                getLogger().debug("Failed to decode url", e);
            }
            try {
                addDirectories(new File(url.getPath()), defaultPackages);
            } catch (IOException exception) {
                throw new RuntimeException(
                        "Unable to scan classpath to find packages", exception);
            }

        }
    }

    private void addDirectories(File parent, Set<String> packages)
            throws IOException {
        Path root = parent.toPath();
        Files.walkFileTree(parent.toPath(), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                File file = dir.toFile();
                if (file.isDirectory()) {
                    Path relative = root.relativize(file.toPath());
                    packages.add(relative.toString().replace(File.separatorChar,
                            '.'));
                }
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        this.request.set(request);
        try {
            super.service(request, response);
        } finally {
            this.request.set(null);
        }
    }

    private String getApplicationRunnerApplicationClassName(
            HttpServletRequest request) {
        try {
            return getClassToRun().getName();
        } catch (ClassNotFoundException e) {
            return getApplicationRunnerURIs(request);
        }
    }

    private final class ProxyDeploymentConfiguration
            implements InvocationHandler, Serializable {
        private final DeploymentConfiguration originalConfiguration;

        private ProxyDeploymentConfiguration(
                DeploymentConfiguration originalConfiguration) {
            this.originalConfiguration = originalConfiguration;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (method.getDeclaringClass() == DeploymentConfiguration.class) {
                // Find the configuration instance to delegate to
                DeploymentConfiguration configuration = findDeploymentConfiguration(
                        originalConfiguration);

                return method.invoke(configuration, args);
            } else {
                return method.invoke(proxy, args);
            }
        }
    }

    /**
     * Parses application runner URIs.
     *
     * If request URL is e.g.
     * http://localhost:8080/vaadin/run/com.vaadin.demo.Calc then
     * <ul>
     * <li>context=vaadin</li>
     * <li>Runner servlet=run</li>
     * <li>Vaadin application=com.vaadin.demo.Calc</li>
     * </ul>
     *
     * @param request
     * @return string array containing widgetset URI, application URI and
     *         context, runner, application classname
     */
    private static String getApplicationRunnerURIs(HttpServletRequest request) {
        final String[] urlParts = request.getRequestURI().toString()
                .split("\\/");
        String contextPath = request.getContextPath();
        if (urlParts[1].equals(contextPath.replaceAll("\\/", ""))) {
            // class name comes after web context and runner application
            if (urlParts.length == 3) {
                throw new IllegalArgumentException("No application specified");
            }
            return urlParts[3];

        } else {
            // no context
            if (urlParts.length == 2) {
                throw new IllegalArgumentException("No application specified");
            }
            return urlParts[2];
        }
    }

    private Class<?> getClassToRun() throws ClassNotFoundException {
        Class<?> appClass = null;

        String baseName = getApplicationRunnerURIs(request.get());
        try {
            appClass = getClass().getClassLoader().loadClass(baseName);
            return appClass;
        } catch (Exception e) {
            //
            for (String pkg : defaultPackages) {
                try {
                    appClass = getClass().getClassLoader()
                            .loadClass(pkg + "." + baseName);
                } catch (ClassNotFoundException ee) {
                    // Ignore as this is expected for many packages
                } catch (Exception e2) {
                    // TODO: handle exception
                    getLogger().debug("Failed to find application class {}. {}",
                            pkg, baseName, e2);
                }
                if (appClass != null) {
                    return appClass;
                }
            }

        }

        throw new ClassNotFoundException(baseName);
    }

    private Logger getLogger() {
        return LoggerFactory
                .getLogger(ApplicationRunnerServlet.class.getName());
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        // Get the original configuration from the super class
        final DeploymentConfiguration originalConfiguration = new DefaultDeploymentConfiguration(
                getClass(), initParameters) {
            @Override
            public String getUIClassName() {
                return getApplicationRunnerApplicationClassName(request.get());
            }
        };

        // And then create a proxy instance that delegates to the original
        // configuration or a customized version
        return (DeploymentConfiguration) Proxy.newProxyInstance(
                DeploymentConfiguration.class.getClassLoader(),
                new Class[] { DeploymentConfiguration.class },
                new ProxyDeploymentConfiguration(originalConfiguration));
    }

    @SuppressWarnings("serial")
    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        // service doesn't use router actually. UI class is responsible to show
        // and update the content by itself with only root route available
        VaadinServletService service = new VaadinServletService(this,
                deploymentConfiguration) {
            @Override
            public Router getRouter() {
                Router router = new Router(getRouteRegistry()) {
                    @Override
                    public int navigate(UI ui, Location location,
                            NavigationTrigger trigger) {
                        return HttpServletResponse.SC_OK;
                    }
                };
                return router;
            }
        };
        service.init();

        final SystemMessagesProvider provider = service
                .getSystemMessagesProvider();
        service.setSystemMessagesProvider(
                (SystemMessagesProvider) systemMessagesInfo -> {
                    if (systemMessagesInfo.getRequest() == null) {
                        return provider.getSystemMessages(systemMessagesInfo);
                    }
                    Object messages = systemMessagesInfo.getRequest()
                            .getAttribute(CUSTOM_SYSTEM_MESSAGES_PROPERTY);
                    if (messages instanceof SystemMessages) {
                        return (SystemMessages) messages;
                    }
                    return provider.getSystemMessages(systemMessagesInfo);
                });
        return service;
    }

    private DeploymentConfiguration findDeploymentConfiguration(
            DeploymentConfiguration originalConfiguration) throws Exception {
        // First level of cache
        DeploymentConfiguration configuration = CurrentInstance
                .get(DeploymentConfiguration.class);

        if (configuration == null) {
            // Not in cache, try to find a VaadinSession to get it from
            VaadinSession session = VaadinSession.getCurrent();

            if (session == null) {
                /*
                 * There's no current session, request or response when serving
                 * static resources, but there's still the current request
                 * maintained by ApplicationRunnerServlet, and there's most
                 * likely also a HttpSession containing a VaadinSession for that
                 * request.
                 */

                HttpServletRequest currentRequest = VaadinServletService
                        .getCurrentServletRequest();
                if (currentRequest != null) {
                    HttpSession httpSession = currentRequest.getSession(false);
                    if (httpSession != null) {
                        Map<Class<?>, CurrentInstance> oldCurrent = CurrentInstance
                                .setCurrent((VaadinSession) null);
                        try {
                            VaadinServletService service = (VaadinServletService) VaadinService
                                    .getCurrent();
                            session = service.findVaadinSession(
                                    new VaadinServletRequest(currentRequest,
                                            service));
                        } finally {
                            /*
                             * Clear some state set by findVaadinSession to
                             * avoid accidentally depending on it when coding on
                             * e.g. static request handling.
                             */
                            CurrentInstance.restoreInstances(oldCurrent);
                            currentRequest.removeAttribute(
                                    VaadinSession.class.getName());
                        }
                    }
                }
            }

            if (session != null) {
                String name = ApplicationRunnerServlet.class.getName()
                        + ".deploymentConfiguration";
                try {
                    session.getLockInstance().lock();

                    /*
                     * Read attribute using reflection to bypass
                     * VaadinSesison.getAttribute which would cause an infinite
                     * loop when checking the production mode setting for
                     * determining whether to check that the session is locked.
                     */
                    Field attributesField = VaadinSession.class
                            .getDeclaredField("attributes");
                    attributesField.setAccessible(true);
                    Attributes sessionAttributes = (Attributes) attributesField
                            .get(session);

                    configuration = (DeploymentConfiguration) sessionAttributes
                            .getAttribute(name);

                    if (configuration == null) {
                        ApplicationRunnerServlet servlet = (ApplicationRunnerServlet) VaadinServlet
                                .getCurrent();
                        Class<?> classToRun;
                        try {
                            classToRun = servlet.getClassToRun();
                        } catch (ClassNotFoundException e) {
                            /*
                             * This happens e.g. if the UI class defined in the
                             * URL is not found or if this servlet just serves
                             * static resources while there's some other servlet
                             * that serves the UI (e.g. when using /run-push/).
                             */
                            return originalConfiguration;
                        }

                        CustomDeploymentConfiguration customDeploymentConfiguration = classToRun
                                .getAnnotation(
                                        CustomDeploymentConfiguration.class);
                        if (customDeploymentConfiguration != null) {
                            Properties initParameters = new Properties(
                                    originalConfiguration.getInitParameters());

                            for (Conf entry : customDeploymentConfiguration
                                    .value()) {
                                initParameters.put(entry.name(), entry.value());
                            }
                            initParameters.put(VaadinSession.UI_PARAMETER,
                                    getApplicationRunnerApplicationClassName(
                                            request.get()));
                            configuration = new DefaultDeploymentConfiguration(
                                    servlet.getClass(), initParameters);
                        } else {
                            configuration = originalConfiguration;
                        }

                        sessionAttributes.setAttribute(name, configuration);
                    }
                } finally {
                    session.getLockInstance().unlock();
                }

                CurrentInstance.set(DeploymentConfiguration.class,
                        configuration);

            } else {
                configuration = originalConfiguration;
            }
        }
        return configuration;
    }
}
