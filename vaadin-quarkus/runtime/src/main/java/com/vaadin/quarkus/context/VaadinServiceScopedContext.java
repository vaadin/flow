/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus.context;

import java.lang.annotation.Annotation;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.event.Observes;

import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.quarkus.QuarkusVaadinServlet;
import com.vaadin.quarkus.annotation.VaadinServiceScoped;

import static jakarta.enterprise.event.Reception.IF_EXISTS;

/**
 * Context for {@link VaadinServiceScoped @VaadinServiceScoped} beans.
 */
public class VaadinServiceScopedContext extends AbstractContext {

    @Override
    protected ContextualStorage getContextualStorage(Contextual<?> contextual,
            boolean createIfNotExist) {
        QuarkusVaadinServlet servlet = (QuarkusVaadinServlet) VaadinServlet
                .getCurrent();
        String servletName;
        if (servlet != null) {
            servletName = servlet.getServletName();
        } else {
            servletName = QuarkusVaadinServlet.getCurrentServletName().get();
        }
        return BeanProvider
                .getContextualReference(Arc.container().beanManager(),
                        ContextualStorageManager.class, false)
                .getContextualStorage(servletName, createIfNotExist);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return VaadinServiceScoped.class;
    }

    @Override
    public boolean isActive() {
        VaadinServlet servlet = VaadinServlet.getCurrent();
        return servlet instanceof QuarkusVaadinServlet || (servlet == null
                && QuarkusVaadinServlet.getCurrentServletName() != null);
    }

    @ApplicationScoped
    @Unremovable
    public static class ContextualStorageManager
            extends AbstractContextualStorageManager<String> {

        public ContextualStorageManager() {
            super(true);
        }

        /**
         * Service destroy event observer.
         *
         * During application shutdown it is container specific whether this
         * observer being called, or not. Application context destroy may happen
         * earlier, and cleanup done by {@link #destroyAll()}.
         *
         * @param event
         *            service destroy event
         */
        private void onServiceDestroy(
                @Observes(notifyObserver = IF_EXISTS) ServiceDestroyEvent event) {
            if (!(event.getSource() instanceof VaadinServletService)) {
                return;
            }
            VaadinServletService service = (VaadinServletService) event
                    .getSource();
            String servletName = service.getServlet().getServletName();
            destroy(servletName);
        }

    }

}
