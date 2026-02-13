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

import java.util.Arrays;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameterFormatOption;
import com.vaadin.flow.router.RouteParameterRegex;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredRoutesTest {

    @Test
    public void emptyConfiguration_allGetMethodsWork() {
        ConfiguredRoutes configuration = new ConfiguredRoutes();

        assertFalse(configuration.hasTemplate(""),
                "No routes should be configured");
        assertFalse(configuration.getTarget("").isPresent(),
                "No routes should be configured");
        assertTrue(configuration.getRoutes().isEmpty(),
                "Configuration should be empty");
        assertTrue(configuration.getTargetRoutes().isEmpty(),
                "Configuration should be empty");
        assertNull(
                configuration
                        .getExceptionHandlerByClass(RuntimeException.class),
                "No exception handler should be found.");
        assertNull(configuration.getTemplate(BaseTarget.class),
                "No target route should be found");
        assertTrue(configuration.getExceptionHandlers().isEmpty(),
                "Configuration should be empty");
        assertFalse(configuration.hasRouteTarget(BaseTarget.class),
                "No route should be found");
    }

    @Test
    public void mutableConfiguration_makingImmutableHasCorrectData() {
        ConfigureRoutes mutable = new ConfigureRoutes();

        mutable.setRoute("", BaseTarget.class,
                Arrays.asList(SecondParentTarget.class, ParentTarget.class));

        ConfiguredRoutes immutable = new ConfiguredRoutes(mutable);

        assertTrue(immutable.hasTemplate(""),
                "Configuration should have \"\" route registered");
        assertEquals(BaseTarget.class, immutable.getTarget("").get(),
                "Configuration should have registered base target.");

        assertTrue(immutable.hasRouteTarget(BaseTarget.class),
                "BaseTarget registration should have been copied over");
        assertEquals("", immutable.getTemplate(BaseTarget.class),
                "Configuration should have registered base target.");

        assertEquals(
                Arrays.asList(SecondParentTarget.class, ParentTarget.class),
                immutable.getNavigationRouteTarget("").getRouteTarget()
                        .getParentLayouts(),
                "Given parentLayouts should have been copied correctly");
    }

    @Test
    public void configuration_provides_target_url() {
        ConfigureRoutes edit = new ConfigureRoutes();
        edit.setRoute("foo/:foo", FooTarget.class);
        edit.setRoute("foo/:foo(qwe)", FooTarget.class);
        edit.setRoute("foo/:foo/bar?(asd)", FooTarget.class);
        edit.setRoute(":foo/:bar?(asd)", FooTarget.class);
        edit.setRoute("foo/:foo/:bar*(asd)", FooTarget.class);
        edit.setRoute("foo/:foo(qwe)/:bar*(asd)", FooTarget.class);
        edit.setRoute("foo/foobar?/:foo(qwe)/:bar*(asd)", FooTarget.class);
        edit.setRoute("foo/foobar?/:foo(qwe)", FooTarget.class);
        edit.setRoute("foo/foobar?/:foo", FooTarget.class);

        edit.setRoute("bar/:bar?", BarTarget.class);
        edit.setRoute("bar/:bar?/foobar", BarTarget.class);
        edit.setRoute("bar/:bar?(qwe)", BarTarget.class);
        edit.setRoute("bar/:bar?(qwe)/:foo*(asd)", BarTarget.class);
        edit.setRoute("bar/:bar?/:foo*", BarTarget.class);
        edit.setRoute(":bar?/:foo*", BarTarget.class);
        edit.setRoute(":bar?/foobar/:foo*", BarTarget.class);

        ConfiguredRoutes config = new ConfiguredRoutes(edit);

        assertNull(config.getTargetUrl(FooTarget.class));
        assertEquals("bar", config.getTargetUrl(BarTarget.class));

        // Make sure all routes are passed.
        config.getRouteModel().getRoutes().entrySet()
                .forEach(stringRouteTargetEntry -> {
                    final boolean requiredParameter = RouteFormat
                            .hasRequiredParameter(
                                    stringRouteTargetEntry.getKey());

                    assertEquals(
                            stringRouteTargetEntry.getValue().getTarget()
                                    .equals(FooTarget.class),
                            requiredParameter);
                });
    }

    @Test
    public void configuration_provides_formatted_url_template() {
        ConfigureRoutes config = new ConfigureRoutes();

        final String template = "/path/to" + "/:intType("
                + RouteParameterRegex.INTEGER + ")" + "/:longType?("
                + RouteParameterRegex.LONG + ")"
                + "/:stringType?/:varargs*(thinking|of|U|and|I)";
        config.setRoute(template, BaseTarget.class);

        assertFalse(template.equals(config.getTemplate(BaseTarget.class)),
                "Template should not contain prefixed forward slash '/'");

        assertEquals(template.substring(1),
                config.getTemplate(BaseTarget.class), "Invalid template");

        assertEquals(
                "path/to/:intType(integer)/:longType?(long)/:stringType?(string)/:varargs*(string)",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.MODIFIER,
                                RouteParameterFormatOption.REGEX_NAME)),
                "Invalid formatted template");

        assertEquals("path/to/:intType/:longType?/:stringType?/:varargs*",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.MODIFIER)),
                "Invalid formatted template");

        assertEquals("path/to/:intType/:longType/:stringType/:varargs",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.NAME)),
                "Invalid formatted template");

        assertEquals("path/to/:(integer)/:?(long)/:?(string)/:*(string)",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.MODIFIER,
                                RouteParameterFormatOption.REGEX_NAME)),
                "Invalid formatted template");

        assertEquals("path/to/:(" + RouteParameterRegex.INTEGER + ")/:?("
                + RouteParameterRegex.LONG + ")/:?/:*(thinking|of|U|and|I)",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.MODIFIER,
                                RouteParameterFormatOption.REGEX)),
                "Invalid formatted template");

        assertEquals("path/to/:integer/:long/:string/:string",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.REGEX_NAME)),
                "Invalid formatted template");

        assertEquals("path/to/:/:?/:?/:*",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.MODIFIER)),
                "Invalid formatted template");

        assertEquals(
                "path/to/:" + RouteParameterRegex.INTEGER + "/:"
                        + RouteParameterRegex.LONG + "/:/:thinking|of|U|and|I",
                config.getTemplate(BaseTarget.class,
                        EnumSet.of(RouteParameterFormatOption.REGEX)),
                "Invalid formatted template");
    }

    @Test
    public void configuration_provides_formatted_url_for_route_not_routeAlias() {
        ConfigureRoutes config = new ConfigureRoutes();

        config.setRoute(RouteTarget.class.getAnnotation(Route.class).value(),
                RouteTarget.class);
        config.setRoute("", RouteTarget.class);

        String targetUrl = config.getTargetUrl(RouteTarget.class,
                new RouteParameters(new RouteParam("message", "hello")));

        assertEquals("home/hello", targetUrl,
                "Route should be matched and not RouteAlias");
    }

    @Tag("div")
    @Route("/home/:message?")
    public static class RouteTarget extends Component {
    }

    @Tag("div")
    public static class FooTarget extends Component {
    }

    @Tag("div")
    public static class BarTarget extends Component {
    }

    @Tag("div")
    public static class BaseTarget extends Component {
    }

    @Tag("div")
    public static class ParentTarget extends Component implements RouterLayout {
    }

    @Tag("div")
    public static class SecondParentTarget extends Component
            implements RouterLayout {
    }
}
