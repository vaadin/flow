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
package com.vaadin.flow.uitest.ui;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.DevModeClassCacheView")
public class DevModeClassCacheView extends AbstractReloadView {

    @SuppressWarnings("unchecked")
    public DevModeClassCacheView() {

        addTriggerButton();
        addViewId();

        try {
            Class<?> reloadCacheClass = Class
                    .forName("com.vaadin.flow.spring.ReloadCache");

            // lookupClasses;
            Field lookupClassesField = reloadCacheClass
                    .getDeclaredField("lookupClasses");
            lookupClassesField.setAccessible(true);
            Set<Class<?>> lookupClasses = (Set<Class<?>>) lookupClassesField
                    .get(null);
            add(new Span("lookup class count:"
                    + (lookupClasses == null ? 0 : lookupClasses.size())));

            // validResources
            Field validResourcesField = reloadCacheClass
                    .getDeclaredField("validResources");
            validResourcesField.setAccessible(true);
            Set<String> validResources = (Set<String>) validResourcesField
                    .get(null);
            add(new Span("valid resource count:" + validResources.size()));

            // skippedResources
            Field skippedResourcesField = reloadCacheClass
                    .getDeclaredField("skippedResources");
            skippedResourcesField.setAccessible(true);
            Set<String> skippedResources = (Set<String>) skippedResourcesField
                    .get(null);
            add(new Span("skipped resource count:" + skippedResources.size()));

            // dynamicWhiteList;
            Field dynamicWhiteListField = reloadCacheClass
                    .getDeclaredField("dynamicWhiteList");
            dynamicWhiteListField.setAccessible(true);
            Set<String> dynamicWhiteList = (Set<String>) dynamicWhiteListField
                    .get(null);
            add(new Span(
                    "dynamic white list count:" + dynamicWhiteList.size()));

            // routePackages;
            Field routePackagesField = reloadCacheClass
                    .getDeclaredField("routePackages");
            routePackagesField.setAccessible(true);
            Set<String> routePackages = (Set<String>) routePackagesField
                    .get(null);

            Span span = new Span("route packages:"
                    + routePackages.stream().collect(Collectors.joining(",")));
            span.setId("last-span");
            add(span);

        } catch (ClassNotFoundException | NoSuchFieldException
                | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
