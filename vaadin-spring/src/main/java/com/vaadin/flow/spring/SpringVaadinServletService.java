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
package com.vaadin.flow.spring;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

/**
 * Spring application context aware Vaadin servlet service implementation.
 *
 * @author Vaadin Ltd
 */
public class SpringVaadinServletService extends VaadinServletService {

    private final transient ApplicationContext context;

    static final String SPRING_BOOT_WEBPROPERTIES_CLASS = "org.springframework.boot.autoconfigure.web.WebProperties";

    /**
     * Creates an instance connected to the given servlet and using the given
     * configuration with provided application {@code context}.
     *
     * @param servlet
     *            the servlet which receives requests
     * @param deploymentConfiguration
     *            the configuration to use
     * @param context
     *            the Spring application context
     */
    public SpringVaadinServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration,
            ApplicationContext context) {
        super(servlet, deploymentConfiguration);
        this.context = context;
    }

    @Override
    protected Optional<Instantiator> loadInstantiators()
            throws ServiceException {
        Optional<Instantiator> spiInstantiator = super.loadInstantiators();
        List<Instantiator> springInstantiators = context
                .getBeansOfType(Instantiator.class).values().stream()
                .collect(Collectors.toList());
        if (spiInstantiator.isPresent() && !springInstantiators.isEmpty()) {
            throw new ServiceException(
                    "Cannot init VaadinService because there are multiple eligible "
                            + "instantiator implementations: Java SPI registered instantiator "
                            + spiInstantiator.get()
                            + " and Spring instantiator beans: "
                            + springInstantiators);
        }
        if (!spiInstantiator.isPresent() && springInstantiators.isEmpty()) {
            Instantiator defaultInstantiator = new SpringInstantiator(this,
                    context);
            return Optional.of(defaultInstantiator);
        }
        return spiInstantiator.isPresent() ? spiInstantiator
                : springInstantiators.stream().findFirst();
    }

    @Override
    public void init() throws ServiceException {
        super.init();
        Map<String, UIInitListener> uiInitListeners = context
                .getBeansOfType(UIInitListener.class);
        uiInitListeners.values().forEach(this::addUIInitListener);
    }

    // This method should be removed when the deprecated class
    // SpringVaadinSession is removed
    @Override
    protected VaadinSession createVaadinSession(VaadinRequest request) {
        return new SpringVaadinSession(this);
    }

    @Override
    public URL getStaticResource(String path) {
        URL resource = super.getStaticResource(path);
        if (resource == null) {
            resource = getResourceURL(path);
        }
        return resource;
    }

    private URL getResourceURL(String path) {
        if (!isSpringBootConfigured()) {
            return null;
        }
        for (String prefix : context.getBean(
                org.springframework.boot.autoconfigure.web.WebProperties.class)
                .getResources().getStaticLocations()) {
            Resource resource = context.getResource(getFullPath(path, prefix));
            if (resource != null && resource.exists()) {
                try {
                    URI uri = resource.getURI();
                    if (uri.isOpaque() && resource.isFile()) {
                        // Prevents 'URI is not hierarchical' error
                        return resource.getFile().getAbsoluteFile().toURI()
                                .toURL();
                    }
                    return resource.getURL();
                } catch (IOException e) {
                    // NO-OP file was not found.
                }
            }
        }
        return null;
    }

    private String getFullPath(String path, String prefix) {
        if (prefix.endsWith("/") && path.startsWith("/")) {
            return prefix + path.substring(1);
        }
        return prefix + path;
    }

    /**
     * Checks if the spring boot resources class is available without causing
     * ClassNotFound or similar exceptions in plain Spring.
     */
    private boolean isSpringBootConfigured() {
        Class<?> resourcesClass = resolveClass(SPRING_BOOT_WEBPROPERTIES_CLASS);
        if (resourcesClass != null) {
            return context.getBeanNamesForType(resourcesClass).length != 0;
        }
        return false;
    }

    private static Class<?> resolveClass(String clazzName) {
        try {
            return Class.forName(clazzName, false,
                    SpringVaadinServletService.class.getClassLoader());
        } catch (LinkageError | ClassNotFoundException e) {
            return null;
        }
    }

}
