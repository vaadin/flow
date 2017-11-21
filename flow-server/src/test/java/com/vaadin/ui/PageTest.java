/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.Page.ExecutionCanceler;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class PageTest {
    private MockUI ui = new MockUI();
    private Page page = ui.getPage();

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullStyleSheet() {
        page.addStyleSheet(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullJavaScript() {
        page.addJavaScript(null);
    }

    public void testSetTitle_nullTitle_clearsPendingJsExecution() {
        page.setTitle("foobar");

        assertEquals(1,
                ui.getInternals().getPendingJavaScriptInvocations().size());

        page.setTitle(null);

        assertEquals(0,
                ui.getInternals().getPendingJavaScriptInvocations().size());
    }

    @Test
    public void testJavasScriptExecutionCancel() {
        assertEquals(0,
                ui.getInternals().getPendingJavaScriptInvocations().size());

        ExecutionCanceler executeJavaScript = page
                .executeJavaScript("window.alert('$0');", "foobar");

        assertEquals(1,
                ui.getInternals().getPendingJavaScriptInvocations().size());

        assertTrue(executeJavaScript.cancelExecution());

        assertEquals(0,
                ui.getInternals().getPendingJavaScriptInvocations().size());
    }

    @Test
    public void testJavaScriptExecutionTooLateCancel() {
        assertEquals(0,
                ui.getInternals().getPendingJavaScriptInvocations().size());

        ExecutionCanceler executeJavaScript = page
                .executeJavaScript("window.alert('$0');", "foobar");

        assertEquals(1,
                ui.getInternals().getPendingJavaScriptInvocations().size());

        assertEquals(1,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        assertEquals(0,
                ui.getInternals().getPendingJavaScriptInvocations().size());

        assertFalse(executeJavaScript.cancelExecution());
    }

}
