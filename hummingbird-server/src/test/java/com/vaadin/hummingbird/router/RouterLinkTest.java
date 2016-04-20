/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.hummingbird.router;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.router.RouterTest.RouterTestUI;
import com.vaadin.hummingbird.router.ViewRendererTest.TestView;
import com.vaadin.shared.ApplicationConstants;

public class RouterLinkTest {
    @Test
    public void buildUrlWithoutParameters() {
        String url = RouterLink.buildUrl("foo/bar");

        Assert.assertEquals("foo/bar", url);
    }

    @Test
    public void buildUrlWithParameters() {
        String url = RouterLink.buildUrl("{foo}/bar/*", "param1",
                "param2/param3");

        Assert.assertEquals("param1/bar/param2/param3", url);
    }

    @Test
    public void bildUrlWithEmptyWildcard() {
        String url = RouterLink.buildUrl("{foo}/bar/*", "param1", "");
        Assert.assertEquals("param1/bar/", url);
    }

    @Test
    public void bildUrlWithOmittedWildcard() {
        String url = RouterLink.buildUrl("{foo}/bar/*", "param1");
        Assert.assertEquals("param1/bar/", url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithTooFewParameters() {
        RouterLink.buildUrl("{foo}/bar/{baz}", "param1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithTooManyParameters() {
        RouterLink.buildUrl("{foo}/bar/*", "param1", "param2", "param3");
    }

    @Test
    public void buildUrlWithRouter() {
        Router router = new Router();
        router.reconfigure(c -> c.setRoute("foo/{bar}", TestView.class));

        String url = RouterLink.buildUrl(router, TestView.class, "asdf");

        Assert.assertEquals("foo/asdf", url);
    }

    @Test
    public void buildEmptyUrlWithRouter() {
        Router router = new Router();
        router.reconfigure(c -> c.setRoute("", TestView.class));

        String url = RouterLink.buildUrl(router, TestView.class);

        Assert.assertEquals("", url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithRouter_noRoutes() {
        Router router = new Router();

        RouterLink.buildUrl(router, TestView.class, "asdf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithRouter_multipleRoutes() {
        Router router = new Router();
        router.reconfigure(c -> {
            c.setRoute("foo/{bar}", TestView.class);
            c.setRoute("another/route", TestView.class);
        });

        RouterLink.buildUrl(router, TestView.class, "asdf");
    }

    @Test
    public void createRouterLink() {
        RouterTestUI ui = new RouterTestUI();
        ui.getRouter().get()
                .reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");
        Assert.assertEquals("Show something", link.getText());
        Assert.assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        Assert.assertFalse(link.getElement().hasAttribute("href"));

        ui.add(link);

        Assert.assertEquals("show/something",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void createReconfigureRouterLink() {
        RouterTestUI ui = new RouterTestUI();
        ui.getRouter().get()
                .reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");

        link.setRoute(TestView.class, "other");

        ui.add(link);

        Assert.assertEquals("show/other",
                link.getElement().getAttribute("href"));

        link.setRoute(TestView.class, "changed");

        Assert.assertEquals("show/changed",
                link.getElement().getAttribute("href"));
    }

}
