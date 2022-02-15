/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.theme;

import java.net.ServerSocket;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.net.PortProber;

public class UtilityClassNameIT extends ChromeBrowserTest {

    @Test
    public void ports() throws Exception {
        int i = 0;
        int port = 0;
        try {

            for (i = 0; i < 1000; i++) {
                port = PortProber.findFreePort();
                System.err.println("Using port " + port);
                ServerSocket socket = new ServerSocket(port);
                System.err.println("listening");
                socket.close();
                System.err.println("stopped");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed at iteration " + i + " when trying port " + port, e);
        }
    }

    @Test
    public void lumoUtils_customStylesHaveBeenExpanded() {
        open();
        checkLogsForErrors();

        SpanElement primary = $(SpanElement.class).id("primary");
        Assert.assertEquals("rgba(0, 128, 0, 1)", primary.getCssValue("color"));
    }
}
