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
package com.vaadin.spring.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.spring.navigator.ViewActivationListener;
import com.vaadin.ui.UI;

/**
 * Default implementation of {@link com.vaadin.spring.internal.ViewCache}. For
 * internal use only.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
public class DefaultViewCache implements ViewCache {

    private static final long serialVersionUID = 4634842615905376953L;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultViewCache.class);

    private Map<String, ViewBeanStore> beanStores = new HashMap<String, ViewBeanStore>();

    private String viewUnderConstruction = null;
    // During the re-creation of a view with the same name, keep the old context
    // after view creation until the scope has actually changed.
    // oldViewBeanStore is used during view creation, and newViewBeanStore after
    // view creation until view activation.
    private ViewBeanStore oldViewBeanStore = null;
    private ViewBeanStore newViewBeanStore = null;

    private String activeView = null;

    private ViewActivationListener listener;

    @SuppressWarnings("serial")
    public DefaultViewCache() {
        Navigator navigator = getCurrentUI().getNavigator();
        if (navigator instanceof SpringNavigator) {
            listener = new ViewActivationListener() {
                @Override
                public void viewActivated(ViewActivationEvent event) {
                    if (!event.isActivated()) {
                        viewDeactivated(event.getViewName());
                    } else {
                        DefaultViewCache.this
                                .viewActivated(event.getViewName());
                    }
                }
            };
            ((SpringNavigator) navigator).addViewActivationListener(listener);
        } else {
            LOGGER.warn(
                    "The Navigator used does not extend SpringNavigator. View scope support may be limited, and future versions of Vaadin Spring may remove support for using a plain Navigator.");
        }
    }

    /**
     * Called by {@link com.vaadin.spring.navigator.SpringViewProvider} when a
     * view scoped view is about to be created.
     *
     * @param viewName
     *            the {@link com.vaadin.spring.annotation.SpringView#name()
     *            name} of the view (not the bean name).
     */
    @Override
    public void creatingView(String viewName) {
        LOGGER.trace("Creating view [{}] in cache [{}]", viewName, this);

        // keep the old bean store until we have truly switched views in the
        // navigator
        oldViewBeanStore = beanStores.get(viewName);
        if (oldViewBeanStore != null) {
            beanStores.remove(viewName);
        }

        getOrCreateBeanStore(viewName);
        viewUnderConstruction = viewName;
    }

    /**
     * Called by {@link com.vaadin.spring.navigator.SpringViewProvider} when a
     * view scoped view has been created.
     *
     * @param viewName
     *            the {@link com.vaadin.spring.annotation.SpringView#name()
     *            name} of the view (not the bean name).
     * @param viewInstance
     *            the created view instance, or {@code null} if the view could
     *            not be created.
     */
    @Override
    public void viewCreated(String viewName, View viewInstance) {
        LOGGER.trace("View [{}] created in cache [{}]", viewName, this);
        viewUnderConstruction = null;
        ViewBeanStore beanStore = getOrCreateBeanStore(viewName);
        if (viewInstance == null) {
            LOGGER.trace(
                    "There was a problem creating the view [{}] in cache [{}], destroying its bean store and restoring the old one (if any)",
                    viewName, this);
            beanStore.destroy();
            // nothing to change in next viewActivated()
            beanStore = null;
        }
        // temporarily revert to the old bean store until the scope actually
        // changes
        newViewBeanStore = beanStore;
        if (oldViewBeanStore != null) {
            beanStores.put(viewName, oldViewBeanStore);
        } else {
            beanStores.remove(viewName);
        }
        oldViewBeanStore = null;
    }

    private void viewActivated(String viewName) {
        LOGGER.trace("View [{}] activated in cache [{}]", viewName, this);
        activeView = viewName;

        // actually activate the scope for the new view
        if (newViewBeanStore != null
                && viewName.equals(newViewBeanStore.viewName)) {
            beanStores.put(viewName, newViewBeanStore);
        }
        newViewBeanStore = null;
    }

    private void viewDeactivated(String viewName) {
        LOGGER.trace(
                "View [{}] deactivated in cache [{}], cleaning up view scoped bean stores",
                viewName, this);
        if (viewName.equals(activeView)) {
            activeView = null;
        }
        // If no bean store exists, not a view scoped view.
        // Bypassing getBeanStore() not to use its error detection.
        ViewBeanStore beanStore = beanStores.get(viewName);
        if (beanStore != null) {
            beanStore.destroy();
        }
        LOGGER.trace("Bean stores stored in cache [{}]: {}", this,
                beanStores.size());
    }

    @Override
    public BeanStore getCurrentViewBeanStore() {
        if (viewUnderConstruction != null) {
            LOGGER.trace(
                    "Currently the view [{}] is under construction in cache [{}], returning its bean store",
                    viewUnderConstruction, this);
            return getBeanStore(viewUnderConstruction);
        } else if (activeView != null) {
            LOGGER.trace(
                    "Currently the view [{}] is active in cache [{}], returning its bean store",
                    activeView, this);
            return getBeanStore(activeView);
        } else {
            throw new IllegalStateException("No active view");
        }
    }

    @PreDestroy
    void destroy() {
        LOGGER.trace(
                "View cache [{}] has been destroyed, destroying all bean stores");
        for (ViewBeanStore beanStore : new HashSet<ViewBeanStore>(
                beanStores.values())) {
            beanStore.destroy();
        }
        UI currentUi = getCurrentUI();
        if (currentUi != null) {
            Navigator navigator = currentUi.getNavigator();
            if (navigator instanceof SpringNavigator) {
                // not in legacy mode
                ((SpringNavigator) navigator)
                        .removeViewActivationListener(listener);
            }
        }
        Assert.isTrue(beanStores.isEmpty(),
                "beanStores should have been emptied by the destruction callbacks");
    }

    private ViewBeanStore getOrCreateBeanStore(final String viewName) {
        ViewBeanStore beanStore = beanStores.get(viewName);
        if (beanStore == null) {
            UI ui = getCurrentUI();
            if (ui == null) {
                throw new IllegalStateException(
                        "No UI bound to current thread");
            }
            beanStore = new ViewBeanStore(ui, viewName,
                    new BeanStore.DestructionCallback() {

                        private static final long serialVersionUID = 5580606280246825742L;

                        @Override
                        public void beanStoreDestroyed(BeanStore beanStore) {
                            beanStores.remove(viewName);
                        }
                    });
            beanStores.put(viewName, beanStore);
        }
        return beanStore;
    }

    /**
     * Returns the current UI.
     */
    private UI getCurrentUI() {
        return UI.getCurrent();
    }

    private ViewBeanStore getBeanStore(String viewName) {
        ViewBeanStore beanStore = beanStores.get(viewName);
        if (beanStore == null) {
            throw new IllegalStateException(
                    "The view " + viewName + " has not been created");
        }
        return beanStore;
    }

    // ViewChangeListener is only used when the navigator is not a
    // SpringNavigator
    class ViewBeanStore extends SessionLockingBeanStore
            implements ViewChangeListener {

        private static final long serialVersionUID = -7655740852919880134L;

        private final String viewName;
        private final Navigator navigator;

        ViewBeanStore(UI ui, String viewName,
                DestructionCallback destructionCallback) {
            super(ui.getSession(), ui.getId() + ":" + viewName,
                    destructionCallback);
            this.viewName = viewName;
            navigator = ui.getNavigator();
            if (navigator == null) {
                throw new IllegalStateException("UI has no Navigator");
            }
            // backwards compatibility
            if (!(navigator instanceof SpringNavigator)) {
                LOGGER.trace("Adding [{}} as view change listener to [{}]",
                        this, navigator);
                navigator.addViewChangeListener(this);
            }
        }

        @Override
        public void destroy() {
            // backwards compatibility
            if (!(navigator instanceof SpringNavigator)) {
                LOGGER.trace("Removing [{}] as view change listener from [{}]",
                        this, navigator);
                navigator.removeViewChangeListener(this);
            }
            super.destroy();
        }

        // only used if the navigator is not a SpringNavigator (backwards
        // compatibility mode)
        @Override
        public boolean beforeViewChange(ViewChangeEvent event) {
            return true;
        }

        // only used if the navigator is not a SpringNavigator (backwards
        // compatibility mode)
        @Override
        public void afterViewChange(ViewChangeEvent viewChangeEvent) {
            if (viewName.equals(viewChangeEvent.getViewName())) {
                viewActivated(viewName);
            } else {
                viewDeactivated(viewName);
            }
        }

    }

}
