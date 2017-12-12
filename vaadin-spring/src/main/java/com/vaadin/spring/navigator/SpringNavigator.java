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
package com.vaadin.spring.navigator;

import com.vaadin.navigator.NavigationStateManager;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.internal.UIScopeImpl;
import com.vaadin.spring.navigator.ViewActivationListener.ViewActivationEvent;
import com.vaadin.spring.server.SpringVaadinServletService;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.SingleComponentContainer;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A Navigator that automatically uses {@link SpringViewProvider} and allows
 * late initialization.
 *
 * @author Vaadin Ltd
 */
@UIScope
public class SpringNavigator extends Navigator {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SpringNavigator.class);

    @Autowired
    private transient ApplicationContext applicationContext;

    @Autowired
    private SpringViewProvider viewProvider;

    private String currentViewName;

    private final List<ViewActivationListener> activationListeners = new LinkedList<ViewActivationListener>();

    public SpringNavigator() {
    }

    public SpringNavigator(UI ui, ComponentContainer container) {
        super(ui, container);
    }

    public SpringNavigator(UI ui, NavigationStateManager stateManager,
            ViewDisplay display) {
        super(ui, stateManager, display);
    }

    public SpringNavigator(UI ui, SingleComponentContainer container) {
        super(ui, container);
    }

    public SpringNavigator(UI ui, ViewDisplay display) {
        super(ui, display);
    }

    /**
     * Fires the {@link ViewActivationEvent}
     *
     * @param viewName
     * @param active
     */
    private void fireActivationEvent(String viewName, boolean active) {
        List<ViewActivationListener> listeners = new ArrayList<ViewActivationListener>(
                activationListeners);
        ViewActivationEvent event = new ViewActivationEvent(this, active,
                viewName);
        for (ViewActivationListener listener : listeners) {
            listener.viewActivated(event);
        }
    }

    /**
     * Adds a listener on view activation
     *
     * @param listener
     */
    public void addViewActivationListener(ViewActivationListener listener) {
        activationListeners.add(listener);
    }

    /**
     * Removes a listener on view activation
     *
     * @param listener
     */
    public void removeViewActivationListener(ViewActivationListener listener) {
        activationListeners.remove(listener);
    }

    @Override
    protected void switchView(ViewChangeEvent event) {
        // it's ok to navigate to the desired view, so I can deactivate
        // the previous View and activate the current. This code is strongly
        // dependent on the Navigator implementation. Probably the Navigator
        // should offer a way to intercept the "beforeViewEnter" event
        String viewName = event.getViewName();
        boolean viewNameChanged = !viewName.equals(currentViewName);
        if (viewNameChanged) {
            if (currentViewName != null) {
                // deactivate
                fireActivationEvent(currentViewName, false);
            }
        }
        super.switchView(event);
        if (viewNameChanged) {
            currentViewName = viewName;
            // activate
            fireActivationEvent(currentViewName, true);
        }
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
        init(ui, null, display);
    }

    /**
     * {@inheritDoc}
     *
     * The {@link SpringViewProvider} bean from the context is automatically
     * registered for the navigator.
     */
    @Override
    public void init(UI ui, NavigationStateManager stateManager,
            ViewDisplay display) {
        super.init(ui, stateManager, display);
        addProvider(viewProvider);
    }

    /**
     * Registers a view class for the view to show when no other view matches
     * the navigation state. This implicitly sets an appropriate error view
     * provider and overrides any previous
     * {@link #setErrorProvider(ViewProvider)} call.
     * <p>
     * A bean of the given type is fetched on demand from the application
     * context to be used as the error view. As a fallback mechanism for
     * backwards compatibility, {@link Class#newInstance()} is used if no such
     * bean is found.
     * <p>
     * Note that an error view bean must be UI or prototype scoped.
     *
     * @param viewClass
     *            The View class whose instance should be used as the error
     *            view.
     */
    @Override
    public void setErrorView(final Class<? extends View> viewClass) {
        if (viewClass == null) {
            setErrorProvider(null);
            return;
        }
        String[] beanNames = BeanFactoryUtils
                .beanNamesForTypeIncludingAncestors(getWebApplicationContext(),
                        viewClass);
        /*
         * Beans count==0 here means fallback into direct class instantiation No
         * need to check for the scope then
         */
        if (beanNames.length > 1) {
            throw new NoUniqueBeanDefinitionException(viewClass);
        } else if (beanNames.length == 1) {
            BeanDefinition beanDefinition = viewProvider
                    .getBeanDefinitionRegistry()
                    .getBeanDefinition(beanNames[0]);
            String scope = beanDefinition.getScope();
            if (!UIScopeImpl.VAADIN_UI_SCOPE_NAME.equals(scope)
                    && !"prototype".equals(scope)) {
                throw new BeanDefinitionValidationException(
                        "Error view must have UI or prototype scope");
            }
        }

        setErrorProvider(new ViewProvider() {
            @Override
            public View getView(String viewName) {
                try {
                    return getWebApplicationContext().getBean(viewClass);
                } catch (NoUniqueBeanDefinitionException e) {
                    throw e;
                } catch (NoSuchBeanDefinitionException e) {
                    // fallback mechanism
                    LOGGER.info(
                            "Could not find error view bean of class [{}] in application context, creating it with Class.newInstance()",
                            viewClass.getName());
                    try {
                        return viewClass.newInstance();
                    } catch (Exception e2) {
                        throw new RuntimeException(e2);
                    }
                }
            }

            @Override
            public String getViewName(String navigationState) {
                return navigationState;
            }
        });
    }

    protected ApplicationContext getWebApplicationContext() {
        if (applicationContext == null) {
            // Assume we have serialized and deserialized and Navigator is
            // trying to find a view so UI.getCurrent() is available
            UI ui = UI.getCurrent();
            if (ui == null) {
                throw new IllegalStateException(
                        "Could not find application context and no current UI is available");
            }
            applicationContext = ((SpringVaadinServletService) ui.getSession()
                    .getService()).getWebApplicationContext();
        }

        return applicationContext;
    }

}
