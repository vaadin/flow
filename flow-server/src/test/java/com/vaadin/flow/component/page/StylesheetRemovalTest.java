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

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for stylesheet registration, removal, and re-addition
 * functionality.
 */
class StylesheetRemovalTest {

    private UI ui;
    private Page page;
    private UIInternals internals;
    private DependencyList dependencyList;

    @BeforeEach
    public void setup() {
        ui = new MockUI();
        page = ui.getPage();
        internals = ui.getInternals();
        dependencyList = internals.getDependencyList();
    }

    @Test
    public void addStyleSheet_returnsNonNullRegistration() {
        Registration reg1 = page.addStyleSheet("styles.css");
        Registration reg2 = page.addStyleSheet("styles.css", LoadMode.LAZY);

        assertNotNull(reg1, "addStyleSheet should return a Registration");
        assertNotNull(reg2,
                "addStyleSheet with LoadMode should return a Registration");
    }

    @Test
    public void addStyleSheet_returnsRegistration_thatRemovesStylesheet() {
        String url = "http://example.com/style.css";

        // Add stylesheet
        Registration registration = page.addStyleSheet(url);
        assertNotNull(registration, "Registration should not be null");

        // Check that dependency is added
        Collection<Dependency> dependencies = dependencyList
                .getPendingSendToClient();
        assertEquals(1, dependencies.size(), "Should have one dependency");
        Dependency dep = dependencies.iterator().next();
        assertEquals(url, dep.getUrl(), "URL should match");
        assertNotNull(dep.getId(), "Dependency should have ID");

        // Remove stylesheet
        registration.remove();

        // Check that removal is tracked
        Set<String> removals = internals.getPendingStyleSheetRemovals();
        assertEquals(1, removals.size(), "Should have one removal pending");
        assertEquals(dep.getId(), removals.iterator().next(),
                "Removal ID should match dependency ID");
    }

    @Test
    public void stylesheetCanBeReAddedAfterRemoval() {
        String url = "http://example.com/reusable.css";

        Registration reg1 = page.addStyleSheet(url);
        Collection<Dependency> deps1 = dependencyList.getPendingSendToClient();
        assertEquals(1, deps1.size());
        Dependency firstDep = deps1.iterator().next();
        String firstId = firstDep.getId();

        dependencyList.clearPendingSendToClient();

        reg1.remove();
        assertTrue(internals.getPendingStyleSheetRemovals().contains(firstId));
        internals.clearPendingStyleSheetRemovals();

        Registration reg2 = page.addStyleSheet(url);
        Collection<Dependency> deps2 = dependencyList.getPendingSendToClient();
        assertEquals(1, deps2.size());
        Dependency secondDep = deps2.iterator().next();

        assertNotEquals(firstId, secondDep.getId(), "IDs should differ");
        assertEquals(url, secondDep.getUrl(), "URLs should match");
    }

    @Test
    public void stylesheetWithLoadMode_canBeReAddedWithDifferentMode() {
        String url = "http://example.com/lazy.css";

        // Add with LAZY mode
        Registration reg1 = page.addStyleSheet(url, LoadMode.LAZY);
        Dependency firstDep = dependencyList.getPendingSendToClient().iterator()
                .next();
        String firstId = firstDep.getId();
        assertEquals(LoadMode.LAZY, firstDep.getLoadMode());

        dependencyList.clearPendingSendToClient();
        reg1.remove();
        internals.clearPendingStyleSheetRemovals();

        // Re-add with EAGER mode
        Registration reg2 = page.addStyleSheet(url, LoadMode.EAGER);
        Dependency secondDep = dependencyList.getPendingSendToClient()
                .iterator().next();

        assertNotEquals(firstId, secondDep.getId(), "IDs should differ");
        assertEquals(url, secondDep.getUrl(), "URLs should match");
        assertEquals(LoadMode.EAGER, secondDep.getLoadMode());
    }

