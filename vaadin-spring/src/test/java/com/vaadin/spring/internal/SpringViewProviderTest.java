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

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.ViewContainer;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.spring.server.AbstractSpringUIProviderTest;
import com.vaadin.ui.UI;

/**
 * Test SpringViewProvider.
 */
@ContextConfiguration
@WebAppConfiguration
public class SpringViewProviderTest extends AbstractSpringUIProviderTest {

    @SpringUI
    @ViewContainer
    private static class TestUI1 extends DummyUI {
    }

    @SpringUI(path = "other")
    // TODO @ViewContainer
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

    }

    @Autowired
    private WebApplicationContext applicationContext;

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
        Assert.isTrue(2 == views.size(), "Wrong number of views returned");
        Assert.isTrue(views.contains(""),
                "Root view not returned by SpringViewProvider");
        Assert.isTrue(views.contains("view2"),
                "Root view not returned by SpringViewProvider");
        UI.setCurrent(null);
    }

}
