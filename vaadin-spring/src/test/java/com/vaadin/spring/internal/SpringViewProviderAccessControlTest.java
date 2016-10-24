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
package com.vaadin.spring.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.server.VaadinSession;
import com.vaadin.spring.access.ViewAccessControl;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.annotation.ViewContainer;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.spring.server.AbstractSpringUIProviderTest;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

/**
 * Test SpringViewProvider access control.
 */
@ContextConfiguration
@WebAppConfiguration
public class SpringViewProviderAccessControlTest
        extends AbstractSpringUIProviderTest {

    @SpringUI
    @ViewContainer
    private static class TestUI1 extends DummyUI {
    }

    @SpringUI(path = "other")
    private static class TestUI2 extends DummyUI {
    }

    @SpringView(name = TestView1.VIEW_NAME)
    private static class TestView1 extends DummyView {
        static final String BEAN_NAME = "view1";
        static final String VIEW_NAME = "";
    }

    @SpringView(name = TestView2.VIEW_NAME, ui = TestUI1.class)
    private static class TestView2 extends DummyView {
        static final String BEAN_NAME = "view2";
        static final String VIEW_NAME = "view2";
    }

    @SpringView(name = TestView3.VIEW_NAME, ui = TestUI1.class)
    private static class TestView3 extends DummyView {
        static final String BEAN_NAME = "view3";
        static final String VIEW_NAME = "view3";
    }

    @SpringView(name = TestOtherUiView.VIEW_NAME, ui = TestUI2.class)
    private static class TestOtherUiView extends DummyView {
        static final String VIEW_NAME = "otheruiview";
    }

    private static class MyAccessDeniedView extends DummyView {
    }

    protected static class MyViewAccessControl implements ViewAccessControl {
        public Set<String> allowedViewBeans = new HashSet<String>();

        @Override
        public boolean isAccessGranted(UI ui, String beanName) {
            return allowedViewBeans.contains(beanName);
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
        @UIScope
        public MyAccessDeniedView accessDeniedView() {
            return new MyAccessDeniedView();
        }

        @Bean
        @Scope("singleton")
        public MyViewAccessControl accessControl() {
            return new MyViewAccessControl();
        }
    }

    @Autowired
    private WebApplicationContext applicationContext;

    private TestUI1 ui;
    private SpringViewProvider viewProvider;

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
        viewProvider = applicationContext.getBean(SpringViewProvider.class);
    }

    @After
    public void teardownUi() {
        ui.setSession(null);
        UI.setCurrent(null);
        CurrentInstance.set(VaadinSession.class, null);
        getAccessControl().allowedViewBeans.clear();
    }

    @Test
    public void testAllowAllViews() throws Exception {
        allowViews(TestView1.BEAN_NAME, TestView2.BEAN_NAME,
                TestView3.BEAN_NAME);
        checkAvailableViews(TestView1.VIEW_NAME, TestView2.VIEW_NAME,
                TestView3.VIEW_NAME);
    }

    @Test
    public void testAllowSomeViews() throws Exception {
        allowViews(TestView1.BEAN_NAME, TestView2.BEAN_NAME);
        checkAvailableViews(TestView1.VIEW_NAME, TestView2.VIEW_NAME);
    }

    @Test
    public void testGetAllowedView() throws Exception {
        allowViews(TestView1.BEAN_NAME);
        Assert.assertTrue("Could not get allowed view",
                viewProvider.getView(TestView1.VIEW_NAME) instanceof TestView1);
    }

    @Test
    public void testGetDisallowedView() throws Exception {
        Assert.assertNull("Got disallowed view",
                viewProvider.getView(TestView1.VIEW_NAME));
    }

    @Test
    public void testGetDisallowedViewWithAccessDeniedView() throws Exception {
        viewProvider.setAccessDeniedViewClass(MyAccessDeniedView.class);
        Assert.assertTrue(
                "Got disallowed view when should get access denied view",
                viewProvider.getView(
                        TestView1.VIEW_NAME) instanceof MyAccessDeniedView);
    }

    // TODO add tests for interaction of error and access denied views

    private void allowViews(String... viewBeanNames) {
        MyViewAccessControl accessControl = getAccessControl();
        for (String viewBeanName : viewBeanNames) {
            accessControl.allowedViewBeans.add(viewBeanName);
        }
    }

    private MyViewAccessControl getAccessControl() {
        return applicationContext.getBean(MyViewAccessControl.class);
    }

    protected void checkAvailableViews(String... viewNames) {
        List<String> views = new ArrayList<String>(
                viewProvider.getViewNamesForCurrentUI());
        Collections.sort(views);
        List<String> expectedViews = new ArrayList<String>(
                Arrays.asList(viewNames));
        Collections.sort(expectedViews);
        Assert.assertEquals("Incorrect set of views returned", expectedViews,
                views);
    }
}
