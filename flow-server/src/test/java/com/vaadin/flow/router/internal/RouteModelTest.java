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

package com.vaadin.flow.router.internal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.RouteParameterData;
import com.vaadin.flow.router.RouteParameterFormat;
import com.vaadin.flow.router.RouteParameterRegex;
import com.vaadin.flow.router.UrlParameters;
import com.vaadin.flow.server.startup.RouteTarget;
import org.junit.Assert;
import org.junit.Test;

public class RouteModelTest {

    private RouteModel getRouteModel() {
        RouteModel root = RouteModel.create();
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
                + RouteParameterRegex.BOOLEAN + ")/edit",
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

        assertNavigation(root, "trunk/red/branch/12/flower/true/edit",
                FlowerEdit.class,
                parameters("id", "12", "name", "red", "open", "true"));

        assertNavigation(root, "trunk/red/branch/12/flower/edit", null, null);

    }

    @Test
    public void varargs_url_parameter_defined_only_as_last_segment() {
        RouteModel root = RouteModel.create();
        try {
            root.addRoute("trunk/:vararg*/edit", routeTarget(Root.class));

            Assert.fail(
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
        final String urlTemplate = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")";
        final UrlParameters urlParameters = parameters("id", "12");

        assertUrl(root, expectedUrl, urlTemplate, urlParameters);

        root.removeRoute(urlTemplate);

        try {
            assertUrl(root, expectedUrl, urlTemplate, urlParameters);
            Assert.fail("Route was just removed.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void route_model_provides_route_target() {
        RouteModel root = getRouteModel();

        final String urlTemplate = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")";
        final UrlParameters urlParameters = parameters("id", "12");

        assertRoute(root, Branch.class, urlTemplate, urlParameters);

        root.removeRoute(urlTemplate);

        try {
            root.getRouteTarget(urlTemplate, urlParameters);
            Assert.fail("Route was just removed.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void route_model_provides_url_template_format() {
        RouteModel root = getRouteModel();

        final String urlTemplate = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")/:list*("
                + RouteParameterRegex.LONG + ")";

        Assert.assertEquals(urlTemplate,
                root.formatUrlTemplate(urlTemplate,
                        EnumSet.of(RouteParameterFormat.NAME,
                                RouteParameterFormat.REGEX,
                                RouteParameterFormat.MODIFIER)));

        Assert.assertEquals("trunk/branch/:id(integer)/:list*(long)",
                root.formatUrlTemplate(urlTemplate,
                        EnumSet.of(RouteParameterFormat.NAME,
                                RouteParameterFormat.REGEX_NAME,
                                RouteParameterFormat.MODIFIER)));

        Assert.assertEquals("trunk/branch/:id(integer)/:list(long)",
                root.formatUrlTemplate(urlTemplate,
                        EnumSet.of(RouteParameterFormat.NAME,
                                RouteParameterFormat.REGEX_NAME)));

        Assert.assertEquals("trunk/branch/:id/:list*",
                root.formatUrlTemplate(urlTemplate,
                        EnumSet.of(RouteParameterFormat.NAME,
                                RouteParameterFormat.MODIFIER)));

        Assert.assertEquals("trunk/branch/:integer/:long",
                root.formatUrlTemplate(urlTemplate,
                        EnumSet.of(RouteParameterFormat.REGEX_NAME)));
    }

    @Test
    public void route_model_provides_parameters() {
        RouteModel root = getRouteModel();

        final String urlTemplate = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")/:list*("
                + RouteParameterRegex.LONG + ")";

        final Map<String, RouteParameterData> parameters = root
                .getParameters(urlTemplate);

        Assert.assertEquals("Incorrect parameters size", 2, parameters.size());

        Assert.assertTrue("Missing parameter", parameters.containsKey("id"));
        Assert.assertTrue("Missing parameter", parameters.containsKey("list"));

        Assert.assertEquals("Wrong parameter data",
                ":id(" + RouteParameterRegex.INTEGER + ")",
                parameters.get("id").getTemplate());
        Assert.assertEquals("Wrong parameter data",
                ":list*(" + RouteParameterRegex.LONG + ")",
                parameters.get("list").getTemplate());
    }

    @Test
    public void route_model_provides_routes() {
        RouteModel root = getRouteModel();

        final Map<String, RouteTarget> routes = root.getRoutes();

        Assert.assertEquals("Incorrect routes size", 8, routes.size());

        final String urlTemplate = "trunk/branch/:id("
                + RouteParameterRegex.INTEGER + ")/:list*("
                + RouteParameterRegex.LONG + ")";

        Assert.assertEquals("Wrong route mapping", BranchChildren.class,
                routes.get(urlTemplate).getTarget());
    }

    private void assertUrl(RouteModel root, String expectedUrl,
            String urlTemplate, UrlParameters parameters) {
        final String modelUrl = root.getUrl(urlTemplate, parameters);
        Assert.assertEquals(expectedUrl, modelUrl);
    }

    private void assertNavigation(RouteModel model, String url,
            Class<? extends Component> target, UrlParameters urlParameters) {

        NavigationRouteTarget result = model.getNavigationRouteTarget(url);

        Assert.assertEquals("Invalid url", url, result.getUrl());

        final RouteTarget routeTarget = result.getRouteTarget();
        assertTarget(target, routeTarget);

        if (target != null) {
            Assert.assertEquals("Invalid url", urlParameters,
                    result.getUrlParameters());
        }
    }

    private void assertRoute(RouteModel model,
            Class<? extends Component> target, String urlTemplate,
            UrlParameters urlParameters) {
        assertTarget(target, model.getRouteTarget(urlTemplate, urlParameters));
    }

    private void assertTarget(Class<? extends Component> target,
            RouteTarget routeTarget) {
        Assert.assertEquals("Weird expected target [" + target + "], actual ["
                + routeTarget + "]", target == null, routeTarget == null);

        if (target != null) {
            Assert.assertTrue(
                    "Invalid expected target [" + target + "], actual "
                            + routeTarget.getTarget(),
                    routeTarget.getTarget().equals(target));
        }
    }

    private RouteTarget routeTarget(Class<? extends Component> target) {
        return new RouteTarget(target, null);
    }

    /**
     * Creates a parameters map where any even index argument is a key (starting
     * with 0) and any odd index argument is a value (starting with 1)
     *
     * @param keysAndValues
     *            the keys and values of the map.
     * @return a Map containing the specified arguments.
     */
    public static UrlParameters parameters(String... keysAndValues) {
        return new UrlParameters(keysAndValues);
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
