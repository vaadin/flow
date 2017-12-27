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
package com.vaadin.flow.router.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.legacy.LocationChangeEvent;
import com.vaadin.flow.router.legacy.View;
import com.vaadin.flow.router.legacy.RouterTest.RouterTestUI;
import com.vaadin.flow.router.legacy.ViewRendererTest.AnotherParentView;
import com.vaadin.flow.router.legacy.ViewRendererTest.AnotherTestView;
import com.vaadin.flow.router.legacy.ViewRendererTest.ParentView;
import com.vaadin.flow.router.legacy.ViewRendererTest.TestView;

public class LocationChangeEventTest {
    private LocationChangeEvent event;
    private NavigationEvent navigationEvent;

    @Before
    public void setup() {
        UI ui = new RouterTestUI();

        event = new LocationChangeEvent(ui.getRouterInterface().get(), ui,
                NavigationTrigger.PROGRAMMATIC, new Location(""),
                Arrays.asList(new AnotherTestView(), new AnotherParentView()),
                Collections.emptyMap());

        event.getSource().reconfigure(
                c -> c.setParentView(TestView.class, ParentView.class));

        navigationEvent = new NavigationEvent(event.getSource(),
                event.getLocation(), event.getUI(), event.getTrigger());
    }

    @Test
    public void noReroute() {
        Assert.assertFalse(event.getRerouteTarget().isPresent());
    }

    @Test
    public void explicitRerouteTarget() {
        NavigationHandler handler = e -> 200;
        event.rerouteTo(handler);

        Assert.assertTrue(event.getRerouteTarget().isPresent());

        Assert.assertSame(handler, event.getRerouteTarget().get());
    }

    @Test
    public void removeRerouteTarget() {
        NavigationHandler handler = e -> 200;
        event.rerouteTo(handler);

        Assert.assertTrue(event.getRerouteTarget().isPresent());

        event.rerouteTo((NavigationHandler) null);

        Assert.assertFalse(event.getRerouteTarget().isPresent());
    }

    @Test
    public void rerouteToErrorView() {
        event.rerouteToErrorView();

        Assert.assertSame(
                event.getSource().getConfiguration().getErrorHandler(),
                event.getRerouteTarget().get());
    }

    @Test
    public void rerouteToView() {
        event.rerouteTo(TestView.class);

        event.getRerouteTarget().get().handle(navigationEvent);

        List<View> viewChain = event.getUI().getInternals()
                .getActiveViewChain();

        Assert.assertEquals(2, viewChain.size());

        Assert.assertSame(TestView.class, viewChain.get(0).getClass());

        // Parent view from router conf
        Assert.assertSame(ParentView.class, viewChain.get(1).getClass());
    }
}
