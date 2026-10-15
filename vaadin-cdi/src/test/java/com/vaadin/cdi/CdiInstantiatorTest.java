/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;
import jakarta.servlet.ServletException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.cdi.annotation.VaadinServiceEnabled;
import com.vaadin.cdi.context.ServiceUnderTestContext;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.MenuAccessControl;

public class CdiInstantiatorTest extends AbstractWeldTest {

    @Singleton
    public static class SomeCdiBean {
    }

    @ApplicationScoped
    public static class ScopedCdiBean {
    }

    public static class MyInterceptor {

        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }

    @Interceptors(MyInterceptor.class)
    public static class InterceptedCdiBean {

        public void exec() {

        }
    }

    public static class ParentBean {

        @Inject
        private BeanManager bm;

        public BeanManager getBm() {
            return bm;
        }

    }

    @Singleton
    public static class SingletonComponent extends Div {
        @Inject
        BeanManager beanManager;
    }

    @Vetoed
    public static class NotACdiBean extends ParentBean {
    }

    public static class Ambiguous extends ParentBean {
    }

    @VaadinServiceEnabled
    public static class I18NTestProvider implements I18NProvider {

        @Override
        public List<Locale> getProvidedLocales() {
            return null;
        }

        @Override
        public String getTranslation(String key, Locale locale,
                Object... params) {
            return null;
        }

    }

    @VaadinServiceEnabled
    public static class TestMenuAccessControl implements MenuAccessControl {

        @Override
        public void setPopulateClientSideMenu(
                PopulateClientMenu populateClientSideMenu) {

        }

        @Override
        public PopulateClientMenu getPopulateClientSideMenu() {
            return null;
        }
    }

    @VaadinServiceEnabled
    public static class TestPageTitleGenerator implements PageTitleGenerator {

        @Override
        public String generatePageTitle(PageTitleContext context) {
            return null;
        }
    }

    @Singleton
    public static class ServiceInitObserver {

        ServiceInitEvent event;

        public void serviceInit(@Observes ServiceInitEvent event) {
            this.event = event;
        }

        public ServiceInitEvent getEvent() {
            return event;
        }

    }

    @Inject
    private BeanManager beanManager;

    @Inject
    @VaadinServiceEnabled
    private CdiInstantiatorFactory instantiatorFactory;

    private Instantiator instantiator;

    @Inject
    private SomeCdiBean singleton;

    @Inject
    private ServiceInitObserver serviceInitObserver;

    private ServiceUnderTestContext serviceUnderTestContext;

    @BeforeEach
    public void setUp() {
        serviceUnderTestContext = new ServiceUnderTestContext(beanManager);
        serviceUnderTestContext.activate();
        instantiator = instantiatorFactory
                .createInstantitor(VaadinService.getCurrent());
    }

    @AfterEach
    public void tearDown() {
        serviceUnderTestContext.tearDownAll();
    }

    @Test
    public void getI18NProvider_beanEnabled_instanceReturned() {
        I18NProvider i18NProvider = instantiator.getI18NProvider();
        Assertions.assertNotNull(i18NProvider);
        Assertions.assertTrue((i18NProvider instanceof I18NTestProvider));
    }

    @Test
    public void getMenuAccessControl_beanEnabled_instanceReturned() {
        MenuAccessControl menuAccessControl = instantiator
                .getMenuAccessControl();
        Assertions.assertNotNull(menuAccessControl);
        Assertions.assertInstanceOf(TestMenuAccessControl.class,
                menuAccessControl);
    }

    @Test
    public void getPageTitleGenerator_beanEnabled_instanceReturned() {
        PageTitleGenerator pageTitleGenerator = instantiator
                .getPageTitleGenerator();
        Assertions.assertNotNull(pageTitleGenerator);
        Assertions.assertInstanceOf(TestPageTitleGenerator.class,
                pageTitleGenerator);
    }

    @Test
    public void getServiceInitListeners_javaSPIListenerExists_containsJavaSPIListener() {
        Assertions.assertTrue(instantiator.getServiceInitListeners().anyMatch(
                listener -> listener instanceof JavaSPIVaadinServiceInitListener));
    }

    @Test
    public void getServiceInitListeners_eventFired_cdiObserverCalled() {
        VaadinService service = Mockito.mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);
        instantiator.getServiceInitListeners()
                .filter(listener -> listener.getClass().getPackage()
                        .equals(CdiInstantiator.class.getPackage()))
                .forEach(listener -> listener.serviceInit(event));
        Assertions.assertSame(event, serviceInitObserver.getEvent());
    }

    @Test
    public void getOrCreate_beanSingleton_sameInstanceReturned() {
        Assertions.assertSame(singleton,
                instantiator.getOrCreate(SomeCdiBean.class));
    }

    @Test
    public void getOrCreate_beanUnsatisfied_instantiatedAndInjectionOccurred() {
        NotACdiBean instance = instantiator.getOrCreate(NotACdiBean.class);
        Assertions.assertNotNull(instance);
        Assertions.assertNotNull(instance.getBm());
    }

    @Test
    public void getOrCreate_beanAmbiguous_instantiatedAndInjectionOccurred() {
        ParentBean instance = instantiator.getOrCreate(ParentBean.class);
        Assertions.assertNotNull(instance);
        Assertions.assertNotNull(instance.getBm());
    }

    @Test
    public void createComponent_componentIsCreated() {
        SingletonComponent component = instantiator
                .createComponent(SingletonComponent.class);
        Assertions.assertNotNull(component);
        Assertions.assertNotNull(component.beanManager);
    }

    @Test
    public void createComponent_componentIsCreatedOnEveryCall()
            throws ServletException {
        SingletonComponent component = instantiator
                .createComponent(SingletonComponent.class);
        Assertions.assertNotNull(component);

        SingletonComponent anotherComponent = instantiator
                .createComponent(SingletonComponent.class);
        Assertions.assertNotEquals(component, anotherComponent);
    }

    @Test
    public void init_callsUsageStatistics() {
        // @Before does instantiator.init()
        // There will be other entries too to filter out
        List<UsageStatistics.UsageEntry> entries = UsageStatistics.getEntries()
                .filter(entry -> entry.getName().contains("Cdi"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, entries.size());

        UsageStatistics.UsageEntry entry = entries.get(0);
        Assertions.assertEquals("flow/CdiInstantiator", entry.getName());
    }

    @Test
    public void getApplicationClass_regularClass_getsSameClass()
            throws ServletException {
        SomeCdiBean instance = instantiator.getOrCreate(SomeCdiBean.class);
        Assertions.assertSame(SomeCdiBean.class,
                instantiator.getApplicationClass(instance));
        Assertions.assertSame(SomeCdiBean.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    public void getApplicationClass_scopedBean_getsApplicationClass()
            throws ServletException {
        ScopedCdiBean instance = instantiator.getOrCreate(ScopedCdiBean.class);
        Assertions.assertSame(ScopedCdiBean.class,
                instantiator.getApplicationClass(instance));
        Assertions.assertSame(ScopedCdiBean.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    public void getApplicationClass_proxiedBean_getsApplicationClass()
            throws ServletException {
        InterceptedCdiBean instance = instantiator
                .getOrCreate(InterceptedCdiBean.class);
        instance.exec();
        Assertions.assertNotSame(InterceptedCdiBean.class, instance.getClass());
        Assertions.assertSame(InterceptedCdiBean.class,
                instantiator.getApplicationClass(instance));
        Assertions.assertSame(InterceptedCdiBean.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

}
