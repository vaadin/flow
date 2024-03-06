/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.di;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.LookupInitializer.AppShellPredicateImpl;
import com.vaadin.flow.server.startup.AppShellPredicate;

public class AppShellPredicateImplTest {

    private AppShellPredicate predicate = new AppShellPredicateImpl();

    private static class TestAppShellPredicateConfig
            implements AppShellConfigurator {

    }

    @Test
    public void isShell_isAppShellConfigurator_returnsTrue() {
        Assert.assertTrue(predicate.isShell(TestAppShellPredicateConfig.class));
    }

    @Test
    public void isShell_isNotAppShellConfigurator_returnsFalse() {
        Assert.assertFalse(predicate.isShell(List.class));
    }
}
