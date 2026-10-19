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
package com.vaadin.flow.test.routing;

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(StateView.class)
public class StateIT extends AbstractDefaultIT {

    @BrowserTest
    public void validateReactInUse() {
        open();

        SpanElement reactEnabled = $(SpanElement.class)
                .id(StateView.ENABLED_SPAN);
        Assertions.assertEquals("React enabled: true", reactEnabled.getText(),
                "React not enabled");

        SpanElement reactInPackage = $(SpanElement.class)
                .id(StateView.REACT_SPAN);

        Assertions.assertEquals("React found: true", reactInPackage.getText(),
                "No react found in package.json");
    }

}
