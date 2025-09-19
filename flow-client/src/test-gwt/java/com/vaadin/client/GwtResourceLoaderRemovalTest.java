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
package com.vaadin.client;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.client.ResourceLoader.ResourceLoadEvent;
import com.vaadin.client.ResourceLoader.ResourceLoadListener;

/**
 * Tests for ResourceLoader stylesheet removal and re-addition functionality.
 */
public class GwtResourceLoaderRemovalTest extends ClientEngineTestBase {

    private TestResourceLoader resourceLoader;
    private Registry registry;
    
    private static class TestResourceLoader extends ResourceLoader {
        
        List<String> loadedUrls = new ArrayList<>();
        int loadStylesheetWithIdCalls = 0;
        int clearLoadedResourceByIdCalls = 0;
        
        public TestResourceLoader(Registry registry) {
            super(registry, false);
        }
        
        @Override
        public void loadStylesheet(String stylesheetUrl,
                ResourceLoadListener resourceLoadListener, String dependencyId) {
            loadStylesheetWithIdCalls++;
            loadedUrls.add(stylesheetUrl);
            
            // Simulate immediate successful loading
            if (resourceLoadListener != null) {
                resourceLoadListener.onLoad(new ResourceLoadEvent(this, stylesheetUrl));
            }
        }
        
        @Override 
        public void loadStylesheet(String stylesheetUrl,
                ResourceLoadListener resourceLoadListener) {
            loadStylesheet(stylesheetUrl, resourceLoadListener, null);
        }
        
        @Override
        public void clearLoadedResourceById(String dependencyId) {
            clearLoadedResourceByIdCalls++;
            super.clearLoadedResourceById(dependencyId);
        }
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        registry = new Registry();
        resourceLoader = new TestResourceLoader(registry);
    }

    /**
     * Test that the loadStylesheet method with dependencyId is called correctly.
     */
    public void testStylesheetRemovalAndReAdd() {
        String url = "test.css";
        String dependencyId = "test-dep-id-123";
        
        // First load with dependency ID
        resourceLoader.loadStylesheet(url, null, dependencyId);
        assertEquals("loadStylesheet with ID should be called once", 
                1, resourceLoader.loadStylesheetWithIdCalls);
        assertEquals("Should have loaded once", 1, resourceLoader.loadedUrls.size());
        
        // Clear the loaded resource by dependency ID
        resourceLoader.clearLoadedResourceById(dependencyId);
        assertEquals("clearLoadedResourceById should be called once", 
                1, resourceLoader.clearLoadedResourceByIdCalls);
        
        // Second load with new dependency ID
        String newDependencyId = "test-dep-id-456";
        resourceLoader.loadStylesheet(url, null, newDependencyId);
        
        assertEquals("loadStylesheet with ID should be called twice", 
                2, resourceLoader.loadStylesheetWithIdCalls);
        assertEquals("Should have loaded twice after removal and re-add", 
                2, resourceLoader.loadedUrls.size());
    }
    
    /**
     * Test that clearing by dependency ID is called properly.
     */
    public void testClearLoadedResourceById() {
        String url1 = "style1.css";
        String url2 = "style2.css";
        String depId1 = "dep-id-1";
        String depId2 = "dep-id-2";
        
        // Load both stylesheets
        resourceLoader.loadStylesheet(url1, null, depId1);
        resourceLoader.loadStylesheet(url2, null, depId2);
        assertEquals("Should have loaded two stylesheets", 
                2, resourceLoader.loadedUrls.size());
        
        // Clear only the first one
        resourceLoader.clearLoadedResourceById(depId1);
        assertEquals("clearLoadedResourceById should be called once", 
                1, resourceLoader.clearLoadedResourceByIdCalls);
        
        // Load first URL again with a new ID
        String newDepId1 = "dep-id-1-new";
        resourceLoader.loadStylesheet(url1, null, newDepId1);
        
        assertEquals("Should have loaded three times total", 
                3, resourceLoader.loadedUrls.size());
    }
    
    /**
     * Test that multiple removals and re-additions work correctly.
     */
    public void testMultipleRemovalsAndReAdds() {
        String url = "dynamic.css";
        
        for (int i = 0; i < 3; i++) {
            String depId = "dep-id-" + i;
            
            // Load the stylesheet
            resourceLoader.loadStylesheet(url, null, depId);
            
            // Clear it
            resourceLoader.clearLoadedResourceById(depId);
        }
        
        // Should have loaded 3 times and cleared 3 times
        assertEquals("Should have loaded 3 times total", 
                3, resourceLoader.loadedUrls.size());
        assertEquals("Should have cleared 3 times total", 
                3, resourceLoader.clearLoadedResourceByIdCalls);
    }
}