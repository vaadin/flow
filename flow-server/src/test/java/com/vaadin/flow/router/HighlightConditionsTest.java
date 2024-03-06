/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
