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
package com.vaadin.flow.spring.instantiator;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.spring.SpringInstantiator;
import com.vaadin.flow.spring.SpringServlet;

@RunWith(SpringRunner.class)
@Import(SpringInstantiatorTest.TestConfiguration.class)
public class SpringInstantiatorTest {

    @Autowired
    private ApplicationContext context;

    @Configuration
    @ComponentScan
    public static class TestConfiguration {

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

    public static VaadinServletService getService(ApplicationContext context,
            Properties configProperties) throws ServletException {
        SpringServlet servlet = new SpringServlet(context, false) {
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

        ServletConfig config = Mockito.mock(ServletConfig.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        Mockito.when(config.getServletContext()).thenReturn(servletContext);

        Mockito.when(config.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
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
}
