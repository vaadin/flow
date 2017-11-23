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
package com.vaadin.flow.demo.model;

import java.io.Serializable;

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.router.Route;

/**
 * Object that defines a demo to be shown at the application.
 *
 */
public class DemoObject implements Serializable {

    private String href;
    private String name;
    private String subcategory;

    /**
     * Default constructor.
     */
    public DemoObject() {

    }

    /**
     * Creates a DemoObject taking the values from the {@link ComponentDemo} and
     * {@link Route} annotations from the input class.
     *
     * @param clazz
     *            the class that contains the {@link ComponentDemo} and
     *            {@link Route} annotations
     */
    public DemoObject(Class<? extends DemoView> clazz) {
        ComponentDemo componentDemo = clazz.getAnnotation(ComponentDemo.class);
        Route route = clazz.getAnnotation(Route.class);

        setHref(route.value()).setName(componentDemo.name())
                .setSubcategory(componentDemo.subcategory());
    }

    /**
     * Gets the relative URL of the demo page.
     *
     * @return the href
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the relative URL of the demo page.
     *
     * @param href
     *            the href of the page
     * @return the object instance for method chaining
     */
    public DemoObject setHref(String href) {
        this.href = href;
        return this;
    }

    /**
     * Gets the name of the demo.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the demo.
     *
     * @param name
     *            the name of the demo.
     * @return the object instance for method chaining
     */
    public DemoObject setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the subcategory of the demo.
     *
     * @return the subcategory to show at the menu of demos
     */
    public String getSubcategory() {
        return subcategory;
    }

    /**
     * Sets the subcategory of the demo.
     *
     * @param subcategory
     *            the subcategory of the demo to show at the menu of demos.
     * @return the object instance for method chaining
     */
    public DemoObject setSubcategory(String subcategory) {
        this.subcategory = subcategory;
        return this;
    }
}
