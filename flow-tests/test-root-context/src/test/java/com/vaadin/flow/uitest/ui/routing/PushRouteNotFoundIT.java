/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.routing;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.uitest.ui.routing.PushRouteNotFoundView;
import com.vaadin.testbench.TestBenchElement;

public class PushRouteNotFoundIT extends ChromeBrowserTest {

    @Test
    public void renderRouteNotFoundErrorPage_pushIsSpecifiedViaParentLayout() {
        open();

        TestBenchElement push = $(TestBenchElement.class).id("push-layout")
                .$(TestBenchElement.class).id("push-mode");
        Assert.assertEquals("Push mode: AUTOMATIC", push.getText());
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        int index = path.lastIndexOf("/");
        return path.substring(0, index + 1)
                + PushRouteNotFoundView.PUSH_NON_EXISTENT_PATH;
    }
}
