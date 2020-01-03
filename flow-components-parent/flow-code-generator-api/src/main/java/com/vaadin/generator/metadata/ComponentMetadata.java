/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.HasComponents;

/**
 * Base class of the representation of a webcomponent for the code generator. It
 * holds all the data needed to generate the Java wrapper class that interacts
 * with the webcomponent at the client side.
 *
 * @since 1.0
 */
public class ComponentMetadata {
    private String tag;
    private String name;
    private String version;
    private String description;
    private String parentTagName;
    private String baseUrl;
    private List<ComponentPropertyData> properties;
    private List<ComponentFunctionData> methods;
    private List<ComponentEventData> events;
    private List<String> behaviors;
    private List<String> slots;
    private List<String> mixins;
    private Map<String, List<String>> variants = new HashMap<>();

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
     * which is used at the HTML page as "&lt;my-component&gt;".
     * 
     * @return The tag name of the webcomponent.
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the tag name used by the webcomponent. For example "my-component",
     * which is used at the HTML page as "&lt;my-component&gt;".
     * 
     * @param tag
     *            The tag name of the webcomponent.
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Gets the {@link List} of properties exposed by the webcomponent. Each
     * individual property is represented by the {@link ComponentPropertyData}
     * object.
     * 
     * @return The list of exposed properties.
     */
    public List<ComponentPropertyData> getProperties() {
        return properties;
    }

    /**
     * Sets the {@link List} of properties exposed by the webcomponent. Each
     * individual property is represented by the {@link ComponentPropertyData}
     * object.
     * 
     * @param properties
     *            The list of exposed properties.
     */
    public void setProperties(List<ComponentPropertyData> properties) {
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
     * Gets the public description of the webcomponent, that can be used to
     * generate the corresponding Javadoc at the Java class.
     * 
     * @return The class-level description of the webcomponent.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the public description of the webcomponent, that can be used to
     * generate the corresponding Javadoc at the Java class.
     * 
     * @param description
     *            The class-level description of the webcomponent.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets parent element tag name or {@code null}, if no parents present.
     * @return parent element tag name or {@code null}, if no parents present.
     */
    public String getParentTagName() {
        return parentTagName;
    }

    /**
     * Sets parent element tag name, that is used to determine inheritance relations.
     *
     * @param parentTagName parent element tag name
     */
    public void setParentTagName(String parentTagName) {
        this.parentTagName = parentTagName;
    }

    /**
     * Gets the list of exposed methods of the webcomponent, that can be
     * called from the corresponding Java class. Each function is represented by
     * the {@link ComponentFunctionData} object.
     * 
     * @return The list of exposed methods.
     */
    public List<ComponentFunctionData> getMethods() {
        return methods;
    }

    /**
     * Sets the list of exposed methods of the webcomponent, that can be
     * called from the corresponding Java class. Each function is represented by
     * the {@link ComponentFunctionData} object.
     * 
     * @param methods
     *            The list of exposed methods.
     */
    public void setMethods(List<ComponentFunctionData> methods) {
        this.methods = methods;
    }

    /**
     * Gets the list of events triggered by the webcomponent, that can be
     * intercepted at the server side by the corresponding Java class. Each
     * event is represented by the {@link ComponentEventData} object.
     * 
     * @return The list of events.
     */
    public List<ComponentEventData> getEvents() {
        return events;
    }

    /**
     * Sets the list of events triggered by the webcomponent, that can be
     * intercepted at the server side by the corresponding Java class. Each
     * event is represented by the {@link ComponentEventData} object.
     * 
     * @param events
     *            The list of events.
     */
    public void setEvents(List<ComponentEventData> events) {
        this.events = events;
    }

    /**
     * Gets the baseUrl of the web component, which is the path to the WebComponent file.
     * <p>
     * E.g. "paper-input/paper-input.html"
     *
     * @return the baseUrl of the web component source file
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Sets the baseUrl of the web component, which is the path to the WebComponent file.
     * <p>
     * E.g. "paper-input/paper-input.html"
     *
     * @param baseUrl the base url to set
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Gets the slots of this web component.
     * <p>
     * Note that the empty string stands for the default slot that has no name. An empty list means that there are no
     * slots.
     *
     * @return a list of the names of the slots
     */
    public List<String> getSlots() {
        return slots;
    }

    /**
     * Sets the slots for this web component.
     * <p>
     * Note that the empty string stands for the default slot that has no name.
     *
     * @param slots
     *            list of the slot names
     */
    public void setSlots(List<String> slots) {
        this.slots = slots;
    }

    /**
     * Gets the mixins of this web component.
     *
     * @return a list of the mixin identifiers declared for this web component
     */
    public List<String> getMixins() {
        return mixins;
    }

    /**
     * Sets the mixins for this web component.
     *
     * @param mixins a list of mixin identifiers declared for this web component
     */
    public void setMixins(List<String> mixins) {
        this.mixins = mixins;
    }

    /**
     * Gets theme variants for the component.
     *
     * @return theme variants data for the component
     */
    public Map<String, List<String>> getVariants() {
        return variants;
    }

    /**
     * Sets theme variants for the component.
     *
     * @param variants theme variants data for the component
     */
    public void setVariants(Map<String, List<String>> variants) {
        this.variants = variants;
    }
}
