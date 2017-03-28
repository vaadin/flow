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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;

/**
 * Test for resolution of the SpringUI path, Theme and Title annotations in
 * SpringUIProvider.
 */
@ContextConfiguration
@WebAppConfiguration
public class SpringUIProviderTestAnnotationPropertyResolution
        extends AbstractSpringUIProviderTest {

    @SpringUI(path = "${vaadin.ui.path:mypath}")
    @Theme("${vaadin.theme:mytheme}")
    @Title("${vaadin.page.title:My Application}")
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
    public void testPath() throws Exception {
        Assert.assertEquals("Properties in UI path are not correctly resolved",
                "mypath", getUiProvider().deriveMappingForUI("ui"));
    }

    @Test
    public void testTheme() throws Exception {
        String theme = getUiProvider()
                .getTheme(buildUiCreateEvent(TestUI.class));
        Assert.assertEquals(
                "Properties in theme name are not correctly resolved",
                "mytheme", theme);
    }

    @Test
    public void testPageTitle() throws Exception {
        String title = getUiProvider()
                .getPageTitle(buildUiCreateEvent(TestUI.class));
        Assert.assertEquals(
                "Properties in page title are not correctly resolved",
                "My Application", title);
    }

}
