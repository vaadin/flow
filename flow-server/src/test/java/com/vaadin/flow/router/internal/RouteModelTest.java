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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.RouteParameterData;
import com.vaadin.flow.router.RouteParameterFormatOption;
import com.vaadin.flow.router.RouteParameterRegex;
import com.vaadin.flow.router.RouteParameters;

public class RouteModelTest {

    private RouteModel getRouteModel() {
        RouteModel root = RouteModel.create(true);
        root.addRoute("", routeTarget(Root.class));
        root.addRoute("trunk", routeTarget(Trunk.class));
        root.addRoute("trunk/branch", routeTarget(Branch.class));
        root.addRoute("trunk/branch/:id(" + RouteParameterRegex.INTEGER + ")",
                routeTarget(Branch.class));
        root.addRoute(
                "trunk/branch/:id(" + RouteParameterRegex.INTEGER + ")/:list*("
                        + RouteParameterRegex.LONG + ")",
                routeTarget(BranchChildren.class));
        root.addRoute("trunk/:name?/:type?/branch/:id?("
                + RouteParameterRegex.INTEGER + ")/edit",
                routeTarget(BranchEdit.class));
        root.addRoute("trunk/:name/:type?/branch/:id("
                + RouteParameterRegex.INTEGER + ")/flower/:open("
                + RouteParameterRegex.LONG + ")/edit",
                routeTarget(FlowerEdit.class));
        root.addRoute("trunk/twig/:leafs*", routeTarget(Twig.class));
        return root;
    }

    @Test
    public void route_model_provides_navigation_route_target() {

        RouteModel root = getRouteModel();

        assertNavigation(root, "trunk/twig", Twig.class, parameters());

        assertNavigation(root, "trunk/twig/a/b/c", Twig.class,
                parameters("leafs", varargs("a", "b", "c")));

        assertNavigation(root, "", Root.class, parameters());

        assertNavigation(root, "trunk", Trunk.class, parameters());

        assertNavigation(root, "trunk/branch", Branch.class, parameters());

        assertNavigation(root, "trunk/branch/12", Branch.class,
                parameters("id", "12"));

        assertNavigation(root, "trunk/branch/12/1/2/3/4/5/6/7",
                BranchChildren.class, parameters("id", "12", "list",
                        varargs("1", "2", "3", "4", "5", "6", "7")));

        assertNavigation(root, "trunk/branch/view", null, null);

        assertNavigation(root, "trunk/branch/edit", BranchEdit.class,
                parameters());

        assertNavigation(root, "trunk/red/branch/12/edit", BranchEdit.class,
                parameters("id", "12", "name", "red"));

        assertNavigation(root, "trunk/branch/12/edit", BranchEdit.class,
                parameters("id", "12"));

        assertNavigation(root, "trunk/red/birch/branch/12/edit",
                BranchEdit.class,
                parameters("id", "12", "name", "red", "type", "birch"));

        assertNavigation(root, "trunk/red/branch/12/flower/1234567890/edit",
                FlowerEdit.class,
                parameters("id", "12", "name", "red", "open", "1234567890"));

        assertNavigation(root, "trunk/red/branch/12/flower/edit", null, null);

    }

    @Test
    public void varargs_url_parameter_defined_only_as_last_segment() {
        RouteModel root = RouteModel.create(true);
        try {
            root.addRoute("trunk/:vararg*/edit", routeTarget(Root.class));

            Assertions.fail(
                    "Varargs url parameter accepted in the middle of the path.");
        } catch (IllegalArgumentException e) {
        }

        root.addRoute("trunk/edit/:vararg*", routeTarget(Root.class));

        String path = "trunk/edit/1/2/3";
        assertNavigation(root, path, Root.class,
                parameters("vararg", varargs("1", "2", "3")));
    }

    @Test
    public void remove_route_target_not_found() {
        RouteModel root = getRouteModel();

        assertNavigation(root, "trunk/branch/12", Branch.class,
                parameters("id", "12"));

        root.removeRoute(
                "trunk/branch/:id(" + RouteParameterRegex.INTEGER + ")");

        assertNavigation(root, "trunk/branch/12", BranchChildren.class,
                parameters("id", "12"));

        root.removeRoute("trunk/branch/:id(" + RouteParameterRegex.INTEGER
                + ")/:list*(" + RouteParameterRegex.LONG + ")");

        assertNavigation(root, "trunk/branch/12", null, null);
    }

