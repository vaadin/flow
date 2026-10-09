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
package com.example.application;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ServiceLoaderIT extends ChromeBrowserTest {

    @Test
    public void open_initListenerFromServiceFileRan() {
        open();

        Assert.assertEquals("Init listener from META-INF/services ran: true",
                $(DivElement.class).id(ServiceLoaderView.INIT_LISTENER_RAN_ID)
                        .getText());
    }

    @Test
    public void open_routeFilterFromServiceFileApplied() {
        open();

        Assert.assertEquals(
                "Route rejected by the filter from META-INF/services registered: false",
                $(DivElement.class)
                        .id(ServiceLoaderView.FILTERED_ROUTE_REGISTERED_ID)
                        .getText());
    }

    @Override
    protected String getTestPath() {
        return "/service-loader";
    }
}
