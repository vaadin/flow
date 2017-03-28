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

import java.util.Collection;

import com.vaadin.spring.annotation.SpringViewDisplay;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.spring.server.AbstractSpringUIProviderTest;
import com.vaadin.spring.test.util.TestSpringNavigator;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

/**
 * Test SpringViewProvider.
 */
@ContextConfiguration
@WebAppConfiguration
@TestPropertySource(properties = "view.name.key=view4")
public class SpringViewProviderTest extends AbstractSpringUIProviderTest {

    @SpringUI
    @SpringViewDisplay
    private static class TestUI1 extends DummyUI {
    }

    @SpringUI(path = "other")
    // TODO @SpringViewDisplay
    private static class TestUI2 extends UI {
        @Override
        protected void init(VaadinRequest request) {
        }
    }

    @SpringView(name = "")
    private static class TestView1 implements View {
        @Override
        public void enter(ViewChangeEvent event) {
        }
    }

    @SpringView(name = "view2", ui = TestUI1.class)
    private static class TestView2 implements View {
        @Override
        public void enter(ViewChangeEvent event) {
        }
    }

    @SpringView(name = "view3", ui = TestUI2.class)
    private static class TestView3 implements View {
        @Override
        public void enter(ViewChangeEvent event) {
        }
    }

    @SpringView(name = "${view.name.key}", ui = TestUI1.class)
    private static class TestView4 implements View {
        @Override
        public void enter(ViewChangeEvent event) {
        }
    }
    
    @SpringView(name = "${undefined.view.name.key:default}", ui = TestUI1.class)
    private static class TestView5 implements View {
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
        public TestUI2 ui2() {
            return new TestUI2();
        }

        // in a real application, these are created dynamically

        @Bean
        @ViewScope
        public TestView1 view1() {
            return new TestView1();
        }

        @Bean
        @ViewScope
        public TestView2 view2() {
            return new TestView2();
        }

        @Bean
        @ViewScope
        public TestView3 view3() {
            return new TestView3();
        }

        @Bean
        @ViewScope
        public TestView4 view4() {
            return new TestView4();
        }

        @Bean
        @ViewScope
        public TestView5 view5() {
        	return new TestView5();
        }

        @Bean
        @UIScope
        public SpringNavigator vaadinNavigator() {
            return new TestSpringNavigator();
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

    @Test
    public void testListViewsForUI1() throws Exception {
        // need a UI to set everything up
        TestUI1 ui = createUi(TestUI1.class);
        UI.setCurrent(ui);
        // SpringViewProvider is UI scoped, so needs to be fetched after
        // createUi()
        SpringViewProvider viewProvider = applicationContext
                .getBean(SpringViewProvider.class);
        Collection<String> views = viewProvider.getViewNamesForCurrentUI();
        Assert.assertTrue("Wrong number of views returned", 4 == views.size());
        Assert.assertTrue("Root view not returned by SpringViewProvider",
                views.contains(""));
        Assert.assertTrue("Root view not returned by SpringViewProvider",
                views.contains("view2"));
        Assert.assertTrue("Root view not returned by SpringViewProvider",
        		views.contains("view4"));
        Assert.assertTrue("Root view not returned by SpringViewProvider",
        		views.contains("default"));
        UI.setCurrent(null);
    }

    @Test
    public void navigateToSameView() throws Exception {
        View view1 = getView("");
        View view1b = getView("");
        Assert.assertNotSame("Expected new view instance on re-navigation",
                view1, view1b);
    }

    protected View getView(String viewName) {
        // use the navigator instead of the view provider to also get the error
        // view
        getNavigator().navigateTo(viewName);
        return getNavigator().getCurrentView();
    }

    protected SpringNavigator getNavigator() {
        return (SpringNavigator) ui.getNavigator();
    }

}
