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
package com.vaadin.generator.metadata;

import java.io.Serializable;
import java.util.List;

import com.vaadin.ui.HasComponents;

/**
 * Base class of the representation of a webcomponent for the code generator. It
 * holds all the data needed to generate the Java wrapper class that interacts
 * with the webcomponent at the client side.
 *
 */
public class ComponentMetadata implements Serializable {

    private String tag;
    private String name;
    private String version;
    private String documentation;
    private List<ComponentProperty> properties;
    private List<ComponentFunction> functions;
    private List<ComponentEvent> events;
    private List<String> behaviors;

    /**
     * Gets the name of the ES6 class of the webcomponent, which is used to
     * create the corresponding Java class.
     * 
     * @return The name of the of the webcomponent.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the ES6 class of the webcomponent, which is used to
     * create the corresponding Java class.
     * 
     * @param name
     *            The name of the of the webcomponent.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the tag name used by the webcomponent. For example "my-component",
     * which is used at the HTML page as "<my-component>".
     * 
     * @return The tag name of the webcomponent.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the tag name used by the webcomponent. For example "my-component",
     * which is used at the HTML page as "<my-component>".
     * 
     * @param tag
     *            The tag name of the webcomponent.
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Gets the {@link List} of properties exposed by the webcomponent. Each
     * individual property is represented by the {@link ComponentProperty}
     * object.
     * 
     * @return The list of exposed properties.
     */
    public List<ComponentProperty> getProperties() {
        return properties;
    }

    /**
     * Sets the {@link List} of properties exposed by the webcomponent. Each
     * individual property is represented by the {@link ComponentProperty}
     * object.
     * 
     * @param properties
     *            The list of exposed properties.
     */
    public void setProperties(List<ComponentProperty> properties) {
        this.properties = properties;
    }

    /**
     * Gets the list of behaviors a webcomponent can have. A behavior can
     * trigger different code generation workflows. For example, the behavior
     * "container" makes the corresponding Java class accept child components,
     * by implementing the {@link HasComponents} interface.
     * 
     * @return The list of behaviors of the webcomponent.
     */
    public List<String> getBehaviors() {
        return behaviors;
    }

    /**
     * Sets the list of behaviors a webcomponent can have. A behavior can
     * trigger different code generation workflows. For example, the behavior
     * "container" makes the corresponding Java class accept child components,
     * by implementing the {@link HasComponents} interface.
     * 
     * @param behaviors
     *            The list of behaviors of the webcomponent.
     */
    public void setBehaviors(List<String> behaviors) {
        this.behaviors = behaviors;
    }

    /**
     * Gets the version of the webcomponent.
     * 
     * @return The version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the webcomponent.
     * 
     * @param version
     *            The version of the webcomponent.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets the public documentation of the webcomponent, that can be used to
     * generate the corresponding Javadoc at the Java class.
     * 
     * @return The class-level documentation of the webcomponent.
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * Sets the public documentation of the webcomponent, that can be used to
     * generate the corresponding Javadoc at the Java class.
     * 
     * @param documentation
     *            The class-level documentation of the webcomponent.
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    /**
     * Gets the list of exposed functions of the webcomponent, that can be
     * called from the corresponding Java class. Each function is represented by
     * the {@link ComponentFunction} object.
     * 
     * @return The list of exposed functions.
     */
    public List<ComponentFunction> getFunctions() {
        return functions;
    }

    /**
     * Sets the list of exposed functions of the webcomponent, that can be
     * called from the corresponding Java class. Each function is represented by
     * the {@link ComponentFunction} object.
     * 
     * @param functions
     *            The list of exposed functions.
     */
    public void setFunctions(List<ComponentFunction> functions) {
        this.functions = functions;
    }

    /**
     * Gets the list of events triggered by the webcomponent, that can be
     * intercepted at the server side by the corresponding Java class. Each
     * event is represented by the {@link ComponentEvent} object.
     * 
     * @return The list of events.
     */
    public List<ComponentEvent> getEvents() {
        return events;
    }

    /**
     * Sets the list of events triggered by the webcomponent, that can be
     * intercepted at the server side by the corresponding Java class. Each
     * event is represented by the {@link ComponentEvent} object.
     * 
     * @param events
     *            The list of events.
     */
    public void setEvents(List<ComponentEvent> events) {
        this.events = events;
    }

}
