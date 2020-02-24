/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.server.startup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.ConfiguredRoutes;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.router.internal.TestAbstractRouteRegistry;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RouteTargetTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Route("")
    @Tag(Tag.DIV)
    public static class NormalRoute extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class OptionalRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter
                        String parameter) {
        }
    }

    /* Mutable tests */
    @Test
    public void adding_route_to_immutable_target_throws() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tried to mutate immutable configuration.");

        RouteTarget target = new RouteTarget(OptionalRoute.class, false);
        target.addRoute(NormalRoute.class);
    }

    /* Remove target */
    @Test
    public void removing_from_immutable_throws() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tried to mutate immutable configuration.");

        RouteTarget target = new RouteTarget(OptionalRoute.class, false);
        target.remove(OptionalRoute.class);
    }

    /* Parent layouts */
    @Test
    public void setParentLayouts_throws_for_immutable() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tried to mutate immutable configuration.");

        RouteTarget target = new RouteTarget(NormalRoute.class, false);
        target.setParentLayouts(NormalRoute.class, Collections.emptyList());
    }

    @Test
    public void setParentLayouts_throws_for_non_registered_target_class() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(
                "Tried to add parent layouts for a non existing target "
                        + OptionalRoute.class.getName());

        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.setParentLayouts(OptionalRoute.class, Collections.emptyList());
    }

}
