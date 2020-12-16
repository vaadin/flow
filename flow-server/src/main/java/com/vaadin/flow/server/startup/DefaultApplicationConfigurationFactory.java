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
package com.vaadin.flow.server.startup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.AbstractPropertyConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.frontend.FrontendUtils.TOKEN_FILE;

/**
 * Default implementation of {@link ApplicationConfigurationFactory}.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public class DefaultApplicationConfigurationFactory
        extends AbstractConfigurationFactory
        implements ApplicationConfigurationFactory {

    protected static class ApplicationConfigurationImpl extends
            AbstractPropertyConfiguration implements ApplicationConfiguration {

        private final VaadinContext context;

        protected ApplicationConfigurationImpl(VaadinContext context,
                Map<String, String> properties) {
            super(properties);
            this.context = context;
        }

        @Override
        public boolean isProductionMode() {
            return getBooleanProperty(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        }

        @Override
        public Enumeration<String> getPropertyNames() {
            return Collections.enumeration(getProperties().keySet());
        }

        @Override
        public VaadinContext getContext() {
            return context;
        }

    }

    @Override
    public ApplicationConfiguration create(VaadinContext context) {
        Map<String, String> props = new HashMap<>();
        for (final Enumeration<String> e = context.getContextParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            props.put(name, context.getContextParameter(name));
        }
        try {
            JsonObject buildInfo = JsonUtil
                    .parse(getTokenFileFromClassloader(context));

            props.putAll(getConfigParametersUsingTokenData(buildInfo));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return new ApplicationConfigurationImpl(context, props);
    }

    /**
     * Gets token file from the classpath using the provided {@code context}.
     * <p>
     * The {@code contextClass} may be a class which is defined in the Web
     * Application module/bundle and in this case it may be used to get Web
     * Application resources. Also a {@link VaadinContext} {@code context}
     * instance may be used to get a context of the Web Application (since the
     * {@code contextClass} may be a class not from Web Application module). In
     * WAR case it doesn't matter which class is used to get the resources (Web
     * Application classes or e.g. "flow-server" classes) since they are loaded
     * by the same {@link ClassLoader}. But in OSGi "flow-server" module classes
     * can't be used to get Web Application resources since they are in
     * different bundles.
     * 
     * @param context
     *            a VaadinContext which may provide information how to get token
     *            file for the web application
     * @return the token file content
     * @throws IOException
     *             if I/O fails during access to the token file
     */
    protected String getTokenFileFromClassloader(VaadinContext context)
            throws IOException {
        String tokenResource = VAADIN_SERVLET_RESOURCES + TOKEN_FILE;

        Lookup lookup = context.getAttribute(Lookup.class);
        ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);

        List<URL> resources = resourceProvider
                .getApplicationResources(tokenResource);

        // Accept resource that doesn't contain
        // 'jar!/META-INF/Vaadin/config/flow-build-info.json'
        URL resource = resources.stream()
                .filter(url -> !url.getPath().endsWith("jar!/" + tokenResource))
                .findFirst().orElse(null);
        if (resource == null && !resources.isEmpty()) {
            return getPossibleJarResource(context, resources);
        }
        return resource == null ? null
                : FrontendUtils.streamToString(resource.openStream());

    }

    /**
     * Check if the webpack.generated.js resources is inside 2 jars
     * (flow-server.jar and application.jar) if this is the case then we can
     * accept a build info file from inside jar with a single jar in the path.
     * <p>
     * Else we will accept any flow-build-info and log a warning that it may not
     * be the correct file, but it's the best we could find.
     */
    private String getPossibleJarResource(VaadinContext context,
            List<URL> resources) throws IOException {
        Objects.requireNonNull(resources);

        Lookup lookup = context.getAttribute(Lookup.class);
        ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);

        assert !resources
                .isEmpty() : "Possible jar resource requires resources to be available.";

        URL webpackGenerated = resourceProvider
                .getApplicationResource(FrontendUtils.WEBPACK_GENERATED);

        // If jar!/ exists 2 times for webpack.generated.json then we are
        // running from a jar
        if (webpackGenerated != null
                && countInstances(webpackGenerated.getPath(), "jar!/") >= 2) {
            for (URL resource : resources) {
                // As we now know that we are running from a jar we can accept a
                // build info with a single jar in the path
                if (countInstances(resource.getPath(), "jar!/") == 1) {
                    return FrontendUtils.streamToString(resource.openStream());
                }
            }
        }
        URL firstResource = resources.get(0);
        if (resources.size() > 1) {
            String warningMessage = String.format(
                    "Unable to fully determine correct flow-build-info.%n"
                            + "Accepting file '%s' first match of '%s' possible.%n"
                            + "Please verify flow-build-info file content.",
                    firstResource.getPath(), resources.size());
            getLogger().warn(warningMessage);
        } else {
            String debugMessage = String.format(
                    "Unable to fully determine correct flow-build-info.%n"
                            + "Accepting file '%s'",
                    firstResource.getPath());
            getLogger().debug(debugMessage);
        }
        return FrontendUtils.streamToString(firstResource.openStream());
    }

    private int countInstances(String input, String value) {
        return input.split(value, -1).length - 1;
    }

    private Logger getLogger() {
        return LoggerFactory
                .getLogger(DefaultApplicationConfigurationFactory.class);
    }
}