    @Test
    public void multipleStylesheets_removeOne_othersRemainIntact() {
        String url1 = "http://example.com/style1.css";
        String url2 = "http://example.com/style2.css";
        String url3 = "http://example.com/style3.css";

        // Add three stylesheets
        Registration reg1 = page.addStyleSheet(url1);
        Registration reg2 = page.addStyleSheet(url2);
        Registration reg3 = page.addStyleSheet(url3);

        Collection<Dependency> allDeps = dependencyList
                .getPendingSendToClient();
        assertEquals(3, allDeps.size(), "Should have three dependencies");

        // Find the dependency for url2
        String idToRemove = null;
        for (Dependency dep : allDeps) {
            if (url2.equals(dep.getUrl())) {
                idToRemove = dep.getId();
                break;
            }
        }
        assertNotNull(idToRemove, "Should find dependency for url2");

        dependencyList.clearPendingSendToClient();

        // Remove only the second stylesheet
        reg2.remove();

        Set<String> removals = internals.getPendingStyleSheetRemovals();
        assertEquals(1, removals.size(), "Should have one removal");
        assertEquals(idToRemove, removals.iterator().next(),
                "Should remove the correct stylesheet");
    }

    @Test
    public void uidlWriter_includesStylesheetRemovals() {
        String url = "http://example.com/to-remove.css";

        // Add and get the dependency ID
        Registration reg = page.addStyleSheet(url);
        Collection<Dependency> deps = dependencyList.getPendingSendToClient();
        String depId = deps.iterator().next().getId();

        dependencyList.clearPendingSendToClient();

        // Remove the stylesheet
        reg.remove();

        // Create UIDL response
        UidlWriter writer = new UidlWriter();
        ObjectNode response = writer.createUidl(ui, false);

        // Check that removals are included in response
        assertTrue(response.has("stylesheetRemovals"),
                "Response should contain stylesheetRemovals");
        ArrayNode removalsArray = (ArrayNode) response
                .get("stylesheetRemovals");
        assertEquals(1, removalsArray.size(), "Should have one removal");
        assertEquals(depId, removalsArray.get(0).asString(),
                "Removal ID should match");

        // After creating UIDL, removals should be cleared
        Set<String> pendingRemovals = internals.getPendingStyleSheetRemovals();
        assertTrue(pendingRemovals.isEmpty(),
                "Pending removals should be cleared after UIDL creation");
    }

    @Test
    public void duplicateStylesheet_notAddedUntilRemovedFromCache() {
        String url = "http://example.com/cached-style.css";

        // Add stylesheet
        Registration reg = page.addStyleSheet(url);
        assertEquals(1, dependencyList.getPendingSendToClient().size());
        dependencyList.clearPendingSendToClient();

        page.addStyleSheet(url);
        assertEquals(0, dependencyList.getPendingSendToClient().size());

        reg.remove();

        page.addStyleSheet(url);
        assertEquals(1, dependencyList.getPendingSendToClient().size());
    }

    @Test
    public void duplicateStylesheet_firstRegistrationCanRemove() {
        String url = "http://example.com/duplicate.css";

        // Add stylesheet twice - both should use same ID
        Registration reg1 = page.addStyleSheet(url);
        String firstDepId = dependencyList.getPendingSendToClient().iterator()
                .next().getId();
        dependencyList.clearPendingSendToClient();

        Registration reg2 = page.addStyleSheet(url);
        assertEquals(0, dependencyList.getPendingSendToClient().size(),
                "Second add should not create pending send");

        // First registration should be able to remove the stylesheet
        reg1.remove();
        Set<String> removals = internals.getPendingStyleSheetRemovals();
        assertEquals(1, removals.size(), "Should have one removal pending");
        assertEquals(firstDepId, removals.iterator().next(),
                "Should use the same dependency ID");
    }

    @Test
    public void duplicateStylesheet_secondRegistrationCanRemove() {
        String url = "http://example.com/duplicate2.css";

        // Add stylesheet twice - both should use same ID
        Registration reg1 = page.addStyleSheet(url);
        String firstDepId = dependencyList.getPendingSendToClient().iterator()
                .next().getId();
        dependencyList.clearPendingSendToClient();

        Registration reg2 = page.addStyleSheet(url);
        assertEquals(0, dependencyList.getPendingSendToClient().size(),
                "Second add should not create pending send");

        // Second registration uses the same ID as the first
        reg2.remove();
        Set<String> removals = internals.getPendingStyleSheetRemovals();
        assertEquals(1, removals.size(), "Should have one removal pending");
        String removedId = removals.iterator().next();
        // Both registrations use the same dependency ID
        assertEquals(firstDepId, removedId,
                "Should use the same ID as the original");
    }
}
