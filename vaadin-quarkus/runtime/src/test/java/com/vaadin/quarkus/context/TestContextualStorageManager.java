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

import java.util.Set;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.Dependent;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.quarkus.context.RouteScopedContext.ContextualStorageManager;
import com.vaadin.quarkus.context.RouteScopedContext.RouteStorageKey;

/**
 *
 * Quarkus will use newly created instance of ContextualStorageManager to fire
 * navigation events. As a result <code>@Observes</code> methods will be called
 * on instance which is not used by any other piece of the code. To be able to
 * call it on the correct instance it's retreived from the session attribute:
 * it's the valid way since the bean is in fact session scoped (originally).
 */
@Dependent
@Unremovable
public class TestContextualStorageManager extends ContextualStorageManager {
    @Override
    protected Set<RouteStorageKey> getKeySet() {
        TestContextualStorageManager manager = (TestContextualStorageManager) VaadinSession
                .getCurrent().getAttribute(BeanManagerProxy
                        .getAttributeName(TestContextualStorageManager.class));
        if (manager != this) {
            return manager.getKeySet();
        }
        return super.getKeySet();
    }

    @Override
    protected void destroy(RouteStorageKey key) {
        TestContextualStorageManager manager = (TestContextualStorageManager) VaadinSession
                .getCurrent().getAttribute(BeanManagerProxy
                        .getAttributeName(TestContextualStorageManager.class));
        if (manager != this) {
            manager.destroy(key);
        } else {
            super.destroy(key);
        }
    }

}
