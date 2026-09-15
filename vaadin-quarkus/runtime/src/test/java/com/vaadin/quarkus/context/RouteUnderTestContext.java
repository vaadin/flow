/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus.context;

import java.util.Collections;
import java.util.List;

import io.quarkus.arc.Arc;
import net.bytebuddy.ByteBuddy;
import org.mockito.Mockito;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;
import com.vaadin.quarkus.context.RouteScopedContext.NavigationData;

public class RouteUnderTestContext implements UnderTestContext {

    private static UIUnderTestContext uiContextUnderTest;

    private UI ui;

    private static int INDEX = 0;

    private void mockUI() {
        if (uiContextUnderTest == null) {
            uiContextUnderTest = new UIUnderTestContext();
            uiContextUnderTest.activate();
        }
        ui = uiContextUnderTest.getUi();
    }

    @Override
    public void activate() {
        if (ui == null) {
            mockUI();
        }
        Class<?> clazz = new ByteBuddy().subclass(TestNavigationTarget.class)
                .name(TestNavigationTarget.class.getName() + "_" + INDEX + "$")
                .make().load(TestNavigationTarget.class.getClassLoader())
                .getLoaded();
        INDEX++;
        UI.setCurrent(ui);
        NavigationData data = new NavigationData(clazz,
                Collections.emptyList());
        ComponentUtil.setData(ui, NavigationData.class, data);
    }

    @Override
    public void tearDownAll() {
        UI.setCurrent(null);
        if (uiContextUnderTest != null) {
            uiContextUnderTest.tearDownAll();
            uiContextUnderTest = null;
        }
    }

    @Override
    public void destroy() {
        // throw new UnsupportedOperationException(
        // "The underlying context cannot be destroyed using mocks. Destroy
        // context via calling API methods");
        List<HasElement> newNavigation = Collections
                .singletonList(new TestHasElement());
        Arc.container().beanManager().getEvent()
                .fire(new AfterNavigationEvent(
                        new LocationChangeEvent(Mockito.mock(Router.class), ui,
                                NavigationTrigger.PROGRAMMATIC,
                                new Location("foo"), newNavigation)));
    }
}
