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
package com.vaadin.flow.di;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.LookupInitializer.AppShellPredicateImpl;
import com.vaadin.flow.server.startup.AppShellPredicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppShellPredicateImplTest {

    private AppShellPredicate predicate = new AppShellPredicateImpl();

    private static class TestAppShellPredicateConfig
            implements AppShellConfigurator {

    }

    @Test
    public void isShell_isAppShellConfigurator_returnsTrue() {
        assertTrue(predicate.isShell(TestAppShellPredicateConfig.class));
    }

    @Test
    public void isShell_isNotAppShellConfigurator_returnsFalse() {
        assertFalse(predicate.isShell(List.class));
    }
}
