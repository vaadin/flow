/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;

class ConfigureRoutesTest {

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

    @Test
    public void mutableConfiguration_canSetRouteTarget() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        Assertions.assertTrue(mutable.hasTemplate(""),
                "Configuration should have \"\" route registered");
        Assertions.assertEquals(BaseTarget.class, mutable.getTarget("").get(),
                "Configuration should have registered base target.");
    }

    @Test
    public void mutableConfiguration_canSetTargetRoute() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        Assertions.assertTrue(mutable.hasRouteTarget(BaseTarget.class), "");

        Assertions.assertEquals("", mutable.getTemplate(BaseTarget.class),
                "Configuration should have registered base target.");
    }

    @Test
    public void mutableConfigurationClear_removesRegisteredRoutes() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        assertSetRoutes(mutable);

        mutable.clear();

        Assertions.assertFalse(mutable.hasRouteTarget(BaseTarget.class));
        Assertions.assertFalse(mutable.hasRouteTarget(ParamTarget.class));

        Assertions.assertNull(
                mutable.getNavigationRouteTarget("").getRouteTarget());
        Assertions.assertNull(
                mutable.getNavigationRouteTarget("123").getRouteTarget());

        assertSetRoutes(mutable);
    }

    @Test
    public void mutableConfigurationClear_preservesErrorRoute() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        mutable.clear();

        Assertions.assertEquals(BaseError.class,
                mutable.getExceptionHandlerByClass(
                        IndexOutOfBoundsException.class),
                "ErrorRoute shouldn't be cleared.");
    }

    @Test
    public void duplicateRootPathRegistration_throwsException() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);
        Assertions.assertTrue(mutable.hasRouteTarget(BaseTarget.class));
        Assertions.assertEquals(BaseTarget.class, mutable
                .getNavigationRouteTarget("").getRouteTarget().getTarget());

        AmbiguousRouteConfigurationException ex = Assertions.assertThrows(
                AmbiguousRouteConfigurationException.class, () -> {
                    mutable.setRoute("", BaseTarget.class);
                });
        Assertions.assertTrue(ex.getMessage().contains(String.format(
                RouteUtil.ROUTE_CONFLICT,
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$BaseTarget",
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$BaseTarget")));
    }

    @Test
    public void duplicateParameterPathRegistration_throwsException() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute(":param", ParamTarget.class);
        Assertions.assertTrue(mutable.hasRouteTarget(ParamTarget.class));
        Assertions.assertEquals(ParamTarget.class, mutable
                .getNavigationRouteTarget("123").getRouteTarget().getTarget());

        AmbiguousRouteConfigurationException ex = Assertions.assertThrows(
                AmbiguousRouteConfigurationException.class, () -> {
                    mutable.setRoute(":param", ParamTarget.class);
                });
        Assertions.assertTrue(ex.getMessage().contains(String.format(
                RouteUtil.ROUTE_CONFLICT_WITH_PARAMS,
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$ParamTarget",
                "com.vaadin.flow.router.internal.ConfigureRoutesTest$ParamTarget")));
    }

    @Test
    public void mutableConfiguration_makingImmutableHasCorrectData() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        ConfiguredRoutes immutable = new ConfiguredRoutes(mutable);

        Assertions.assertTrue(immutable.hasTemplate(""),
                "Configuration should have \"\" route registered");
        Assertions.assertEquals(BaseTarget.class, immutable.getTarget("").get(),
                "Configuration should have registered base target.");

        Assertions.assertTrue(immutable.hasRouteTarget(BaseTarget.class), "");
        Assertions.assertEquals("", immutable.getTemplate(BaseTarget.class),
                "Configuration should have registered base target.");
    }

    @Test
    public void mutableConfiguration_canSetErrorTargets() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        Assertions.assertFalse(mutable.getExceptionHandlers().isEmpty(),
                "Exception targets should be available");
        Assertions.assertEquals(BaseError.class,
                mutable.getExceptionHandlerByClass(
                        IndexOutOfBoundsException.class),
                "Given exception returned unexpected handler class");
    }

    @Test
    public void populatedMutableConfiguration_clearRemovesAllContent() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class);

        mutable.setErrorRoute(IndexOutOfBoundsException.class, BaseError.class);

        Assertions.assertFalse(mutable.getRoutes().isEmpty(),
                "Configuration should have routes.");
        Assertions.assertFalse(mutable.getExceptionHandlers().isEmpty(),
                "Configuration should have exceptions.");

        mutable.clear();

        Assertions.assertTrue(mutable.getRoutes().isEmpty(),
                "After clear all routes should have been removed.");
        Assertions.assertTrue(mutable.getTargetRoutes().isEmpty(),
                "After clear all targetRoutes should have been removed. ");
        Assertions.assertFalse(mutable.getExceptionHandlers().isEmpty(),
                "After clear  exception targets should still be available.");
    }

    private void assertSetRoutes(ConfigureRoutes mutable) {
        mutable.setRoute("", BaseTarget.class);
        mutable.setRoute(":param", ParamTarget.class);

        Assertions.assertTrue(mutable.hasRouteTarget(BaseTarget.class));
        Assertions.assertTrue(mutable.hasRouteTarget(ParamTarget.class));

        Assertions.assertEquals(BaseTarget.class, mutable
                .getNavigationRouteTarget("").getRouteTarget().getTarget());
        Assertions.assertEquals(ParamTarget.class, mutable
                .getNavigationRouteTarget("123").getRouteTarget().getTarget());
    }

}
