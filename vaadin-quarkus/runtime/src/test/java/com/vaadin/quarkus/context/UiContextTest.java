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

import java.lang.reflect.Proxy;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.BeanManager;

import com.vaadin.quarkus.context.UiContextTest.TestUIScopedContext;

@QuarkusTest
public class UiContextTest extends AbstractContextTest<TestUIScopedContext> {

    @Override
    protected UnderTestContext newContextUnderTest() {
        return new UIUnderTestContext();
    }

    @Override
    protected Class<TestUIScopedContext> getContextType() {
        return TestUIScopedContext.class;
    }

    public static class TestUIScopedContext extends UIScopedContext {

        @Override
        BeanManager getBeanManager() {
            BeanManager beanManager = super.getBeanManager();
            BeanManager proxy = (BeanManager) Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class<?>[] { BeanManager.class },
                    new BeanManagerProxy(beanManager,
                            UIScopedContext.ContextualStorageManager.class));
            return proxy;
        }
    }

    @Dependent
    @Unremovable
    @Alternative
    @Priority(1)
    public static class TestContextualStorageManager
            extends UIScopedContext.ContextualStorageManager {

    }

}
