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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import org.junit.Assert;
import org.junit.Test;

public class RouteSegmentTest {

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
    public static class BranchEdit extends Component {
    }

    @Tag(Tag.DIV)
    public static class FlowerEdit extends Component {
    }

    @Test
    public void test() {

        RouteSegment root = RouteSegment.createRoot();
        root.addPath("", Root.class);
        root.addPath("trunk", Trunk.class);
        root.addPath("trunk/branch/:id<int>", Branch.class);
        root.addPath("trunk/:[name]/:[type]/branch/:[id<int>]/edit",
                BranchEdit.class);
        root.addPath(
                "trunk/:name/:[type]/branch/:id<int>/flower/:open<bool>/edit",
                FlowerEdit.class);

        RouteSegment.RouteSearchResult result;
        String path;

        path = "";
        result = root.getRoute(path);
        assertResult(result, path, Root.class, null);

        path = "trunk";
        result = root.getRoute(path);
        assertResult(result, path, Trunk.class, null);

        path = "trunk/branch";
        result = root.getRoute(path);
        assertResult(result, path, null, null);

        path = "trunk/branch/12";
        result = root.getRoute(path);
        assertResult(result, path, Branch.class, parameters("id", "12"));

        path = "trunk/branch/view";
        result = root.getRoute(path);
        assertResult(result, path, null, null);

        path = "trunk/branch/edit";
        result = root.getRoute(path);
        assertResult(result, path, BranchEdit.class, null);

        path = "trunk/red/branch/12/edit";
        result = root.getRoute(path);
        assertResult(result, path, BranchEdit.class,
                parameters("id", "12", "name", "red"));

        path = "trunk/branch/12/edit";
        result = root.getRoute(path);
        assertResult(result, path, BranchEdit.class, parameters("id", "12"));

        path = "trunk/red/birch/branch/12/edit";
        result = root.getRoute(path);
        assertResult(result, path, BranchEdit.class,
                parameters("id", "12", "name", "red", "type", "birch"));

        path = "trunk/red/branch/12/flower/true/edit";
        result = root.getRoute(path);
        assertResult(result, path, FlowerEdit.class,
                parameters("id", "12", "name", "red", "open", "true"));

        path = "trunk/red/branch/12/flower/edit";
        result = root.getRoute(path);
        assertResult(result, path, null, null);

    }

    private Map<String, String> parameters(String... keysAndValues) {
        Map<String, String> result = new HashMap<>(keysAndValues.length / 2);

        for (int i = 0; i < keysAndValues.length; i++) {
            result.put(keysAndValues[i], keysAndValues[++i]);
        }

        return result;
    }

    private void assertResult(RouteSegment.RouteSearchResult result,
            String path, Class<? extends Component> target,
            Map<String, String> urlParameters) {

        System.out.println("result: " + result);

        if (urlParameters == null) {
            urlParameters = Collections.emptyMap();
        }

        Assert.assertEquals("Invalid path", path, result.getPath());

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
