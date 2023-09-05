/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;
import jakarta.servlet.ServletException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RunWith(SpringRunner.class)
@Import(TestServletConfiguration.class)
public class SpringVaadinServletServiceTest {

    @Autowired
    private ApplicationContext context;

    @Component
    public static class TestInstantiator implements Instantiator {

        @Autowired
        private ApplicationContext context;

        @Override
        public Stream<VaadinServiceInitListener> getServiceInitListeners() {
            return context.getBeansOfType(VaadinServiceInitListener.class)
                    .values().stream();
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

    @Test
    public void getInstantiator_springManagedBean_instantiatorBeanReturned()
            throws ServletException {
        VaadinService service = SpringInstantiatorTest.getService(context,
                null);

        Instantiator instantiator = service.getInstantiator();

        Assert.assertEquals(TestInstantiator.class, instantiator.getClass());
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

    @Test
    public void requestInterceptorsAreRegisteredOnTheService()
            throws ServletException {
        VaadinService service = SpringInstantiatorTest.getService(context,
                new Properties());

        List<VaadinRequestInterceptor> interceptors = StreamSupport
                .stream(service.getVaadinRequestInterceptors().spliterator(),
                        false)
                .toList();
        Assertions.assertEquals(1, interceptors.size(),
                "There should be 1 filter");
        Assertions.assertInstanceOf(
                TestServletConfiguration.MyRequestInterceptor.class,
                interceptors.get(0), "MyFilter should be registered");
    }

}
