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
package com.vaadin.quarkus.context;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.quarkus.QuarkusVaadinServlet;
import com.vaadin.quarkus.annotation.VaadinServiceScoped;

@QuarkusTest
public class ServiceContextTest
        extends AbstractContextTest<VaadinServiceScopedContext> {
    @Inject
    private BeanManager beanManager;

    @Test
    public void isActive_noServletAndNoServletName_notActive()
            throws InterruptedException {
        // getCurrentServletName() returns an Optional, so it is never null and
        // asking whether it is decided nothing: the context reported itself
        // active with no servlet name on the thread, and then read the name
        // off the empty Optional. Checked on a thread of its own so that
        // neither the current servlet nor the name can be left over from
        // another test.
        AtomicReference<Boolean> active = new AtomicReference<>();
        AtomicReference<Boolean> namePresent = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            namePresent.set(
                    QuarkusVaadinServlet.getCurrentServletName().isPresent());
            active.set(new VaadinServiceScopedContext().isActive());
        });
        thread.start();
        thread.join();

        Assertions.assertFalse(namePresent.get(),
                "a thread that has not been inside init() or service() has no "
                        + "servlet name, and asking must not throw");
        Assertions.assertFalse(active.get(),
                "the service context is not active without a servlet name");
    }

    @Override
    protected UnderTestContext newContextUnderTest() {
        return new ServiceUnderTestContext(beanManager);
    }

    @Override
    protected Class<VaadinServiceScopedContext> getContextType() {
        return VaadinServiceScopedContext.class;
    }

    @VaadinServiceScoped
    public static class ServiceScopedTestBean extends TestBean {
    }

}
