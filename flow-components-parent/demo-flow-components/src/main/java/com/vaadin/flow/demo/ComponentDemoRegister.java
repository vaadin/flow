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
package com.vaadin.flow.demo;

import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import com.vaadin.flow.demo.views.DemoView;

/**
 * Registration initializer that collects all the demo views available
 */
@HandlesTypes(ComponentDemo.class)
public class ComponentDemoRegister implements ServletContainerInitializer {

    private static List<Class<? extends DemoView>> availableViews = new ArrayList<>();

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        availableViews = set.stream()
                .filter(DemoView.class::isAssignableFrom)
                .map(clazz -> (Class<? extends DemoView>) clazz)
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());
    }

    public static List<Class<? extends DemoView>> getAvailableViews() {
        return new ArrayList<>(availableViews);
    }

    public static Optional<Class<? extends DemoView>> getViewFor(String componentName){
        return availableViews.stream()
                .filter(v -> v.getAnnotation(ComponentDemo.class).href().equals(componentName))
                .findFirst();
    }
}
