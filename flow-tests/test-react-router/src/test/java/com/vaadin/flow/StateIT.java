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
package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StateIT extends ChromeBrowserTest {

    @Test
    public void validateReactInUse() {
        open();

        waitForDevServer();

        SpanElement reactEnabled = $(SpanElement.class)
                .id(StateView.ENABLED_SPAN);
        Assert.assertEquals("React not enabled", "React enabled: true",
                reactEnabled.getText());

        SpanElement reactInPackage = $(SpanElement.class)
                .id(StateView.REACT_SPAN);

        Assert.assertEquals("No react found in package.json",
                "React found: true", reactInPackage.getText());
    }

}
