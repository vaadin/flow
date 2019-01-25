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

import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.webcomponent.WebComponentRegistry;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.ThemeDefinition;
import com.vaadin.flow.theme.ThemeUtil;

/**
 * Custom UI for use with WebComponents served from the server.
 */
public class WebComponentUI extends UI {

    public static final String NO_NAVIGATION = "Navigation is not available for WebComponents";

    @Override
    public void doInit(VaadinRequest request, int uiId) {
        super.doInit(request, uiId);
        assignLumoThemeIfAvailable();
    }

    /**
     * Connect a client side web component element with a server side {@link
     * Component} that's added as a virtual child to the UI as the actual
     * relation of the elements is unknown.
     *
     * @param tag
     *         web component tag
     * @param webComponentElementId
     *         client side id of the element
     */
    @ClientCallable
    public void connectWebComponent(String tag, String webComponentElementId) {
        Optional<Class<? extends Component>> webComponent = WebComponentRegistry
                .getInstance(VaadinServlet.getCurrent().getServletContext())
                .getWebComponent(tag);

        if (!webComponent.isPresent()) {
            LoggerFactory.getLogger(WebComponentUI.class)
                    .warn("Received connect request for non existing WebComponent '{}'",
                            tag);
            return;
        }

        Component wcInstance = Instantiator.get(this)
                .getOrCreate(webComponent.get());

        WebComponentWrapper wrapper = new WebComponentWrapper(tag, wcInstance);

        getElement().getStateProvider()
                .appendVirtualChild(getElement().getNode(),
                        wrapper.getElement(), NodeProperties.INJECT_BY_ID,
                        webComponentElementId);
        wrapper.getElement().executeJavaScript("$0.serverConnected()");
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

    private void assignLumoThemeIfAvailable() {
        Optional<ThemeDefinition> lumoOptional = ThemeUtil
                .getLumoThemeDefinition();

        if (lumoOptional.isPresent()) {
            ThemeDefinition lumoThemeDefinition = lumoOptional.get();
            AbstractTheme theme;
            theme = Instantiator.get(this)
                    .getOrCreate(lumoThemeDefinition.getTheme());
            getInternals().setTheme(theme);
        }
    }
}
