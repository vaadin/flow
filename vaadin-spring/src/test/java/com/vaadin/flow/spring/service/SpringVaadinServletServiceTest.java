/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.service;

import javax.servlet.ServletException;

import java.util.Properties;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

@RunWith(SpringRunner.class)
@Import(TestServletConfiguration.class)
public class SpringVaadinServletServiceTest {

    private static final String FOO = "foo";

    private static final String BAR = "bar";

    private static final Properties BASE_PROPERTIES = new Properties();

    @Autowired
    private ApplicationContext context;

    @Component
    public static class TestInstantiator implements Instantiator {

        @Override
        public boolean init(VaadinService service) {
            return Boolean.TRUE.toString()
                    .equals(service.getDeploymentConfiguration()
                            .getInitParameters().getProperty(FOO));
        }

        @Override
        public Stream<VaadinServiceInitListener> getServiceInitListeners() {
            return Stream.of();
        }

        @Override
        public <T> T getOrCreate(Class<T> type) {
            return null;
        }

        @Override
        public <T extends com.vaadin.flow.component.Component> T createComponent(
                Class<T> componentClass) {
            return null;
        }
    }

    @Component
    public static class NonUniqueInstantiator extends TestInstantiator {

        @Override
        public boolean init(VaadinService service) {
            return BAR.equals(service.getDeploymentConfiguration()
                    .getInitParameters().getProperty(FOO));
        }
    }

    @Test
    public void getInstantiator_springManagedBean_instantiatorBeanReturned()
            throws ServletException {
        Properties properties = new Properties(BASE_PROPERTIES);
        properties.setProperty(FOO, Boolean.TRUE.toString());
        VaadinService service = SpringInstantiatorTest.getService(context,
                properties);

        Instantiator instantiator = service.getInstantiator();

        Assert.assertEquals(TestInstantiator.class, instantiator.getClass());
    }

    @Test
    public void getInstantiator_javaSPIClass_instantiatorPojoReturned()
            throws ServletException {
        Properties properties = new Properties(BASE_PROPERTIES);
        properties.setProperty(FOO, Boolean.FALSE.toString());
        VaadinService service = SpringInstantiatorTest.getService(context,
                properties);

        Instantiator instantiator = service.getInstantiator();

        Assert.assertEquals(JavaSPIInstantiator.class, instantiator.getClass());
    }

    @Test(expected = ServletException.class)
    public void getInstantiator_nonUnique_exceptionIsThrown()
            throws ServletException {
        Properties properties = new Properties(BASE_PROPERTIES);
        properties.setProperty(FOO, BAR);
        SpringInstantiatorTest.getService(context, properties);
    }

    @Test
    public void uiInitListenerAsSpringBean_listenerIsAutoregisteredAsUIInitiLietnerInSpringService()
            throws ServletException, ServiceException {
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());

        UI ui = new UI();
        TestUIInitListener listener = context.getBean(TestUIInitListener.class);

        service.fireUIInitListeners(ui);

        Assert.assertEquals(1, listener.events.size());
        Assert.assertSame(ui, listener.events.get(0).getUI());
    }

}
