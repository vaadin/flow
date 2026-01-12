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
package com.vaadin.flow.eagerbootstrap;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ParameterIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/parameter";
    }

    private void openWithParameter(String parameter) {
        String url = getTestURL();
        getDriver().get(url + "/" + parameter);
        waitForDevServer();
    }

    @Test
    public void setParameterCalledAsExpected() {
        openWithParameter("foo");
        Assert.assertEquals("setParameter called with: foo",
                getParametersText());
        $("*").id("barLink").click();
        Assert.assertEquals(
                "setParameter called with: foo\nsetParameter called with: bar",
                getParametersText());
    }

    private String getParametersText() {
        return $("*").id("parameters").getText();
    }

    private int getInstance() {
        String instanceText = $("*").id("instance").getText();
        return Integer
                .parseInt(instanceText.replace("This is view instance ", ""));
    }

}
