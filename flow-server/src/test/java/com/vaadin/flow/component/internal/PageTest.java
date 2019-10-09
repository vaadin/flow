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
package com.vaadin.flow.component.internal;

import java.util.Collection;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.Page.ExecutionCanceler;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.tests.util.MockUI;

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

        Assert.assertEquals(1, countPendingInvocations());

        page.setTitle(null);

        Assert.assertEquals(0, countPendingInvocations());
    }

    @Test
    public void testJavasScriptExecutionCancel() {
        Assert.assertEquals(0, countPendingInvocations());

        ExecutionCanceler executeJavaScript = page
                .executeJs("window.alert('$0');", "foobar");

        Assert.assertEquals(1, countPendingInvocations());

        Assert.assertTrue(executeJavaScript.cancelExecution());

        Assert.assertEquals(0, countPendingInvocations());
    }

    @Test
    public void testJavaScriptExecutionTooLateCancel() {
        Assert.assertEquals(0, countPendingInvocations());

        ExecutionCanceler executeJavaScript = page
                .executeJs("window.alert('$0');", "foobar");

        Assert.assertEquals(1, countPendingInvocations());

        Assert.assertEquals(1,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        Assert.assertEquals(0, countPendingInvocations());

        Assert.assertFalse(executeJavaScript.cancelExecution());
    }

    @Test
    public void addDynamicImport_dynamicDependencyIsAvaialbleViaGetPendingSendToClient() {
        page.addDynamicImport("foo");

        DependencyList list = ui.getInternals().getDependencyList();
        Collection<Dependency> dependencies = list.getPendingSendToClient();
        Assert.assertEquals(1, dependencies.size());
        Dependency dependency = dependencies.iterator().next();
        Assert.assertEquals("foo", dependency.getUrl());
    }

    private long countPendingInvocations() {
        return ui.getInternals().getPendingJavaScriptInvocations().count();
    }

}
