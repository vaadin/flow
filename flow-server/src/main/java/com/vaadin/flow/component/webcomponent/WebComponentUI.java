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
package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Custom UI for use with WebComponents served from the server.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class WebComponentUI extends UI {
    public static final String NO_NAVIGATION = "Navigation is not available for WebComponents";

    @Override
    public void doInit(VaadinRequest request, int uiId, String appId) {
        super.doInit(request, uiId, appId);

        assignThemeVariant();
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
        private String userAssignedId;
        private String webComponentElementId;
        private JsonNode attributeValues;

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
         *            the id of the embedded web component
         * @param userAssignedId
         *            the id user might have set on the embedding web component
         *            element
         * @param attributeValues
         *            initial attribute values as a JsonObject. If present,
         *            these will override the default value designated by the
         *            {@code WebComponentExporter} but only for this instance.
         */
        public WebComponentConnectEvent(UI source, boolean fromClient,
                @EventData("tag") String tag,
                @EventData("id") String webComponentElementId,
                @EventData("userAssignedId") String userAssignedId,
                @EventData("attributeValues") JsonNode attributeValues) {
            super(source, true);
            this.tag = tag;
            this.userAssignedId = userAssignedId;
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
         * Gets the user-assigned id of the web component.
         *
         * @return user-assigned id
         */
        public String getWebComponentUserAssignedId() {
            return userAssignedId;
        }

        /**
         * Gets the initial attribute values.
         *
         * @return the initial attribute values
         */
        public JsonNode getAttributeJson() {
            return attributeValues;
        }
    }

    /**
     * Connects a client side web component element with a server side
     * {@link Component} that's added as a virtual child to this UI.
     *
     * @param event
     *            an event describing the tag, element id and initial property
     *            values of the web component
     */
    private void connectWebComponent(WebComponentConnectEvent event) {
        final Optional<WebComponentConfiguration<? extends Component>> webComponentConfiguration = getConfigurationRegistry()
                .getConfiguration(event.getTag());

        if (!webComponentConfiguration.isPresent()) {
            LoggerFactory.getLogger(WebComponentUI.class).warn(
                    "Received connect request for non existing WebComponent '{}'",
                    event.getTag());
            return;
        }

        final boolean shouldBePreserved = isConfigurationAnnotated(
                webComponentConfiguration.get(), PreserveOnRefresh.class);

        if (!shouldBePreserved) {
            attachCreatedWebComponent(webComponentConfiguration.get(), event);
        } else if (getInternals().getExtendedClientDetails() != null) {
            attachCachedOrCreatedWebComponent(webComponentConfiguration.get(),
                    event, getComponentHash(event,
                            getInternals().getExtendedClientDetails()));
        } else {
            getPage().retrieveExtendedClientDetails(extendedClientDetails -> {
                attachCachedOrCreatedWebComponent(
                        webComponentConfiguration.get(), event,
                        getComponentHash(event, extendedClientDetails));
            });
        }
    }

    private void attachCreatedWebComponent(
            WebComponentConfiguration<? extends Component> configuration,
            WebComponentConnectEvent event) {
        Element elementToAttach = createWebComponentWrapper(configuration,
                event);
        attachComponentToUI(elementToAttach, event.webComponentElementId);
    }

    private void attachCachedOrCreatedWebComponent(
            WebComponentConfiguration<? extends Component> configuration,
            WebComponentConnectEvent event, final String hash) {
        Element elementToAttach = null;

        Optional<Element> old = getRegistry().get(hash);
        if (old.isPresent()) {
            elementToAttach = old.get().removeFromTree(false);
        }

        // did not have an element in the cache, create a new one
        if (elementToAttach == null) {
            elementToAttach = createWebComponentWrapper(configuration, event);
        }

        attachComponentToUI(elementToAttach, event.webComponentElementId);
        getRegistry().put(hash, elementToAttach);
    }

    private Element createWebComponentWrapper(
            WebComponentConfiguration<? extends Component> configuration,
            WebComponentConnectEvent event) {

        Element rootElement = new Element(event.getTag());
        WebComponentBinding binding = configuration.createWebComponentBinding(
                Instantiator.get(this), rootElement, event.getAttributeJson());
        WebComponentWrapper wrapper = new WebComponentWrapper(rootElement,
                binding, getConfigurationRegistry().getShadowDomElements());

        return wrapper.getElement();
    }

    private void attachComponentToUI(Element child, String elementId) {
        getElement().getStateProvider().appendVirtualChild(
                getElement().getNode(), child, NodeProperties.INJECT_BY_ID,
                elementId);
        child.executeJs("$0.serverConnected()");
    }

    private boolean isConfigurationAnnotated(
            WebComponentConfiguration<? extends Component> configuration,
            Class<? extends Annotation> annotationClass) {
        return AnnotationReader.getAnnotationFor(
                configuration.getExporterClass(), annotationClass).isPresent();
    }

    private String getComponentHash(WebComponentConnectEvent event,
            ExtendedClientDetails details) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(details);
        String id = event.getWebComponentUserAssignedId();
        if (id == null || id.isEmpty()) {
            // if no user-assigned id is available, fallback to generated id
            // generated id is unable to uniquely identify components between
            // locations, so relying on it can cause state leak
            id = event.getWebComponentElementId();
        }
        return String.format("%s:%s:%s", details.getWindowName(),
                event.getTag(), id);
    }

    private SessionEmbeddedComponentRegistry getRegistry() {
        return SessionEmbeddedComponentRegistry
                .getSessionRegistry(VaadinSession.getCurrent());
    }

    @Override
    public boolean isNavigationSupported() {
        return false;
    }

    @Override
    public void navigate(String location) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public <T extends Component> Optional<T> navigate(
            Class<T> navigationTarget) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public <T, C extends Component & HasUrlParameter<T>> Optional<C> navigate(
            Class<? extends C> navigationTarget, T parameter) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public void navigate(String location, QueryParameters queryParameters) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    private void assignThemeVariant() {
        WebComponentConfigurationRegistry registry = getConfigurationRegistry();
        Optional<Theme> theme = registry
                .getEmbeddedApplicationAnnotation(Theme.class);
        if (!theme.isPresent()
                || theme.get().themeClass().equals(AbstractTheme.class)) {
            return;
        }
        AbstractTheme themeInstance = Instantiator.get(this)
                .getOrCreate(theme.get().themeClass());
        ThemeDefinition definition = new ThemeDefinition(theme.get());
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

    private WebComponentConfigurationRegistry getConfigurationRegistry() {
        return WebComponentConfigurationRegistry
                .getInstance(getSession().getService().getContext());
    }

    private static class SessionEmbeddedComponentRegistry
            implements Serializable {
        private final VaadinSession session;
        private ConcurrentHashMap<String, Element> cache = new ConcurrentHashMap<>();

        SessionEmbeddedComponentRegistry(VaadinSession session) {
            this.session = session;
        }

        static SessionEmbeddedComponentRegistry getSessionRegistry(
                VaadinSession session) {
            Objects.requireNonNull(session,
                    "Null session is not supported for session route registry");
            SessionEmbeddedComponentRegistry registry = session
                    .getAttribute(SessionEmbeddedComponentRegistry.class);
            if (registry == null) {
                registry = new SessionEmbeddedComponentRegistry(session);
                session.setAttribute(SessionEmbeddedComponentRegistry.class,
                        registry);
            }
            if (!registry.session.equals(session)) {
                throw new IllegalStateException(String.format(
                        "Session has an attribute for %s registered for "
                                + "another session!",
                        SessionEmbeddedComponentRegistry.class
                                .getSimpleName()));
            }
            return registry;
        }

        /**
         * Placed {@code element} uniquely identified by the supplied {@code
         * identifier} into the registry. If an element with the {@code
         * identifier} exists in the registry, the old value will be kept and
         * new one will be ignored.
         * <p>
         * This is an atomic operation.
         *
         * @param identifier
         *            Unique identifier for the {@code element}
         * @param element
         *            {@link com.vaadin.flow.dom.Element} to store
         */
        void put(String identifier, Element element) {
            Objects.requireNonNull(identifier);
            Objects.requireNonNull(element);
            // atomic placement
            cache.computeIfAbsent(identifier, id -> element);
        }

        /**
         * Retrieves the {@link com.vaadin.flow.dom.Element} stored in the
         * registry, identified by {@code identifier}.
         *
         * @param identifier
         *            Unique identifier for the {@link Element}
         * @return an {@link Element}, or {@code null} if nothing is stored for
         *         the given {@code identifier}
         */
        Optional<Element> get(String identifier) {
            Objects.requireNonNull(identifier);
            return Optional.ofNullable(cache.get(identifier));
        }
    }
}
