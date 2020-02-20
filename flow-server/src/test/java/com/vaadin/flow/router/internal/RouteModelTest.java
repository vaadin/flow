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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.UrlParameters;
import org.junit.Assert;
import org.junit.Test;

public class RouteModelTest {

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

    @Test
    public void route_model_with_various_urls() {

        RouteModel root = RouteModel.create();
        root.addRoute("", routeTarget(Root.class));
        root.addRoute("trunk", routeTarget(Trunk.class));
        root.addRoute("trunk/branch", routeTarget(Branch.class));
        root.addRoute("trunk/branch/:id(int)", routeTarget(Branch.class));
        root.addRoute("trunk/branch/:id(int)/:list*(long)",
                routeTarget(BranchChildren.class));
        root.addRoute("trunk/:name?/:type?/branch/:id?(int)/edit",
                routeTarget(BranchEdit.class));
        root.addRoute(
                "trunk/:name/:type?/branch/:id(int)/flower/:open(bool)/edit",
                routeTarget(FlowerEdit.class));
        root.addRoute("trunk/twig/:leafs*", routeTarget(Twig.class));

        NavigationRouteTarget result;
        String path;

        path = "trunk/twig";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, Twig.class, null);

        path = "trunk/twig/a/b/c";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, Twig.class,
                parameters("leafs", varargs("a", "b", "c")));

        path = "";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, Root.class, null);

        path = "trunk";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, Trunk.class, null);

        path = "trunk/branch";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, Branch.class, null);

        path = "trunk/branch/12";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, Branch.class, parameters("id", "12"));

        path = "trunk/branch/12/1/2/3/4/5/6/7";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, BranchChildren.class, parameters("id", "12",
                "list", varargs("1", "2", "3", "4", "5", "6", "7")));

        path = "trunk/branch/view";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, null, null);

        path = "trunk/branch/edit";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, BranchEdit.class, null);

        path = "trunk/red/branch/12/edit";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, BranchEdit.class,
                parameters("id", "12", "name", "red"));

        path = "trunk/branch/12/edit";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, BranchEdit.class, parameters("id", "12"));

        path = "trunk/red/birch/branch/12/edit";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, BranchEdit.class,
                parameters("id", "12", "name", "red", "type", "birch"));

        path = "trunk/red/branch/12/flower/true/edit";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, FlowerEdit.class,
                parameters("id", "12", "name", "red", "open", "true"));

        path = "trunk/red/branch/12/flower/edit";
        result = root.getNavigationRouteTarget(path);
        assertResult(result, path, null, null);

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
        NavigationRouteTarget result = root.getNavigationRouteTarget(path);
        assertResult(result, path, Root.class,
                parameters("vararg", varargs("1", "2", "3")));
    }

    private RouteTarget routeTarget(Class<? extends Component> target) {
        return new RouteTarget(target, null);
    }

    private void assertResult(NavigationRouteTarget result, String path,
            Class<? extends Component> target, UrlParameters urlParameters) {

        if (urlParameters == null) {
            urlParameters = new UrlParameters();
        }

        Assert.assertEquals("Invalid path", path, result.getUrl());

        Assert.assertEquals(
                "Weird expected target [" + target + "], actual result ["
                        + result + "]",
                target == null, result.getTarget() == null);

        if (target != null) {
            Assert.assertTrue(
                    "Invalid expected target [" + target + "], actual "
                            + result.getTarget().getRoutes(),
                    result.getTarget().containsTarget(target));
        }

        Assert.assertEquals("Invalid path", urlParameters,
                result.getUrlParameters());

    }

}
