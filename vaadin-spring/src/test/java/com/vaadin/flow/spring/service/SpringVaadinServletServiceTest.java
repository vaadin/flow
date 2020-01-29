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
package com.vaadin.flow.spring.service;

import java.util.Properties;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;

@RunWith(SpringRunner.class)
@Import(SpringVaadinServletServiceTest.TestServletConfiguration.class)
public class SpringVaadinServletServiceTest {

    private static final String FOO = "foo";

    private static final String BAR = "bar";

    private static final Properties BASE_PROPERTIES = new Properties();

    @Autowired
    private ApplicationContext context;

    @Configuration
    @ComponentScan
    public static class TestServletConfiguration {

    }

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

}
