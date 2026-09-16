/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.cdi;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.cdi.context.ServiceUnderTestContext;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.ServiceException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableWeld
public class CdiVaadinServletServiceExecutorTest {

    @Nested
    class UseDefaultManagedExecutor extends BaseTest {

        @WeldSetup
        public WeldInitiator weld = withManagedExecutorService().build();

        @Test
        void getExecutor_serviceInitialized_cdiManagedExecutorReturned()
                throws ServiceException {
            initService(beanManager);
            Executor executor = service.getExecutor();
            Assertions.assertSame(managedExecutorService, executor);
        }
    }

    @Nested
    class UseVaadinServiceEnabledExecutor extends BaseTest {

        @WeldSetup
        public WeldInitiator weld = withManagedExecutorService()
                .addBeans(customTaskExecutorBean()).build();

        @Test
        void getExecutor_serviceInitialized_cdiManagedExecutorReturned()
                throws Exception {
            initService(beanManager);
            Executor executor = service.getExecutor();
            Assertions.assertSame(customExecutor, executor);
        }
    }

    @Nested
    class UseDefaultVaadinExecutor extends BaseTest {

        @WeldSetup
        public WeldInitiator weld = WeldInitiator.performDefaultDiscovery();

        @Test
        void getExecutor_serviceInitialized_cdiManagedExecutorReturned()
                throws ServiceException {
            initService(beanManager);
            Executor executor = service.getExecutor();
            Assertions.assertNotNull(executor);
            Assertions.assertNotSame(managedExecutorService, executor);
            Assertions.assertNotSame(customExecutor, executor);
        }
    }

    @Nested
    class AmbiguousVaadinServiceEnabledExecutor extends BaseTest {

        public WeldInitiator weld = withManagedExecutorService()
                .addBeans(
                        customTaskExecutorBean(
                                b -> b.name("customVaadinExecutor")),
                        customTaskExecutorBean(b -> b.name("otherExecutor")))
                .build();

        @Test
        void deploymentValidation_throwsException() throws Exception {
            var initWeldMethod = ReflectTools.findMethod(WeldInitiator.class,
                    "initWeld", Object.class);
            initWeldMethod.setAccessible(true);
            Throwable error = Assertions.assertThrows(
                    InvocationTargetException.class,
                    () -> initWeldMethod.invoke(weld, this));
            error = error.getCause();
            while (error != null && !(error instanceof IllegalStateException)) {
                error = error.getCause();
            }
            assertNotNull(error);
            assertTrue(
                    error.getMessage().contains("at most one Executor bean"));
            assertTrue(error.getMessage().contains("@VaadinServiceEnabled"));
        }
    }

    // Multiple @VaadinServiceEnabled beans, alternatives are ignored
    @Nested
    class AmbiguousVaadinServiceEnabledWithAlternativeExecutor
            extends BaseTest {

        Executor expectedExecutor = Mockito.mock(Executor.class);

        @WeldSetup
        public WeldInitiator weld = withManagedExecutorService().addBeans(
                customTaskExecutorBean(
                        b -> b.name("customVaadinExecutor").alternative(true)),
                customTaskExecutorBean(b -> b.name("alternativeVaadinExecutor")
                        .creating(expectedExecutor)))
                .build();

        @Test
        void getExecutor_serviceInitialized_cdiManagedExecutorReturned()
                throws ServiceException {
            initService(beanManager);
            Executor executor = service.getExecutor();
            Assertions.assertSame(expectedExecutor, executor);
        }
    }

    private static abstract class BaseTest {

        ManagedExecutorService managedExecutorService = Mockito
                .mock(ManagedExecutorService.class);

        Executor customExecutor = Mockito.mock(Executor.class);

        @Inject
        BeanManager beanManager;

        CdiVaadinServletService service;

        void initService(BeanManager beanManager) throws ServiceException {
            ServiceUnderTestContext serviceUnderTestContext = new ServiceUnderTestContext(
                    beanManager);
            serviceUnderTestContext.activate();
            service = serviceUnderTestContext.getService();
            service.init();
        }

        WeldInitiator.Builder withManagedExecutorService() {
            return WeldInitiator.from(new Weld())
                    // Simulate @Resource injection without name or lookup into
                    // VaadinTaskExecutorSelector bean in weld mock environment
                    .bindResource(
                            "java:comp/env/com.vaadin.cdi.VaadinTaskExecutorSelector$FromResource/managedExecutor",
                            managedExecutorService);
        }

        Bean<?> customTaskExecutorBean() {
            return customTaskExecutorBean(unused -> {
            });
        }

        Bean<?> customTaskExecutorBean(
                Consumer<MockBean.Builder<Executor>> customizer) {
            MockBean.Builder<Executor> builder = MockBean.<Executor> builder()
                    .types(Executor.class).scope(Singleton.class)
                    .addQualifier(BeanLookup.SERVICE).creating(customExecutor);
            customizer.accept(builder);
            return builder.build();
        }

    }

}
