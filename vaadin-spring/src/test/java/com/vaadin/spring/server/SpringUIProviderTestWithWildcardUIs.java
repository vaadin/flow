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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.navigator.PushStateNavigation;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.spring.annotation.EnableVaadinNavigation;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.UI;

@ContextConfiguration
@WebAppConfiguration
public class SpringUIProviderTestWithWildcardUIs
        extends AbstractSpringUIProviderTest {

    @SpringUI
    private static class Root extends DummyUI {
    }

    @SpringUI(path = "sub/*")
    private static class Sub extends DummyUI {
    }

    @SpringUI(path = "wild/**")
    private static class Wildcard extends DummyUI {
    }

    @SpringUI(path = "pushState")
    @PushStateNavigation
    private static class PushState extends DummyUI {
    }

    @SpringUI(path = "pushState/sub")
    @PushStateNavigation
    private static class PushStateSub extends DummyUI {
    }

    @Configuration
    @EnableVaadinNavigation
    static class Config extends AbstractSpringUIProviderTest.Config {
        @Bean
        public Root root() {
            return new Root();
        }

        @Bean
        public Sub sub() {
            return new Sub();
        }

        @Bean
        public Wildcard wildcard() {
            return new Wildcard();
        }

        @Bean
        public PushState pushState() {
            return new PushState();
        }

        @Bean
        public PushStateSub pushStateSub() {
            return new PushStateSub();
        }
    }

    @Test
    public void testRootUI() {
        verifyUIFromPath(Root.class, "");
        verifyUIFromPath(Root.class, "/");
    }

    @Test
    public void testSubUI() {
        verifyUIFromPath(Sub.class, "/sub");
        verifyUIFromPath(Sub.class, "/sub/");
        verifyUIFromPath(Sub.class, "/sub/foo");
    }

    @Test
    public void testWildcardUI() {
        verifyUIFromPath(Wildcard.class, "/wild");
        verifyUIFromPath(Wildcard.class, "/wild/");
        verifyUIFromPath(Wildcard.class, "/wild/foo");
        verifyUIFromPath(Wildcard.class, "/wild/foo/bar");
        verifyUIFromPath(Wildcard.class, "/wild/foo/bar/baz");
    }

    @Test
    public void testPushStateUI() {
        verifyUIFromPath(PushState.class, "/pushState");
        verifyUIFromPath(PushState.class, "/pushState/");
        verifyUIFromPath(PushState.class, "/pushState/foo");
        verifyUIFromPath(PushState.class, "/pushState/foo/bar");
        verifyUIFromPath(PushState.class, "/pushState/foo/bar/baz");
    }

    @Test
    public void testPushStateSubUI() {
        verifyUIFromPath(PushStateSub.class, "/pushState/sub");
        verifyUIFromPath(PushStateSub.class, "/pushState/sub/");
        verifyUIFromPath(PushStateSub.class, "/pushState/sub/foo");
        verifyUIFromPath(PushStateSub.class, "/pushState/sub/foo/bar");
        verifyUIFromPath(PushStateSub.class, "/pushState/sub/foo/bar/baz");
    }

    private void verifyUIFromPath(Class<? extends UI> cls, String path) {
        VaadinRequest request = mockRequest();
        when(request.getPathInfo()).thenReturn(path);
        assertEquals(cls.getCanonicalName(),
                getUiProvider().getUIClass(new UIClassSelectionEvent(request))
                        .getCanonicalName());
    }

    private VaadinRequest mockRequest() {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getService()).thenReturn(mock(VaadinService.class));
        return request;
    }
}