    @Test
    public void remove_route_url_not_found() {
        RouteModel root = getRouteModel();

        final String expectedUrl = "trunk/branch/12";
        final String template = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")";
        final RouteParameters parameters = parameters("id", "12");

        assertUrl(root, expectedUrl, template, parameters);

        root.removeRoute(template);

        try {
            assertUrl(root, expectedUrl, template, parameters);
            Assertions.fail("Route was just removed.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void route_model_provides_route_target() {
        RouteModel root = getRouteModel();

        final String template = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")";
        final RouteParameters parameters = parameters("id", "12");

        assertRoute(root, Branch.class, template, parameters);

        root.removeRoute(template);

        try {
            root.getRouteTarget(template, parameters);
            Assertions.fail("Route was just removed.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void route_model_provides_url_template_format() {
        RouteModel root = getRouteModel();

        final String template = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")/:list*("
                + RouteParameterRegex.LONG + ")";

        Assertions.assertEquals(template,
                root.formatTemplate(template,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.REGEX,
                                RouteParameterFormatOption.MODIFIER)));

        Assertions.assertEquals("trunk/branch/:id(integer)/:list*(long)",
                root.formatTemplate(template,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.REGEX_NAME,
                                RouteParameterFormatOption.MODIFIER)));

        Assertions.assertEquals("trunk/branch/:id(integer)/:list(long)",
                root.formatTemplate(template,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.REGEX_NAME)));

        Assertions.assertEquals("trunk/branch/:id/:list*",
                root.formatTemplate(template,
                        EnumSet.of(RouteParameterFormatOption.NAME,
                                RouteParameterFormatOption.MODIFIER)));

        Assertions.assertEquals("trunk/branch/:integer/:long",
                root.formatTemplate(template,
                        EnumSet.of(RouteParameterFormatOption.REGEX_NAME)));
    }

    @Test
    public void route_model_provides_parameters() {
        RouteModel root = getRouteModel();

        final String template = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")/:list*("
                + RouteParameterRegex.LONG + ")";

        final Map<String, RouteParameterData> parameters = root
                .getParameters(template);

        Assertions.assertEquals(2, parameters.size(),
                "Incorrect parameters size");

        Assertions.assertTrue(parameters.containsKey("id"),
                "Missing parameter");
        Assertions.assertTrue(parameters.containsKey("list"),
                "Missing parameter");

        Assertions.assertEquals(":id(" + RouteParameterRegex.INTEGER + ")",
                parameters.get("id").getTemplate(), "Wrong parameter data");
        Assertions.assertEquals(":list*(" + RouteParameterRegex.LONG + ")",
                parameters.get("list").getTemplate(), "Wrong parameter data");
    }

    @Test
    public void route_model_provides_routes() {
        RouteModel root = getRouteModel();

        final Map<String, RouteTarget> routes = root.getRoutes();

        Assertions.assertEquals(8, routes.size(), "Incorrect routes size");

        final String template = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")/:list*("
                + RouteParameterRegex.LONG + ")";

        Assertions.assertEquals(BranchChildren.class,
                routes.get(template).getTarget(), "Wrong route mapping");
    }

    @Test
    public void mutable_methods_throw_when_model_is_immutable() {
        RouteModel immutable = RouteModel.create(false);
        try {
            immutable.addRoute("foo/:foo", routeTarget(Root.class));

            Assertions.fail("Immutable model should not be mutable.");
        } catch (IllegalStateException e) {
        }

        RouteModel mutable = RouteModel.create(true);
        mutable.addRoute("foo/:foo", routeTarget(Root.class));

        RouteModel immutableCopy = RouteModel.copy(mutable, false);

        try {
            immutableCopy.removeRoute("foo/:foo");

            Assertions.fail("Immutable model should not be mutable.");
        } catch (IllegalStateException e) {
        }
    }

    private void assertUrl(RouteModel root, String expectedUrl, String template,
            RouteParameters parameters) {
        final String modelUrl = root.getUrl(template, parameters);
        Assertions.assertEquals(expectedUrl, modelUrl);
    }

    private void assertNavigation(RouteModel model, String url,
            Class<? extends Component> target, RouteParameters parameters) {

        NavigationRouteTarget result = model.getNavigationRouteTarget(url);

        Assertions.assertEquals(url, result.getPath(), "Invalid url");

        final RouteTarget routeTarget = result.getRouteTarget();
        assertTarget(target, routeTarget);

        if (target != null) {
            Assertions.assertEquals(parameters, result.getRouteParameters(),
                    "Invalid url");
        }
    }

    private void assertRoute(RouteModel model,
            Class<? extends Component> target, String template,
            RouteParameters parameters) {
        assertTarget(target, model.getRouteTarget(template, parameters));
    }

    private void assertTarget(Class<? extends Component> target,
            RouteTarget routeTarget) {
        Assertions.assertTrue((target == null) == (routeTarget == null),
                "Weird expected target [" + target + "], actual [" + routeTarget
                        + "]");

        if (target != null) {
            Assertions.assertTrue(routeTarget.getTarget().equals(target),
                    "Invalid expected target [" + target + "], actual "
                            + routeTarget.getTarget());
        }
    }

    private RouteTarget routeTarget(Class<? extends Component> target) {
        return new RouteTarget(target, null);
    }

    /**
     * Creates a parameters map where any even index argument is a key (starting
     * with 0) and any odd index argument is a value (starting with 1)
     *
     * @param namesAndValues
     *            the keys and values of the map.
     * @return a Map containing the specified arguments.
     */
    public static RouteParameters parameters(String... namesAndValues) {
        if (namesAndValues.length % 2 == 1) {
            throw new IllegalArgumentException(
                    "Input varargs must be of even size.");
        }

        Map<String, String> paramsMap = new HashMap<>(
                namesAndValues.length / 2);

        for (int i = 0; i < namesAndValues.length; i += 2) {
            final String name = namesAndValues[i];
            if (paramsMap.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Parameter " + name + " is specified more than once.");
            }

            final String value = namesAndValues[i + 1];
            paramsMap.put(name, value);
        }

        return new RouteParameters(paramsMap);
    }

    /**
     * Creates a List out of the specified arguments.
     *
     * @param varargs
     *            an array of strings.
     * @return a List containing the specified arguments.
     */
    public static String varargs(String... varargs) {
        return PathUtil.getPath(Arrays.asList(varargs));
    }

    @Tag(Tag.DIV)
    public static class Root extends Component {
    }

    @Tag(Tag.DIV)
    public static class Trunk extends Component {
    }

    @Tag(Tag.DIV)
    public static class Branch extends Component {
    }

    @Tag(Tag.DIV)
    public static class BranchChildren extends Component {
    }

    @Tag(Tag.DIV)
    public static class Twig extends Component {
    }

    @Tag(Tag.DIV)
    public static class BranchEdit extends Component {
    }

    @Tag(Tag.DIV)
    public static class FlowerEdit extends Component {
    }

}
