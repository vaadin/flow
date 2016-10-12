/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.server;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.VaadinConfiguration;
import com.vaadin.ui.UI;

@RunWith(SpringJUnit4ClassRunner.class)
// make sure the context is cleaned
@DirtiesContext
public abstract class AbstractSpringUIProviderTest {

    @Configuration
    protected static class Config extends VaadinConfiguration {
        @Bean
        public VaadinServlet vaadinServlet() {
            return new SpringVaadinServlet();
        }
    }

    private final class MySpringVaadinServletService
            extends SpringVaadinServletService {
        private MySpringVaadinServletService(VaadinServlet servlet)
                throws ServiceException {
            super(servlet, new DefaultDeploymentConfiguration(
                    MySpringVaadinServletService.class, new Properties()), "");
            init();
        }

        @Override
        public VaadinSession createVaadinSession(VaadinRequest request)
                throws com.vaadin.server.ServiceException {
            return super.createVaadinSession(request);
        }

    }

    protected static final int TEST_UIID = 123;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private MockServletContext servletContext;

    @Autowired
    private MockHttpServletRequest request;

    @Autowired
    private SpringVaadinServlet servlet;

    private MySpringVaadinServletService service;
    private SpringVaadinServletRequest vaadinServletRequest;
    private VaadinSession vaadinSession;
    private SpringUIProvider uiProvider;

    @Before
    public void setup() throws Exception {
        // need to circumvent a lot of normal mechanisms as many relevant
        // methods are private
        // TODO very ugly - can this be simplified?
        servlet.init(new MockServletConfig(servletContext));
        setService(new MySpringVaadinServletService(servlet));
        setVaadinServletRequest(
                new SpringVaadinServletRequest(request, getService(), true));
        setVaadinSession(
                getService().createVaadinSession(getVaadinServletRequest()));
        VaadinSession.setCurrent(vaadinSession);

        uiProvider = new SpringUIProvider(getVaadinSession());
    }

    @After
    public void tearDown() {
        VaadinSession.setCurrent(null);
    }

    protected SpringUIProvider getUiProvider() {
        return uiProvider;
    }

    protected UICreateEvent buildUiCreateEvent(Class<? extends UI> uiClass) {
        return new UICreateEvent(getVaadinServletRequest(), uiClass, TEST_UIID);
    }

    @SuppressWarnings("unchecked")
    protected <T extends UI> T createUi(Class<T> uiClass) {
        return (T) getUiProvider().createInstance(buildUiCreateEvent(uiClass));
    }

    public MySpringVaadinServletService getService() {
        return service;
    }

    private void setService(MySpringVaadinServletService service) {
        this.service = service;
    }

    public SpringVaadinServletRequest getVaadinServletRequest() {
        return vaadinServletRequest;
    }

    private void setVaadinServletRequest(
            SpringVaadinServletRequest vaadinServletRequest) {
        this.vaadinServletRequest = vaadinServletRequest;
    }

    public VaadinSession getVaadinSession() {
        return vaadinSession;
    }

    private void setVaadinSession(VaadinSession vaadinSession) {
        this.vaadinSession = vaadinSession;
    }

}
