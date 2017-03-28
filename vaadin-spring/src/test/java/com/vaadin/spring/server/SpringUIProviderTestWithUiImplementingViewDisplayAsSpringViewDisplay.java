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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.Assert;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringViewDisplay;

/**
 * Test for normal (full) use cases of SpringUIProvider with automatic
 * navigation configuration on the view and the UI implementing ViewDisplay.
 */
@ContextConfiguration
@WebAppConfiguration
public class SpringUIProviderTestWithUiImplementingViewDisplayAsSpringViewDisplay
        extends AbstractSpringUIProviderTest {

    @SpringUI
    @SpringViewDisplay
    private static class TestUI extends DummyUI implements ViewDisplay {
        @Override
        public void showView(View view) {
        }
    }

    @Configuration
    @EnableVaadinNavigation
    static class Config extends AbstractSpringUIProviderTest.Config {
        // this gets configured by the UI provider
        @Bean
        public TestUI ui() {
            return new TestUI();
        }
    }

    @Test
    public void testGetNavigator() throws Exception {
        // need a UI for the scope of the Navigator
        TestUI ui = createUi(TestUI.class);
        Assert.notNull(ui.getNavigator(),
                "Navigator not available from SpringUIProvider");
    }

    @Test
    public void testConfigureNavigator() {
        TestUI ui = createUi(TestUI.class);
        Assert.isTrue(ui.getNavigator().getDisplay() instanceof TestUI,
                "Navigator is not configured for a custom ViewDisplay");
    }

    @Test
    public void testFindSpringViewDisplay() throws Exception {
        TestUI ui = createUi(TestUI.class);
        Assert.isInstanceOf(TestUI.class, getUiProvider().findSpringViewDisplay(ui),
                "View display is not a TestUI");
    }

}
