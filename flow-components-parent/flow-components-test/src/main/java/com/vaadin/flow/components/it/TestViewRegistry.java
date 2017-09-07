/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.components.it;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

/**
 * Registration initializer that collects all the available test views.
 */
@HandlesTypes(TestView.class)
public class TestViewRegistry implements ServletContainerInitializer {

    private static List<Class<? extends TestView>> availableViews = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        if (set == null) {
            return;
        }
        set.forEach(clazz -> {
            if (TestView.class.isAssignableFrom(clazz)) {
                availableViews.add((Class<? extends TestView>) clazz);
            }
        });
    }

    /**
     * Get a list of all registered test views.
     *
     * @return list of all test views
     */
    public static List<Class<? extends TestView>> getAvailableViews() {
        return new ArrayList<>(availableViews);
    }
}
