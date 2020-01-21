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

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
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

    @Test
    public void test() {
        RouteSegment root = RouteSegment.createRoot();
        root.addPath("", Root.class);
        root.addPath("trunk", Trunk.class);
        root.addPath("trunk/branch", Branch.class);

        Optional<RouteSegment.RouteResult> route = root.findRoute("");
        System.out.println("route: " + route);

        route = root.findRoute("trunk");
        System.out.println("route: " + route);

        route = root.findRoute("trunk/branch");
        System.out.println("route: " + route);
    }

}
