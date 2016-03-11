/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.Page.LocationChangeListener;

public class PageTest {
    private Page page = new MockUI().getPage();

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullStyleSheet() {
        page.addStyleSheet(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullJavaScript() {
        page.addJavaScript(null);
    }

    @Test
    public void testLocationInternalOrExternal() {
        Assert.assertTrue(isInternalLocation(""));
        Assert.assertTrue(isInternalLocation("foo"));
        Assert.assertTrue(isInternalLocation("foo/bar"));
        Assert.assertTrue(isInternalLocation("foo/../bar"));

        Assert.assertFalse(isInternalLocation("http://google.com"));
        Assert.assertFalse(isInternalLocation("//google.com"));
        Assert.assertFalse(isInternalLocation("/foo"));
        Assert.assertFalse(isInternalLocation("../foo"));
    }

    private boolean isInternalLocation(String location) {
        AtomicBoolean eventFired = new AtomicBoolean(false);

        LocationChangeListener listener = e -> eventFired.set(true);
        page.addLocationChangeListener(listener);

        page.setLocation(location);

        page.removeLocationChangeListener(listener);

        return eventFired.get();
    }
}
