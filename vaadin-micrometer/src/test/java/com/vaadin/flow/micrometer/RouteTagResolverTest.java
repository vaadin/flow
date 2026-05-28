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
package com.vaadin.flow.micrometer;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;

public class RouteTagResolverTest {

    private static final class FakeRouteA extends Component {
    }

    private static final class FakeRouteB extends Component {
    }

    private static final class FakeRouteC extends Component {
    }

    @Test
    public void nullTargetIsUnknown() {
        RouteTagResolver resolver = new RouteTagResolver(10);
        Assert.assertEquals(MeterNames.ROUTE_UNKNOWN, resolver.tagFor(null));
    }

    @Test
    public void firstRoutesAreAdmittedThenBucketedAsOther() {
        RouteTagResolver resolver = new RouteTagResolver(2);

        String a = resolver.tagFor(FakeRouteA.class);
        String b = resolver.tagFor(FakeRouteB.class);
        String c = resolver.tagFor(FakeRouteC.class);

        Assert.assertEquals(FakeRouteA.class.getSimpleName(), a);
        Assert.assertEquals(FakeRouteB.class.getSimpleName(), b);
        Assert.assertEquals(MeterNames.ROUTE_OTHER, c);
    }

    @Test
    public void admittedRouteRemainsAdmittedAfterCapHit() {
        RouteTagResolver resolver = new RouteTagResolver(1);
        resolver.tagFor(FakeRouteA.class);
        resolver.tagFor(FakeRouteB.class); // overflow -> _other

        Assert.assertEquals(FakeRouteA.class.getSimpleName(),
                resolver.tagFor(FakeRouteA.class));
    }
}
