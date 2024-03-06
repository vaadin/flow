/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

    private long countPendingInvocations() {
        return ui.getInternals().getPendingJavaScriptInvocations().count();
    }

}
