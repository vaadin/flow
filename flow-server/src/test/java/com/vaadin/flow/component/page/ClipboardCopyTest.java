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
package com.vaadin.flow.component.page;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

public class ClipboardCopyTest {

    @Test
    public void setValue_updatesProperty() {
        Element element = new Element("button");
        ClipboardCopy copy = new ClipboardCopy(element,
                Registration.once(() -> {
                }));

        copy.setValue("hello");
        Assert.assertEquals("hello",
                element.getProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY));

        copy.setValue("world");
        Assert.assertEquals("world",
                element.getProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY));
    }

    @Test
    public void setValueNull_setsEmptyString() {
        Element element = new Element("button");
        ClipboardCopy copy = new ClipboardCopy(element,
                Registration.once(() -> {
                }));

        copy.setValue(null);
        Assert.assertEquals("",
                element.getProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY));
    }

    @Test
    public void remove_callsCleanupRegistration() {
        AtomicBoolean cleaned = new AtomicBoolean(false);
        Element element = new Element("button");
        ClipboardCopy copy = new ClipboardCopy(element,
                Registration.once(() -> cleaned.set(true)));

        copy.remove();
        Assert.assertTrue(cleaned.get());
    }

    @Test
    public void remove_calledTwice_cleanupRunsOnlyOnce() {
        int[] count = { 0 };
        Element element = new Element("button");
        ClipboardCopy copy = new ClipboardCopy(element,
                Registration.once(() -> count[0]++));

        copy.remove();
        copy.remove();
        Assert.assertEquals(1, count[0]);
    }
}
