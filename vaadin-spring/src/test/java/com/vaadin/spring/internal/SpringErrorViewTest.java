/*
 * Copyright 2015-2017 The original authors
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
package com.vaadin.spring.internal;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.spring.server.AbstractSpringUIProviderTest;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

/**
 * Test SpringViewProvider.
 */
@ContextConfiguration
@WebAppConfiguration
public class SpringErrorViewTest extends AbstractSpringUIProviderTest {

    @SpringUI
    @SpringViewDisplay
    private static class TestUI1 extends DummyUI {

    }

    @SpringView(name = "errorViewScopeView")
    private static class TestErrorViewScopeView implements View {
        @Override
        public void enter(ViewChangeEvent event) {
        }
    }

    @SpringView(name = "errorViewScopeUI")
    @UIScope
    private static class TestErrorViewScopeUI implements View {
        @Override
        public void enter(ViewChangeEvent event) {
        }
    }

    @Configuration
    @EnableVaadinNavigation
    static class Config extends AbstractSpringUIProviderTest.Config {
        // this gets configured by the UI provider
        @Bean
        public TestUI1 ui1() {
            return new TestUI1();
        }

        @Bean
        @UIScope
        public TestErrorViewScopeUI errorViewScopeUI()
        {
            return new TestErrorViewScopeUI();
        }

        @Bean
        @ViewScope
        public TestErrorViewScopeView errorViewScopeView()
        {
            return new TestErrorViewScopeView();
        }
    }

    @Autowired
    private WebApplicationContext applicationContext;

    private TestUI1 ui;

    @Before
    public void setupUi() {
        // need a UI to set everything up
        ui = createUi(TestUI1.class);

        VaadinSession session = createVaadinSessionMock();
        CurrentInstance.set(VaadinSession.class, session);
        ui.setSession(session);
        UI.setCurrent(ui);
        // SpringViewProvider is UI scoped, so needs to be fetched after
        // createUi()
        applicationContext.getBean(SpringViewProvider.class);
    }

    @After
    public void teardownUi() {
        ui.setSession(null);
        UI.setCurrent(null);
        CurrentInstance.set(VaadinSession.class, null);
    }

    @Test(expected = BeanDefinitionValidationException.class)
    public void testViewScope() throws Exception {
        TestUI1 ui = createUi(TestUI1.class);
        UI.setCurrent(ui);
        getNavigator().setErrorView(TestErrorViewScopeView.class);
    }

    @Test
    public void testUiScope() throws Exception {
        TestUI1 ui = createUi(TestUI1.class);
        UI.setCurrent(ui);
        getNavigator().setErrorView(TestErrorViewScopeUI.class);
    }

    protected SpringNavigator getNavigator() {
        return (SpringNavigator) ui.getNavigator();
    }

}
