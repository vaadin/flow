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
package com.vaadin.flow.component.internal;

import java.util.Collection;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
class PageTest {
    private MockUI ui = new MockUI();
    private Page page = ui.getPage();

    @AfterEach
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    public void testAddNullStyleSheet() {
        assertThrows(IllegalArgumentException.class, () -> {
            page.addStyleSheet(null);
        });
    }

    @Test
    public void testAddNullJavaScript() {
        assertThrows(IllegalArgumentException.class, () -> {
            page.addJavaScript(null);
        });
    }

    @Test
    public void testJavasScriptExecutionCancel() {
        assertEquals(0, countPendingInvocations());

        PendingJavaScriptResult executeJavaScript = page
                .executeJs("window.alert('$0');", "foobar");

        assertEquals(1, countPendingInvocations());

        assertTrue(executeJavaScript.cancelExecution());

        assertEquals(0, countPendingInvocations());
    }

    @Test
    public void testJavaScriptExecutionTooLateCancel() {
        assertEquals(0, countPendingInvocations());

        PendingJavaScriptResult executeJavaScript = page
                .executeJs("window.alert('$0');", "foobar");

        assertEquals(1, countPendingInvocations());

        assertEquals(1,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        assertEquals(0, countPendingInvocations());

        assertFalse(executeJavaScript.cancelExecution());
    }

    @Test
    public void addDynamicImport_dynamicDependencyIsAvaialbleViaGetPendingSendToClient() {
        page.addDynamicImport("foo");

        DependencyList list = ui.getInternals().getDependencyList();
        Collection<Dependency> dependencies = list.getPendingSendToClient();
        assertEquals(1, dependencies.size());
        Dependency dependency = dependencies.iterator().next();
        assertEquals("foo", dependency.getUrl());
    }

    private long countPendingInvocations() {
        return ui.getInternals().getPendingJavaScriptInvocations().count();
    }

}
