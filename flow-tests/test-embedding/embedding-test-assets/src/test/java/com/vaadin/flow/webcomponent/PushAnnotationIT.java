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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PushAnnotationIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/push.html";
    }

    @Test
    public void pushUpdatesEmbeddedWebComponent() {
        open();

        int initialUpdateCount = getUpdateCount();
        Assert.assertTrue(
                "The initial update count should be less than maximum 50, but it has value "
                        + initialUpdateCount,
                initialUpdateCount < 50);

        waitUntil(driver -> getUpdateCount() > initialUpdateCount, 5);

        int nextUpdateCount = getUpdateCount();

        Assert.assertTrue(
                "The next interim update count should be less than maximum 50, but it has value "
                        + nextUpdateCount,
                nextUpdateCount < 50);

        waitUntil(driver -> getUpdateCount() == 50, 5);

        int updateCount = getUpdateCount();
        Assert.assertEquals(
                "The update count should have reached the maximum 50, but it " +
                        "has value "
                        + updateCount,
                50, updateCount);
    }

    private int getUpdateCount() {
        TestBenchElement webComponent = $("embedded-push").first();
        String count = webComponent.getText();
        return Integer.parseInt(count);
    }
}
