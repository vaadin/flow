/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.router;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.EasyMock;
import org.junit.Test;

import com.vaadin.router.RouteLocation.RouteSegmentVisitor;

public class RouteLocationTest {

    @Test
    public void testWalkRoute() {
        RouteLocation location = new RouteLocation(new Location("foo/{bar}/*"));

        AtomicInteger calls = new AtomicInteger();
        location.visitSegments(new RouteSegmentVisitor() {
            @Override
            public void acceptSegment(String segmentName) {
                assertEquals("foo", segmentName);
                assertEquals(1, calls.incrementAndGet());
            }

            @Override
            public void acceptPlaceholder(String placeholderName) {
                assertEquals("bar", placeholderName);
                assertEquals(2, calls.incrementAndGet());
            }

            @Override
            public void acceptWildcard() {
                assertEquals(3, calls.incrementAndGet());
            }
        });

        assertEquals(3, calls.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void walkRouteWithInvalidWildcard() {
        RouteLocation location = new RouteLocation(new Location("foo/*/*"));

        RouteSegmentVisitor visitor = EasyMock
                .createMock(RouteSegmentVisitor.class);

        location.visitSegments(visitor);
    }
}
