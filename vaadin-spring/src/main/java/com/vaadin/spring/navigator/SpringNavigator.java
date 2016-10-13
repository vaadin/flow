/*
 * Copyright 2015-2016 The original authors
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
package com.vaadin.spring.navigator;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.NavigationStateManager;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;

/**
 * A Navigator that automatically uses {@link SpringViewProvider} and allows
 * late initialization.
 *
 * @author Vaadin Ltd
 */
@UIScope
public class SpringNavigator extends Navigator {

    @Autowired
    private SpringViewProvider viewProvider;

    /**
     * Initializes an injected navigator and registers
     * {@link SpringViewProvider} for it.
     * <p>
     * The default navigation state manager (based on URI fragments) is used.
     * <p>
     * Navigation is automatically initiated after {@code UI.init()} if a
     * navigator was created. If at a later point changes are made to the
     * navigator, {@code navigator.navigateTo(navigator.getState())} may need to
     * be explicitly called to ensure the current view matches the navigation
     * state.
     *
     * @param ui
     *            The UI to which this Navigator is attached.
     * @param container
     *            The component container used to display the views handled by
     *            this navigator
     */
    public void init(UI ui, ComponentContainer container) {
        init(ui, new ComponentContainerViewDisplay(container));
    }

    /**
     * Initializes an injected navigator and registers
     * {@link SpringViewProvider} for it.
     * <p>
     * The default navigation state manager (based on URI fragments) is used.
     * <p>
     * Navigation is automatically initiated after {@code UI.init()} if a
     * navigator was created. If at a later point changes are made to the
     * navigator, {@code navigator.navigateTo(navigator.getState())} may need to
     * be explicitly called to ensure the current view matches the navigation
     * state.
     *
     * @param ui
     *            The UI to which this Navigator is attached.
     * @param container
     *            The single component container used to display the views
     *            handled by this navigator
     */
    public void init(UI ui, SingleComponentContainer container) {
        init(ui, new SingleComponentContainerViewDisplay(container));
    }

    /**
     * Initializes an injected navigator and registers
     * {@link SpringViewProvider} for it.
     * <p>
     * The default navigation state manager (based on URI fragments) is used.
     * <p>
     * Navigation is automatically initiated after {@code UI.init()} if a
     * navigator was created. If at a later point changes are made to the
     * navigator, {@code navigator.navigateTo(navigator.getState())} may need to
     * be explicitly called to ensure the current view matches the navigation
     * state.
     *
     * @param ui
     *            The UI to which this Navigator is attached.
     * @param display
     *            The ViewDisplay used to display the views handled by this
     *            navigator
     */
    public void init(UI ui, ViewDisplay display) {
        init(ui, new UriFragmentManager(ui.getPage()), display);
    }

    /**
     * {@inheritDoc}
     *
     * The {@link SpringViewProvider} bean from the context is automatically
     * registered for the navigator.
     */
    @Override
    protected void init(UI ui, NavigationStateManager stateManager,
            ViewDisplay display) {
        super.init(ui, stateManager, display);
        addProvider(viewProvider);
    }

}