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
package com.vaadin.flow.component.page;

import java.util.Collection;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests for stylesheet removal and re-addition functionality.
 */
public class StylesheetRemovalTest {
    
    private UI ui;
    private Page page;
    private UIInternals internals;
    private DependencyList dependencyList;
    
    @Before
    public void setup() {
        ui = new MockUI();
        page = ui.getPage();
        internals = ui.getInternals();
        dependencyList = internals.getDependencyList();
    }
    
    @Test
    public void addStyleSheet_returnsRegistration_thatRemovesStylesheet() {
        String url = "http://example.com/style.css";
        
        // Add stylesheet
        Registration registration = page.addStyleSheet(url);
        Assert.assertNotNull("Registration should not be null", registration);
        
        // Check that dependency is added
        Collection<Dependency> dependencies = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should have one dependency", 1, dependencies.size());
        Dependency dep = dependencies.iterator().next();
        Assert.assertEquals("URL should match", url, dep.getUrl());
        Assert.assertNotNull("Dependency should have ID", dep.getId());
        
        // Remove stylesheet
        registration.remove();
        
        // Check that removal is tracked
        Set<String> removals = internals.getPendingStyleSheetRemovals();
        Assert.assertEquals("Should have one removal pending", 1, removals.size());
        Assert.assertEquals("Removal ID should match dependency ID", dep.getId(), removals.iterator().next());
    }
    
    @Test
    public void addStyleSheet_removeAndReAdd_shouldWorkCorrectly() {
        String url = "http://example.com/reusable.css";
        
        // First addition
        Registration reg1 = page.addStyleSheet(url);
        Collection<Dependency> deps1 = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should have one dependency after first add", 1, deps1.size());
        Dependency firstDep = deps1.iterator().next();
        String firstId = firstDep.getId();
        Assert.assertNotNull("First dependency should have ID", firstId);
        
        // Clear pending (simulate sent to client)
        dependencyList.clearPendingSendToClient();
        
        // Remove the stylesheet
        reg1.remove();
        Set<String> removals = internals.getPendingStyleSheetRemovals();
        Assert.assertTrue("Removal should be tracked", removals.contains(firstId));
        
        // Clear removal tracking (simulate sent to client)
        internals.clearPendingStyleSheetRemovals();
        
        // Re-add the same stylesheet
        Registration reg2 = page.addStyleSheet(url);
        Collection<Dependency> deps2 = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should have one dependency after re-add", 1, deps2.size());
        Dependency secondDep = deps2.iterator().next();
        String secondId = secondDep.getId();
        Assert.assertNotNull("Second dependency should have ID", secondId);
        Assert.assertNotEquals("Second ID should be different from first", firstId, secondId);
        Assert.assertEquals("URL should be the same", url, secondDep.getUrl());
    }
    
    @Test
    public void addStyleSheetWithLoadMode_removeAndReAdd_shouldWorkCorrectly() {
        String url = "http://example.com/lazy.css";
        
        // First addition with LAZY load mode
        Registration reg1 = page.addStyleSheet(url, LoadMode.LAZY);
        Collection<Dependency> deps1 = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should have one dependency after first add", 1, deps1.size());
        Dependency firstDep = deps1.iterator().next();
        String firstId = firstDep.getId();
        Assert.assertNotNull("First dependency should have ID", firstId);
        Assert.assertEquals("Load mode should be LAZY", LoadMode.LAZY, firstDep.getLoadMode());
        
        // Clear pending (simulate sent to client)
        dependencyList.clearPendingSendToClient();
        
        // Remove the stylesheet
        reg1.remove();
        Set<String> removals = internals.getPendingStyleSheetRemovals();
        Assert.assertTrue("Removal should be tracked", removals.contains(firstId));
        
        // Clear removal tracking (simulate sent to client)
        internals.clearPendingStyleSheetRemovals();
        
        // Re-add the same stylesheet with EAGER mode
        Registration reg2 = page.addStyleSheet(url, LoadMode.EAGER);
        Collection<Dependency> deps2 = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should have one dependency after re-add", 1, deps2.size());
        Dependency secondDep = deps2.iterator().next();
        String secondId = secondDep.getId();
        Assert.assertNotNull("Second dependency should have ID", secondId);
        Assert.assertNotEquals("Second ID should be different from first", firstId, secondId);
        Assert.assertEquals("URL should be the same", url, secondDep.getUrl());
        Assert.assertEquals("Load mode should be EAGER", LoadMode.EAGER, secondDep.getLoadMode());
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
        
        Collection<Dependency> allDeps = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should have three dependencies", 3, allDeps.size());
        
        // Find the dependency for url2
        String idToRemove = null;
        for (Dependency dep : allDeps) {
            if (url2.equals(dep.getUrl())) {
                idToRemove = dep.getId();
                break;
            }
        }
        Assert.assertNotNull("Should find dependency for url2", idToRemove);
        
        // Clear pending (simulate sent to client)
        dependencyList.clearPendingSendToClient();
        
        // Remove only the second stylesheet
        reg2.remove();
        
        Set<String> removals = internals.getPendingStyleSheetRemovals();
        Assert.assertEquals("Should have one removal", 1, removals.size());
        Assert.assertEquals("Should remove the correct stylesheet", idToRemove, removals.iterator().next());
    }
    
    @Test
    public void uidlWriter_includesStylesheetRemovals() {
        String url = "http://example.com/to-remove.css";
        
        // Add and get the dependency ID
        Registration reg = page.addStyleSheet(url);
        Collection<Dependency> deps = dependencyList.getPendingSendToClient();
        String depId = deps.iterator().next().getId();
        
        // Clear pending (simulate sent to client)
        dependencyList.clearPendingSendToClient();
        
        // Remove the stylesheet
        reg.remove();
        
        // Create UIDL response
        UidlWriter writer = new UidlWriter();
        ObjectNode response = writer.createUidl(ui, false);
        
        // Check that removals are included in response
        Assert.assertTrue("Response should contain stylesheetRemovals", 
                response.has("stylesheetRemovals"));
        ArrayNode removalsArray = (ArrayNode) response.get("stylesheetRemovals");
        Assert.assertEquals("Should have one removal", 1, removalsArray.size());
        Assert.assertEquals("Removal ID should match", depId, removalsArray.get(0).asText());
        
        // After creating UIDL, removals should be cleared
        Set<String> pendingRemovals = internals.getPendingStyleSheetRemovals();
        Assert.assertTrue("Pending removals should be cleared after UIDL creation", 
                pendingRemovals.isEmpty());
    }
    
    @Test
    public void testStylesheetCacheClearing_afterRemoval() {
        String url = "http://example.com/cached-style.css";
        
        // Add stylesheet first time
        Registration reg1 = page.addStyleSheet(url);
        Collection<Dependency> deps1 = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should have one dependency", 1, deps1.size());
        
        // Clear pending (simulate sent to client)
        dependencyList.clearPendingSendToClient();
        
        // Try adding same URL again - should be cached
        page.addStyleSheet(url);
        Collection<Dependency> deps2 = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should not add duplicate dependency", 0, deps2.size());
        
        // Remove the stylesheet
        reg1.remove();
        
        // Now add the same URL again - should work since cache was cleared
        page.addStyleSheet(url);
        Collection<Dependency> deps3 = dependencyList.getPendingSendToClient();
        Assert.assertEquals("Should add dependency after removal", 1, deps3.size());
        Dependency newDep = deps3.iterator().next();
        Assert.assertEquals("URL should match", url, newDep.getUrl());
        Assert.assertNotNull("Should have a new ID", newDep.getId());
    }
}