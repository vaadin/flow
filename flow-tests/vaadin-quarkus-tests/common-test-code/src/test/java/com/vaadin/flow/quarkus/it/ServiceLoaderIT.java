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
package com.vaadin.flow.quarkus.it;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.quarkus.it.serviceloader.ServiceLoaderView;
import com.vaadin.flow.test.AbstractChromeIT;

@QuarkusIntegrationTest
class ServiceLoaderIT extends AbstractChromeIT {

    @Override
    protected String getTestPath() {
        return "/service-loader";
    }

    @Test
    void open_initListenerFromServiceFileRan() {
        open();

        Assertions
                .assertEquals("Init listener from META-INF/services ran: true",
                        $(DivElement.class)
                                .id(ServiceLoaderView.INIT_LISTENER_RAN_ID)
                                .getText());
    }

    @Test
    void open_routeFilterFromServiceFileApplied() {
        open();

        Assertions.assertEquals(
                "Route rejected by the filter from META-INF/services registered: false",
                $(DivElement.class)
                        .id(ServiceLoaderView.FILTERED_ROUTE_REGISTERED_ID)
                        .getText());
    }
}
