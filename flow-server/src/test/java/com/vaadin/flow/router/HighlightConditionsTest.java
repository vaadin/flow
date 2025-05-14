/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.router;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class HighlightConditionsTest {

    @Test
    public void locationPrefix_defaultRoute_emptyLocationMatches() {
        HighlightCondition<RouterLink> condition = HighlightConditions
                .locationPrefix();
        RouterLink link = Mockito.mock(RouterLink.class);
        AfterNavigationEvent event = Mockito.mock(AfterNavigationEvent.class);

        Mockito.when(link.getHref()).thenReturn("");
        Location location = new Location("");
        Mockito.when(event.getLocation()).thenReturn(location);
        Assert.assertTrue(condition.shouldHighlight(link, event));
    }

    @Test
    public void locationPrefix_defaultRoute_nonEmptyLocationDoesNotMatch() {
        HighlightCondition<RouterLink> condition = HighlightConditions
                .locationPrefix();
        RouterLink link = Mockito.mock(RouterLink.class);
        AfterNavigationEvent event = Mockito.mock(AfterNavigationEvent.class);

        Mockito.when(link.getHref()).thenReturn("");
        Location location = new Location("foo");
        Mockito.when(event.getLocation()).thenReturn(location);
        Assert.assertFalse(condition.shouldHighlight(link, event));
    }

    @Test
    public void locationPrefix_notDefaultRoute_prefixMatches() {
        HighlightCondition<RouterLink> condition = HighlightConditions
                .locationPrefix();
        RouterLink link = Mockito.mock(RouterLink.class);
        AfterNavigationEvent event = Mockito.mock(AfterNavigationEvent.class);

        Mockito.when(link.getHref()).thenReturn("foo");
        Location location = new Location("foobar");
        Mockito.when(event.getLocation()).thenReturn(location);
        Assert.assertTrue(condition.shouldHighlight(link, event));
    }

    @Test
    public void locationPrefix_notDefaultRoute_nonPrefixDoesNotMatch() {
        HighlightCondition<RouterLink> condition = HighlightConditions
                .locationPrefix();
        RouterLink link = Mockito.mock(RouterLink.class);
        AfterNavigationEvent event = Mockito.mock(AfterNavigationEvent.class);

        Mockito.when(link.getHref()).thenReturn("foo");
        Location location = new Location("bar");
        Mockito.when(event.getLocation()).thenReturn(location);
        Assert.assertFalse(condition.shouldHighlight(link, event));
    }
}
