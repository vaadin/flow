/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.router.internal;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;

public class ConfigureRoutesTest {

    @Tag("div")
    public static class BaseTarget extends Component {
    }

    @Tag("div")
    public static class ParamTarget extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Tag("div")
    public static class BaseError extends Component
            implements HasErrorParameter<IndexOutOfBoundsException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<IndexOutOfBoundsException> parameter) {
            return 0;
        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void mutableConfiguration_canSetRouteTarget() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        Assert.assertTrue("Configuration should have \"\" route registered",
                mutable.hasTemplate(""));
        Assert.assertEquals("Configuration should have registered base target.",
                BaseTarget.class, mutable.getTarget("").get());
    }

    @Test
    public void mutableConfiguration_canSetTargetRoute() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        Assert.assertTrue("", mutable.hasRouteTarget(BaseTarget.class));

        Assert.assertEquals("Configuration should have registered base target.",
                "", mutable.getTemplate(BaseTarget.class));
    }

    @Test
    public void mutableConfigurationClear_removesRegisteredRoutes() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        assertSetRoutes(mutable);

        mutable.clear();

        Assert.assertFalse(mutable.hasRouteTarget(BaseTarget.class));
        Assert.assertFalse(mutable.hasRouteTarget(ParamTarget.class));

        Assert.assertNull(
                mutable.getNavigationRouteTarget("").getRouteTarget());
        Assert.assertNull(
                mutable.getNavigationRouteTarget("123").getRouteTarget());

        assertSetRoutes(mutable);
    }

    @Test
    public void mutableConfigurationClear_preservesErrorRoute() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        mutable.clear();

        Assert.assertEquals("ErrorRoute shouldn't be cleared.", BaseError.class,
                mutable.getExceptionHandlerByClass(
                        IndexOutOfBoundsException.class));
    }

    @Test
    public void duplicateRootPathRegistration_throwsException() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);
        Assert.assertTrue(mutable.hasRouteTarget(BaseTarget.class));
        Assert.assertEquals(BaseTarget.class, mutable
                .getNavigationRouteTarget("").getRouteTarget().getTarget());

        exceptionRule.expect(AmbiguousRouteConfigurationException.class);
        exceptionRule.reportMissingExceptionWithMessage(
                "Duplicate routes shouldn't be accepted.");
        exceptionRule.expectMessage(String.format(RouteUtil.ROUTE_CONFLICT,
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$BaseTarget",
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$BaseTarget"));
        mutable.setRoute("", BaseTarget.class);
    }

    @Test
    public void duplicateParameterPathRegistration_throwsException() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute(":param", ParamTarget.class);
        Assert.assertTrue(mutable.hasRouteTarget(ParamTarget.class));
        Assert.assertEquals(ParamTarget.class, mutable
                .getNavigationRouteTarget("123").getRouteTarget().getTarget());

        exceptionRule.expect(AmbiguousRouteConfigurationException.class);
        exceptionRule.reportMissingExceptionWithMessage(
                "Duplicate parameter routes shouldn't be accepted.");
        exceptionRule.expectMessage(String.format(
                RouteUtil.ROUTE_CONFLICT_WITH_PARAMS,
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$ParamTarget",
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$ParamTarget"));
        mutable.setRoute(":param", ParamTarget.class);
    }

    @Test
    public void mutableConfiguration_makingImmutableHasCorrectData() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        ConfiguredRoutes immutable = new ConfiguredRoutes(mutable);

        Assert.assertTrue("Configuration should have \"\" route registered",
                immutable.hasTemplate(""));
        Assert.assertEquals("Configuration should have registered base target.",
                BaseTarget.class, immutable.getTarget("").get());

        Assert.assertTrue("", immutable.hasRouteTarget(BaseTarget.class));
        Assert.assertEquals("Configuration should have registered base target.",
                "", immutable.getTemplate(BaseTarget.class));
    }

    @Test
    public void mutableConfiguration_canSetErrorTargets() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        Assert.assertFalse("Exception targets should be available",
                mutable.getExceptionHandlers().isEmpty());
        Assert.assertEquals("Given exception returned unexpected handler class",
                BaseError.class, mutable.getExceptionHandlerByClass(
                        IndexOutOfBoundsException.class));
    }

    @Test
    public void populatedMutableConfiguration_clearRemovesAllContent() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        Assert.assertFalse("Configuration should have routes.",
                mutable.getRoutes().isEmpty());
        Assert.assertFalse("Configuration should have exceptions.",
                mutable.getExceptionHandlers().isEmpty());

        mutable.clear();

        Assert.assertTrue("After clear all routes should have been removed.",
                mutable.getRoutes().isEmpty());
        Assert.assertTrue(
                "After clear all targetRoutes should have been removed. ",
                mutable.getTargetRoutes().isEmpty());
        Assert.assertFalse(
                "After clear  exception targets should still be available.",
                mutable.getExceptionHandlers().isEmpty());
    }

    private void assertSetRoutes(ConfigureRoutes mutable) {
        mutable.setRoute("", BaseTarget.class);
        mutable.setRoute(":param", ParamTarget.class);

        Assert.assertTrue(mutable.hasRouteTarget(BaseTarget.class));
        Assert.assertTrue(mutable.hasRouteTarget(ParamTarget.class));

        Assert.assertEquals(BaseTarget.class, mutable
                .getNavigationRouteTarget("").getRouteTarget().getTarget());
        Assert.assertEquals(ParamTarget.class, mutable
                .getNavigationRouteTarget("123").getRouteTarget().getTarget());
    }

}
