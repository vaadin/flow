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

import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;

/**
 * Test for normal (full) use cases of SpringUIProvider with automatic
 * navigation configuration on the view but no view display defined.
 */
@ContextConfiguration
@WebAppConfiguration
public class SpringUIProviderTestWithoutSpringViewDisplay
        extends AbstractSpringUIProviderTest {

    @SpringUI
    private static class TestUI extends DummyUI {
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
    public void testConfigureNavigator() {
        TestUI ui = createUi(TestUI.class);
        Assert.isNull(ui.getNavigator(),
                "Navigator is configured even though there is no SpringViewDisplay");
    }

    @Test
    public void testFindSpringViewDisplay() throws Exception {
        TestUI ui = createUi(TestUI.class);
        Assert.isNull(getUiProvider().findSpringViewDisplay(ui),
                "View display is not a Panel");
    }

}
