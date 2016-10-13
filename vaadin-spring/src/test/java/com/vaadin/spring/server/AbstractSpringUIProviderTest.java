/*
 * Copyright 2015-2016 The original authors
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

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.server.UICreateEvent;
import com.vaadin.server.VaadinServletService;
import com.vaadin.spring.annotation.EnableVaadin;
import com.vaadin.spring.internal.UIScopeImpl;
import com.vaadin.spring.test.util.SingletonBeanStoreRetrievalStrategy;
import com.vaadin.spring.test.util.TestVaadinSession;
import com.vaadin.ui.UI;

@RunWith(SpringJUnit4ClassRunner.class)
// make sure the context is cleaned
@DirtiesContext
public abstract class AbstractSpringUIProviderTest {

    @Configuration
    @EnableVaadin
    protected static class Config {
    }

    protected static final int TEST_UIID = 123;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private HttpServletRequest request;

    private TestVaadinSession vaadinSession;
    private SpringUIProvider uiProvider;

    @Before
    public void setup() throws Exception {
        UIScopeImpl.setBeanStoreRetrievalStrategy(
                new SingletonBeanStoreRetrievalStrategy());

        vaadinSession = new TestVaadinSession(applicationContext);
        uiProvider = new SpringUIProvider(vaadinSession);
    }

    protected SpringUIProvider getUiProvider() {
        return uiProvider;
    }

    protected UICreateEvent buildUiCreateEvent(Class<? extends UI> uiClass) {
        return new UICreateEvent(new SpringVaadinServletRequest(request,
                (VaadinServletService) vaadinSession.getService(), false),
                uiClass, TEST_UIID);
    }

    @SuppressWarnings("unchecked")
    protected <T extends UI> T createUi(Class<T> uiClass) {
        return (T) getUiProvider().createInstance(buildUiCreateEvent(uiClass));
    }

}
