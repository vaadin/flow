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

/**
 * Object that defines a demo to be shown at the application.
 *
 */
public class DemoObject implements Serializable {

    private String href;
    private String name;

    public DemoObject() {

    }

    public DemoObject(ComponentDemo componentDemo) {
        setHref(componentDemo.href()).setName(componentDemo.name());
    }

    /**
     * Gets the relative URL of the demo page.
     * 
     * @return the href.
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the relative URL of the demo page.
     * 
     * @param href
     *            The href of the page.
     * @return The object instance for method chaining.
     */
    public DemoObject setHref(String href) {
        this.href = href;
        return this;
    }

    /**
     * Gets the name of the demo.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the demo.
     * 
     * @param name
     *            The name of the demo.
     * @return The object instance for method chaining.
     */
    public DemoObject setName(String name) {
        this.name = name;
        return this;
    }
}
