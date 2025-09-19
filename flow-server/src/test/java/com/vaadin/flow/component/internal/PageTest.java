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
package com.vaadin.flow.component.internal;

import java.util.Collection;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.shared.Registration;
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

    @Test
    public void testJavasScriptExecutionCancel() {
        Assert.assertEquals(0, countPendingInvocations());

        PendingJavaScriptResult executeJavaScript = page
                .executeJs("window.alert('$0');", "foobar");

        Assert.assertEquals(1, countPendingInvocations());

        Assert.assertTrue(executeJavaScript.cancelExecution());

        Assert.assertEquals(0, countPendingInvocations());
    }

    @Test
    public void testJavaScriptExecutionTooLateCancel() {
        Assert.assertEquals(0, countPendingInvocations());

        PendingJavaScriptResult executeJavaScript = page
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

    @Test
    public void addStyleSheet_canBeReAddedAfterRemoval() {
        String stylesheetUrl = "styles/test.css";
        DependencyList list = ui.getInternals().getDependencyList();
        
        // Add stylesheet first time
        var registration = page.addStyleSheet(stylesheetUrl);
        Collection<Dependency> dependencies = list.getPendingSendToClient();
        Assert.assertEquals(1, dependencies.size());
        Dependency firstDependency = dependencies.iterator().next();
        Assert.assertEquals(stylesheetUrl, firstDependency.getUrl());
        Assert.assertNotNull(firstDependency.getId());
        
        // Clear pending to simulate that it was sent
        list.clearPendingSendToClient();
        
        // Remove the stylesheet
        registration.remove();
        
        // Add stylesheet again
        var registration2 = page.addStyleSheet(stylesheetUrl);
        dependencies = list.getPendingSendToClient();
        Assert.assertEquals("Stylesheet should be added again after removal", 
                1, dependencies.size());
        Dependency secondDependency = dependencies.iterator().next();
        Assert.assertEquals(stylesheetUrl, secondDependency.getUrl());
        Assert.assertNotNull(secondDependency.getId());
        
        // The IDs should be different
        Assert.assertNotEquals("New dependency should have a different ID",
                firstDependency.getId(), secondDependency.getId());
        
        // Clean up
        registration2.remove();
    }

    private long countPendingInvocations() {
        return ui.getInternals().getPendingJavaScriptInvocations().count();
    }

}
