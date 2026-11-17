/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring.scopes;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.BrowserTab;
import com.vaadin.flow.server.VaadinSession;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the beans
 * to the {@link BrowserTab} of the current {@link UI}. Registered by default as
 * the scope "{@value #VAADIN_BROWSER_TAB_SCOPE_NAME}".
 * <p>
 * The beans are shared by all the UIs that are loaded in the same browser tab,
 * so they survive a reload and navigation to another URL of the application in
 * the same tab. They are destroyed when the browser tab is destroyed, which
 * happens some time after the tab was closed or when the session is destroyed.
 * Destruction callbacks, such as {@code @PreDestroy} methods, therefore run
 * during the request of another UI, or with no request at all, and must not
 * rely on {@link UI#getCurrent()}. See {@link BrowserTab} for how browser tabs
 * are identified and when they are destroyed.
 *
 * @see com.vaadin.flow.spring.annotation.BrowserTabScope
 *
 * @author Vaadin Ltd
 */
public class VaadinBrowserTabScope extends AbstractScope {

    /** Name the scope is registered under. */
    public static final String VAADIN_BROWSER_TAB_SCOPE_NAME = "vaadin-browser-tab";

    private static final String BEAN_STORE_ATTRIBUTE = VaadinBrowserTabScope.class
            .getName() + ".beanStore";

    /**
     * Creates the scope. Spring instantiates it.
     */
    public VaadinBrowserTabScope() {
        // Default constructor
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) {
        beanFactory.registerScope(VAADIN_BROWSER_TAB_SCOPE_NAME, this);
    }

    @Override
    public String getConversationId() {
        final VaadinSession session = getVaadinSession();
        session.getLockInstance().lock();
        try {
            return session.getSession().getId() + "-BrowserTab:"
                    + BrowserTab.get(getUI()).getId();
        } finally {
            session.getLockInstance().unlock();
        }
    }

    @Override
    protected BeanStore getBeanStore() {
        final VaadinSession session = getVaadinSession();
        session.getLockInstance().lock();
        try {
            BrowserTab tab = BrowserTab.get(getUI());
            BeanStore beanStore = (BeanStore) tab
                    .getAttribute(BEAN_STORE_ATTRIBUTE);
            if (beanStore == null) {
                BeanStore newBeanStore = new BeanStore(session);
                tab.setAttribute(BEAN_STORE_ATTRIBUTE, newBeanStore);
                tab.addDestroyListener(destroyedTab -> newBeanStore.destroy());
                beanStore = newBeanStore;
            }
            return beanStore;
        } finally {
            session.getLockInstance().unlock();
        }
    }

    private UI getUI() {
        return UI.getCurrentOrThrow();
    }
}
