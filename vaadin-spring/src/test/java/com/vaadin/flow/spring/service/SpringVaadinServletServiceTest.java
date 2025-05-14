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
package com.vaadin.flow.spring.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.spring.instantiator.SpringInstantiatorTest;
import jakarta.servlet.ServletException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Properties;
import java.util.stream.Stream;

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
            throws ServletException, ServiceException {
        VaadinServletService service = (VaadinServletService) SpringInstantiatorTest
                .getService(context, new Properties());
        VaadinServletRequest request = new VaadinServletRequest(
                new MockHttpServletRequest(), service);

        try {
            service.handleRequest(request, new VaadinServletResponse(
                    new MockHttpServletResponse(), service));
        } catch (Exception e) {
            Assert.assertTrue(
                    "Exception must be related to missing frontend folder",
                    e.getMessage().contains("Unable to find index.html"));
        }

        Assert.assertEquals("Interceptor got called on start", "true",
                request.getAttribute("started"));
        Assert.assertEquals("Interceptor got called on error", "true",
                request.getAttribute("error"));
        Assert.assertEquals("Interceptor got called on stop", "true",
                request.getAttribute("stopped"));
    }

}
