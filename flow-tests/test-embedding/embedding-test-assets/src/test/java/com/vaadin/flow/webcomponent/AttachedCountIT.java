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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AttachedCountIT extends ChromeBrowserTest implements HasById {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/attachedCount.html";
    }

    @Test
    public void removedBeforeConnecting_connectsOnlyWhenAttachedAgain() {
        open();
        // Wait for the client to connect the element in the page. The page
        // also adds an element and removes it again before it connects.
        waitUntil(driver -> !getAttachedCount("first").isEmpty());

        $("button").id("add").click();
        waitUntil(driver -> !getAttachedCount("added").isEmpty());
        Assert.assertEquals(
                "Only the elements in the page should have a server-side component",
                "2", getAttachedCount("added"));

        $("button").id("reattach").click();
        waitUntil(driver -> !getAttachedCount("removed").isEmpty());
        Assert.assertEquals(
                "The removed element should connect when attached again", "3",
                getAttachedCount("removed"));
    }

    private String getAttachedCount(String id) {
        return byId(id, "count").getText();
    }
}
