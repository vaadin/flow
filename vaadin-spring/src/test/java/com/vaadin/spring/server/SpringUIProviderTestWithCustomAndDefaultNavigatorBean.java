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
package com.vaadin.spring.server;

import org.junit.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.spring.navigator.SpringNavigator;

/**
 * Test SpringUIProvider for the case where the application has a custom
 * navigator bean as well as the default navigation configuration enabled.
 */
@ContextConfiguration
@WebAppConfiguration
public class SpringUIProviderTestWithCustomAndDefaultNavigatorBean
        extends AbstractSpringUIProviderTest {

    @SpringUI
    @SpringViewDisplay
    private static class TestUI extends DummyUI {
    }

    private static class MyNavigator extends SpringNavigator {
    }

    @Configuration
    @EnableVaadinNavigation
    static class Config extends AbstractSpringUIProviderTest.Config {
        // Vaadin Spring Boot has another layer of autoconfiguration that is
        // tested separately. With plain Vaadin Spring, no auto-configuration is
        // active by default, but explicitly defining a Navigator bean will
        // cause a conflict.
        @Bean
        @UIScope
        public MyNavigator myNavigator() {
            return new MyNavigator();
        }

        // this gets configured by the UI provider
        @Bean
        public TestUI ui() {
            return new TestUI();
        }
    }

    @Test(expected = NoUniqueBeanDefinitionException.class)
    public void testGetNavigator() throws Exception {
        // need a UI for the scope of the Navigator
        TestUI ui = createUi(TestUI.class);
        ui.getNavigator();
    }

}
