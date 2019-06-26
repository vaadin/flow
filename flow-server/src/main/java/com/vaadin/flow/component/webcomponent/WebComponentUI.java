/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.webcomponent;

import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;
import com.vaadin.flow.theme.ThemeUtil;

import elemental.json.JsonObject;

/**
 * Custom UI for use with WebComponents served from the server.
 *
 * @author Vaadin Ltd.
 */
public class WebComponentUI extends UI {

    public static final String NO_NAVIGATION = "Navigation is not available for WebComponents";

    @Override
    public void doInit(VaadinRequest request, int uiId) {
        super.doInit(request, uiId);

        assignTheme();

        VaadinSession session = getSession();
        DeploymentConfiguration deploymentConfiguration = session.getService()
                .getDeploymentConfiguration();
        if (deploymentConfiguration.isCompatibilityMode()
                && deploymentConfiguration.useCompiledFrontendResources()) {
            /*
             * This code adds a number of HTML dependencies to the page but in
             * fact there are no such HTML files: they should have been
             * generated during transpilation via maven plugin. To be able to
             * activate transpiled code the embedded application imports the
             * "dependencies" which represent the transpiled files.
             *
             * This code is not needed when in npm mode, since the web
             * components will be contained within index.js
             */
            getConfigurationRegistry().getConfigurations()
            .forEach(config -> getPage()
                    .addHtmlImport(getWebComponentHtmlPath(config)));
        }

        getEventBus().addListener(WebComponentConnectEvent.class,
                this::connectWebComponent);
    }

    /**
     * Event used for sending the activation event for an exported web component
     * from the client to the server.
     */
    @DomEvent("connect-web-component")
    public static class WebComponentConnectEvent extends ComponentEvent<UI> {

        private String tag;
        private String webComponentElementId;
        private JsonObject attributeValues;

        /**
         * Creates a new web component connection event.
         *
         * @param source
         *            the component that was attached
         * @param fromClient
         *            <code>true</code> if the event was originally fired on the
         *            client, <code>false</code> if the event originates from
         *            server-side logic
         * @param tag
         *            the tag of the element to connect
         * @param webComponentElementId
         *            the id of the web component
         * @param attributeValues
         *            initial attribute values as a JsonObject. If present,
         *            these will override the default value designated by the
         *            {@code WebComponentExporter} but only for this instance.
         */
        public WebComponentConnectEvent(UI source, boolean fromClient,
                @EventData("tag") String tag,
                @EventData("id") String webComponentElementId,
                @EventData("attributeValues") JsonObject attributeValues) {
            super(source, true);
            this.tag = tag;
            this.webComponentElementId = webComponentElementId;
            this.attributeValues = attributeValues;
        }

        /**
         * Gets the tag of the element to connect.
         *
         * @return the tag of the element
         */
        public String getTag() {
            return tag;
        }

        /**
         * Gets the id of the web component.
         *
         * @return the id of the web component
         */
        public String getWebComponentElementId() {
            return webComponentElementId;
        }

        /**
         * Gets the initial attribute values.
         *
         * @return the initial attribute values
         */
        public JsonObject getAttributeJson() {
            return attributeValues;
        }
    }

    /**
     * Connect a client side web component element with a server side
     * {@link Component} that's added as a virtual child to the UI as the actual
     * relation of the elements is unknown.
     *
     * @param event
     *            web component tag
     * @param webComponentElementId
     *            client side id of the element
     */
    private void connectWebComponent(WebComponentConnectEvent event) {
        Optional<WebComponentConfiguration<? extends Component>> webComponentConfiguration = getConfigurationRegistry()
                .getConfiguration(event.getTag());

        if (!webComponentConfiguration.isPresent()) {
            LoggerFactory.getLogger(WebComponentUI.class).warn(
                    "Received connect request for non existing WebComponent '{}'",
                    event.getTag());
            return;
        }

        Element rootElement = new Element(event.getTag());
        WebComponentBinding binding = webComponentConfiguration.get()
                .createWebComponentBinding(Instantiator.get(this), rootElement,
                        event.getAttributeJson());
        WebComponentWrapper wrapper = new WebComponentWrapper(rootElement,
                binding);

        getElement().getStateProvider().appendVirtualChild(
                getElement().getNode(), wrapper.getElement(),
                NodeProperties.INJECT_BY_ID, event.getWebComponentElementId());
        wrapper.getElement().executeJs("$0.serverConnected()");
    }

    @Override
    public Router getRouter() {
        return null;
    }

    @Override
    public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path) {
        return Optional.empty();
    }

    @Override
    public void navigate(String location) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public void navigate(Class<? extends Component> navigationTarget) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public <T, C extends Component & HasUrlParameter<T>> void navigate(
            Class<? extends C> navigationTarget, T parameter) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public void navigate(String location, QueryParameters queryParameters) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    private void assignTheme() {
        WebComponentConfigurationRegistry registry = getConfigurationRegistry();
        Optional<Theme> theme = registry
                .getEmbeddedApplicationAnnotation(Theme.class);
        if (theme.isPresent()) {
            getInternals().setTheme(theme.get().value());
            assignVariant(registry, theme.get());
        } else {
            ThemeUtil.getLumoThemeDefinition().map(ThemeDefinition::getTheme)
                    .ifPresent(getInternals()::setTheme);
        }
    }

    private void assignVariant(WebComponentConfigurationRegistry registry,
            Theme theme) {
        AbstractTheme themeInstance = Instantiator.get(this)
                .getOrCreate(theme.value());
        ThemeDefinition definition = new ThemeDefinition(theme);
        Map<String, String> attributes = themeInstance
                .getHtmlAttributes(definition.getVariant());

        registry.getConfigurations()
                .forEach(config -> addAttributes(config.getTag(), attributes));
    }

    private void addAttributes(String tag, Map<String, String> attributes) {
        final StringBuilder builder = new StringBuilder();
        builder.append("var elements = document.querySelectorAll('").append(tag)
                .append("');")
                .append("for (let i = 0; i < elements.length; i++) {");
        attributes.forEach((attribute, value) -> builder
                .append("elements[i].setAttribute('").append(attribute)
                .append("', '").append(value).append("');"));
        builder.append("}");
        getPage().executeJs(builder.toString());
    }

    private String getWebComponentHtmlPath(
            WebComponentConfiguration<? extends Component> config) {
        DeploymentConfiguration deploymentConfiguration = getSession()
                .getService().getDeploymentConfiguration();
        String path = deploymentConfiguration.getCompiledWebComponentsPath();

        return path + '/' + config.getTag() + ".html";
    }

    private WebComponentConfigurationRegistry getConfigurationRegistry() {
        return WebComponentConfigurationRegistry
                .getInstance(getSession().getService().getContext());
    }
}
