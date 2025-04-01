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
package com.vaadin.flow.spring.instantiator;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.atmosphere.cpr.AtmosphereFramework;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.communication.JSR356WebsocketInitializer;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.spring.SpringInstantiator;
import com.vaadin.flow.spring.SpringServlet;

@RunWith(SpringRunner.class)
@Import(SpringInstantiatorTest.TestConfiguration.class)
public class SpringInstantiatorTest {

    @Autowired
    private ApplicationContext context;

    // NOTE: some test expect configuration to have proxyBeanMethods = true
    @Configuration
    @ComponentScan
    public static class TestConfiguration {

        // Expose at least one bean definition so that the configuration class
        // get proxied
        @Bean
        Dummy dummy() {
            return new Dummy();
        }

        static class Dummy {
        }

    }

    public static class RouteTarget1 extends Div {

    }

    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public static class RouteTarget2 extends Div {

    }

    @Component
    public static class TestVaadinServiceInitListener
            implements VaadinServiceInitListener {

        @Override
        public void serviceInit(ServiceInitEvent event) {
        }

    }

    @Component
    public static class ServiceInitListenerWithSpringEvent {

        boolean called;

        @EventListener
        public void init(ServiceInitEvent event) {
            called = true;
        }
    }

    @Component
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

    @Test
    public void createRouteTarget_pojo_instanceIsCreated()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);

        RouteTarget1 target1 = instantiator
                .createRouteTarget(RouteTarget1.class, null);
        Assert.assertNotNull(target1);
    }

    @Test
    public void getServiceInitListeners_springManagedBeanAndJavaSPI_bothClassesAreInStream()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);

        Set<?> set = instantiator.getServiceInitListeners()
                .map(Object::getClass).collect(Collectors.toSet());

        Assert.assertTrue(set.contains(TestVaadinServiceInitListener.class));
        Assert.assertTrue(set.contains(JavaSPIVaadinServiceInitListener.class));
    }

    @Test
    public void getServiceInitListeners_springEventListener()
            throws ServletException {
        getInstantiator(context);

        Assert.assertTrue(context
                .getBean(ServiceInitListenerWithSpringEvent.class).called);
    }

    @Test
    public void createRouteTarget_springManagedBean_instanceIsCreated()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);

        RouteTarget2 singleton = context.getBean(RouteTarget2.class);

        Assert.assertEquals(singleton,
                instantiator.createRouteTarget(RouteTarget2.class, null));
    }

    @Test
    public void getI18NProvider_i18nProviderIsABean_i18nProviderIsAvailable()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);

        Assert.assertNotNull(instantiator.getI18NProvider());
        Assert.assertEquals(I18NTestProvider.class,
                instantiator.getI18NProvider().getClass());
    }

    @Test
    public void createComponent_componentIsCreatedOnEveryCall()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);
        RouteTarget2 component = instantiator
                .createComponent(RouteTarget2.class);
        Assert.assertNotNull(component);

        RouteTarget2 anotherComponent = instantiator
                .createComponent(RouteTarget2.class);
        Assert.assertNotEquals(component, anotherComponent);
    }

    public static VaadinService getService(ApplicationContext context,
            Properties configProperties) throws ServletException {
        return getService(context, configProperties, false);
    }

    public static VaadinService getService(ApplicationContext context,
            Properties configProperties, boolean rootMapping)
            throws ServletException {
        SpringServlet servlet = new SpringServlet(context, rootMapping) {
            @Override
            protected DeploymentConfiguration createDeploymentConfiguration(
                    Properties initParameters) {
                if (configProperties != null) {
                    configProperties.putAll(initParameters);
                    return super.createDeploymentConfiguration(
                            configProperties);
                }
                return super.createDeploymentConfiguration(initParameters);
            }
        };
        String servletName = SpringServlet.class.getSimpleName();

        ServletConfig config = Mockito.mock(ServletConfig.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(servletContext.getClassLoader())
                .thenReturn(servlet.getClass().getClassLoader());

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(appConfig.getContext())
                .thenReturn(new VaadinServletContext(servletContext));
        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);

        Lookup lookup = Mockito.mock(Lookup.class);
        ResourceProvider provider = Mockito.mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(provider);

        StaticFileHandlerFactory staticFileHandlerFactory = vaadinService -> new StaticFileServer(
                (VaadinServletService) vaadinService);
        Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(staticFileHandlerFactory);

        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        Mockito.when(config.getServletContext()).thenReturn(servletContext);
        Mockito.when(config.getServletName()).thenReturn(servletName);
        Mockito.when(config.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(servletContext.getServerInfo()).thenReturn("MockServer");
        // Prevent Atmosphere initialization by providing a mock framework
        // instance. Push is not required by calling tests, and initialization
        // would anyway fail because of mocking Servlet environment
        Mockito.when(servletContext.getAttribute(
                JSR356WebsocketInitializer.getAttributeName(servletName)))
                .thenReturn(Mockito.mock(AtmosphereFramework.class));
        servlet.init(config);
        return servlet.getService();
    }

    public static Instantiator getInstantiator(ApplicationContext context)
            throws ServletException {
        Properties initParameters = new Properties();
        return getService(context, initParameters).getInstantiator();
    }

    @Test
    public void getOrCreateBean_noBeansGivenCannotInstantiate_throwsExceptionWithoutHint() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class,
                Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(context.getBeanNamesForType(Number.class))
                .thenReturn(new String[] {});
        Mockito.when(context.getAutowireCapableBeanFactory()
                .createBean(Number.class))
                .thenThrow(new BeanInstantiationException(Number.class,
                        "This is an abstract class"));
        SpringInstantiator instantiator = new SpringInstantiator(null, context);

        try {
            instantiator.getOrCreate(Number.class);
        } catch (BeanInstantiationException e) {
            Assert.assertNotNull(e.getMessage());
            Assert.assertFalse(e.getMessage().contains("[HINT]"));
            return;
        }
        Assert.fail();
    }

    @Test
    public void getOrCreateBean_multipleBeansGivenCannotInstantiate_throwsExceptionWithHint() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class,
                Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(context.getBeanNamesForType(Number.class))
                .thenReturn(new String[] { "one", "two" });
        Mockito.when(context.getAutowireCapableBeanFactory()
                .createBean(Number.class))
                .thenThrow(new BeanInstantiationException(Number.class,
                        "This is an abstract class"));
        SpringInstantiator instantiator = new SpringInstantiator(null, context);

        try {
            instantiator.getOrCreate(Number.class);
        } catch (BeanInstantiationException e) {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().contains("[HINT]"));
            return;
        }
        Assert.fail();
    }

    @Test
    public void getOrCreateBean_oneBeanGiven_noException() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.getBeanNamesForType(Number.class))
                .thenReturn(new String[] { "one" });
        Mockito.when(context.getBean(Number.class)).thenReturn(0);
        SpringInstantiator instantiator = new SpringInstantiator(null, context);

        Number bean = instantiator.getOrCreate(Number.class);

        Assert.assertEquals(0, bean);
    }

    @Test
    public void getOrCreateBean_multipleBeansGivenButCanInstantiate_noException() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class,
                Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(context.getBeanNamesForType(String.class))
                .thenReturn(new String[] { "one", "two" });
        Mockito.when(context.getAutowireCapableBeanFactory()
                .createBean(String.class)).thenReturn("string");
        SpringInstantiator instantiator = new SpringInstantiator(null, context);

        String bean = instantiator.getOrCreate(String.class);

        Assert.assertEquals("string", bean);
    }

    @Test
    public void getApplicationClass_regularClass_getsSameClass()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);
        RouteTarget1 instance = instantiator.getOrCreate(RouteTarget1.class);
        Assert.assertSame(RouteTarget1.class,
                instantiator.getApplicationClass(instance));
        Assert.assertSame(RouteTarget1.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    public void getApplicationClass_scopedBean_getsApplicationClass()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);
        RouteTarget2 instance = context.getBean(RouteTarget2.class);
        Assert.assertSame(RouteTarget2.class,
                instantiator.getApplicationClass(instance));
        Assert.assertSame(RouteTarget2.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

    @Test
    public void getApplicationClass_proxiedBean_getsApplicationClass()
            throws ServletException {
        Instantiator instantiator = getInstantiator(context);
        TestConfiguration instance = context.getBean(TestConfiguration.class);
        Assert.assertNotSame(TestConfiguration.class, instance.getClass());
        Assert.assertSame(TestConfiguration.class,
                instantiator.getApplicationClass(instance));
        Assert.assertSame(TestConfiguration.class,
                instantiator.getApplicationClass(instance.getClass()));
    }

}
