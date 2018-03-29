/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.SpringVaadinSession;

/**
 * Implementation of Spring's
 * {@link org.springframework.beans.factory.config.Scope} that binds the beans
 * to the current {@link UI}. Registered by default as the scope "
 * {@value #VAADIN_UI_SCOPE_NAME}".
 *
 * @see com.vaadin.flow.spring.annotation.UIScope
 *
 * @author Vaadin Ltd
 *
 */
public class VaadinUIScope extends AbstractScope {

    public static final String VAADIN_UI_SCOPE_NAME = "vaadin-ui";

    private static class UIStoreWrapper
            implements ComponentEventListener<DetachEvent> {

        private final VaadinSession session;

        private final Registration sessionDestroyListenerRegistration;

        private final Map<Integer, BeanStore> uiStores;

        private UIStoreWrapper(VaadinSession session) {
            session.checkHasLock();
            uiStores = new HashMap<>();
            this.session = session;
            if (session instanceof SpringVaadinSession) {
                sessionDestroyListenerRegistration = null;
                ((SpringVaadinSession) session)
                        .addDestroyListener(event -> destroy());
            } else {
                sessionDestroyListenerRegistration = session.getService()
                        .addSessionDestroyListener(event -> destroy());
            }
        }

        @Override
        public void onComponentEvent(DetachEvent event) {
            session.checkHasLock();
            UI ui = event.getUI();
            BeanStore beanStore = uiStores.remove(ui.getUIId());
            if (beanStore != null) {
                beanStore.destroy();
            }
        }

        BeanStore getBeanStore(UI ui) {
            BeanStore beanStore = uiStores.get(ui.getUIId());
            if (beanStore == null) {
                beanStore = new BeanStore(session);
                uiStores.put(ui.getUIId(), beanStore);
                ui.addDetachListener(this);
            }
            return beanStore;
        }

        private void destroy() {
            session.lock();
            try {
                session.setAttribute(UIStoreWrapper.class, null);
                uiStores.values().forEach(BeanStore::destroy);
                uiStores.clear();
            } finally {
                session.unlock();
                if (sessionDestroyListenerRegistration != null) {
                    sessionDestroyListenerRegistration.remove();
                }
            }
        }

    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.registerScope(VAADIN_UI_SCOPE_NAME, this);
    }

    @Override
    public String getConversationId() {
        return getVaadinSession().getSession().getId() + "-UI:"
                + getUI().getUIId();
    }

    @Override
    protected BeanStore getBeanStore() {
        final VaadinSession session = getVaadinSession();
        session.lock();
        try {
            UIStoreWrapper wrapper = session.getAttribute(UIStoreWrapper.class);
            if (wrapper == null) {
                wrapper = new UIStoreWrapper(session);
                session.setAttribute(UIStoreWrapper.class, wrapper);
            }
            return wrapper.getBeanStore(getUI());
        } finally {
            session.unlock();
        }
    }

    private UI getUI() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "There is no UI available. The UI scope is not active");
        }
        return ui;
    }
}
