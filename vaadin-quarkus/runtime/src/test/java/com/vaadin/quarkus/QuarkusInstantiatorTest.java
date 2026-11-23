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
package com.vaadin.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Locale;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;
import com.vaadin.quarkus.context.ServiceUnderTestContext;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@QuarkusTest
public class QuarkusInstantiatorTest {

    @Singleton
    public static class SomeCdiBean {
    }

    @ApplicationScoped
    public static class ScopedCdiBean {
    }

    @Interceptor
    @MyBinding
    public static class MyInterceptor {

        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    public @interface MyBinding {

    }

    @Singleton
    public static class InterceptedCdiBean {

        @MyBinding
        public void exec() {
            // only the interception matters, not what the method does
        }
    }

    public static class ParentBean {

        @Inject
        private BeanManager bm;

        public BeanManager getBm() {
            return bm;
        }

    }

    @Dependent
    @Unremovable
    @Tag("div")
    public static class CdiComponent extends Component {
        @Inject
        BeanManager beanManager;
    }

    @Vetoed
    @Singleton
    public static class NotACdiBean extends ParentBean {
    }

    public static class Ambiguous extends ParentBean {
    }

    @VaadinServiceEnabled
    @Singleton
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
    @Singleton
    public static class TestMenuAccessControl implements MenuAccessControl {

        @Override
        public void setPopulateClientSideMenu(
                PopulateClientMenu populateClientSideMenu) {
            // the tests only look up this bean, they do not configure it
        }

        @Override
        public PopulateClientMenu getPopulateClientSideMenu() {
            return null;
        }
    }

    @VaadinServiceEnabled
    @Singleton
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

    @Singleton
    @VaadinServiceEnabled
    public static class CdiBeanServiceInitListener
            implements VaadinServiceInitListener {

        ServiceInitEvent event;

        public void serviceInit(ServiceInitEvent event) {
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
    private QuarkusInstantiatorFactory instantiatorFactory;

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
        assertNotNull(i18NProvider);
        assertTrue(i18NProvider instanceof I18NTestProvider);
    }

    @Test
    public void getMenuAccessControl_beanProvided_instanceReturned() {
        MenuAccessControl menuAccessControl = instantiator
                .getMenuAccessControl();
        assertNotNull(menuAccessControl);
        assertTrue(menuAccessControl instanceof TestMenuAccessControl);
    }

    @Test
    public void getPageTitleGenerator_beanProvided_instanceReturned() {
        PageTitleGenerator pageTitleGenerator = instantiator
                .getPageTitleGenerator();
        assertNotNull(pageTitleGenerator);
        assertTrue(pageTitleGenerator instanceof TestPageTitleGenerator);
    }

    @Test
    public void getServiceInitListeners_CDIBeanListenerExists_containsCDIBeanListener() {
        assertTrue(instantiator.getServiceInitListeners()
                .anyMatch(CdiBeanServiceInitListener.class::isInstance));
    }

    @Test
    public void getServiceInitListeners_eventFired_cdiObserverCalled() {
        VaadinService service = mock(VaadinService.class);
        ServiceInitEvent event = new ServiceInitEvent(service);
        instantiator.getServiceInitListeners()
                .filter(listener -> listener.getClass().getPackage()
                        .equals(QuarkusInstantiator.class.getPackage()))
                .forEach(listener -> listener.serviceInit(event));
        assertSame(event, serviceInitObserver.getEvent());
    }

    @Test
    public void getOrCreate_beanSingleton_sameInstanceReturned() {
        assertSame(singleton, instantiator.getOrCreate(SomeCdiBean.class));
    }

    @Test
    @Disabled("Unmanaged instance are currently not supported in Quarkus add-on, see comments in QuarkusInstantiator.getOrCreate")
    public void getOrCreate_beanUnsatisfied_instantiatedAndInjectionOccurred() {
        NotACdiBean instance = instantiator.getOrCreate(NotACdiBean.class);
        assertNotNull(instance);
        assertNotNull(instance.getBm());
    }

    @Test
    @Disabled("Unmanaged instance are currently not supported in Quarkus add-on, see comments in QuarkusInstantiator.getOrCreate")
    public void getOrCreate_beanAmbiguous_instantiatedAndInjectionOccurred() {
        ParentBean instance = instantiator.getOrCreate(ParentBean.class);
        assertNotNull(instance);
        assertNotNull(instance.getBm());
    }

    @Test
    public void createComponent_componentIsCreated() {
        CdiComponent component = instantiator
                .createComponent(CdiComponent.class);
        assertNotNull(component);
        assertNotNull(component.beanManager);
    }

    @Test
    public void createComponent_componentIsCreatedOnEveryCall() {
        CdiComponent component = instantiator
                .createComponent(CdiComponent.class);
        assertNotNull(component);

        CdiComponent anotherComponent = instantiator
                .createComponent(CdiComponent.class);
        assertNotEquals(component, anotherComponent);
    }

    @Test
    public void getApplicationClass_regularClass_getsSameClass() {
        SomeCdiBean instance = instantiator.getOrCreate(SomeCdiBean.class);
        assertSame(SomeCdiBean.class,
                instantiator.getApplicationClass(instance));
        assertSame(SomeCdiBean.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    public void getApplicationClass_scopedBean_getsApplicationClass() {
        ScopedCdiBean instance = instantiator.getOrCreate(ScopedCdiBean.class);
        assertSame(ScopedCdiBean.class,
                instantiator.getApplicationClass(instance));
        assertSame(ScopedCdiBean.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    public void getApplicationClass_proxiedBean_getsApplicationClass() {
        InterceptedCdiBean instance = instantiator
                .getOrCreate(InterceptedCdiBean.class);
        instance.exec();
        assertNotSame(InterceptedCdiBean.class, instance.getClass());
        assertSame(InterceptedCdiBean.class,
                instantiator.getApplicationClass(instance));
        assertSame(InterceptedCdiBean.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

}
