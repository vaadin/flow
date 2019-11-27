/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.component.combobox;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import elemental.json.JsonObject;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * Element for internal use only.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.ComboBoxDropdownElement#UNKNOWN",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-combo-box-dropdown")
@HtmlImport("frontend://bower_components/vaadin-combo-box/src/vaadin-combo-box-dropdown.html")
public abstract class GeneratedVaadinComboBoxDropdown<R extends GeneratedVaadinComboBoxDropdown<R>>
        extends Component implements HasStyle {

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code opened} property from the webcomponent
     */
    protected boolean isOpenedBoolean() {
        return getElement().getProperty("opened", false);
    }

    /**
     * @param opened
     *            the boolean value to set
     */
    protected void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * This property is synchronized automatically from client side when a
     * 'template-changed' event happens.
     * 
     * @return the {@code template} property from the webcomponent
     */
    @Synchronize(property = "template", value = "template-changed")
    protected JsonObject getTemplateJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("template");
    }

    /**
     * @param template
     *            the JsonObject value to set
     */
    protected void setTemplate(JsonObject template) {
        getElement().setPropertyJson("template", template);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element to position/align the dropdown by.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code positionTarget} property from the webcomponent
     */
    protected JsonObject getPositionTargetJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("positionTarget");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element to position/align the dropdown by.
     * </p>
     * 
     * @param positionTarget
     *            the JsonObject value to set
     */
    protected void setPositionTarget(JsonObject positionTarget) {
        getElement().setPropertyJson("positionTarget", positionTarget);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If {@code true}, overlay is aligned above the {@code positionTarget}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alignedAbove} property from the webcomponent
     */
    protected boolean isAlignedAboveBoolean() {
        return getElement().getProperty("alignedAbove", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If {@code true}, overlay is aligned above the {@code positionTarget}
     * </p>
     * 
     * @param alignedAbove
     *            the boolean value to set
     */
    protected void setAlignedAbove(boolean alignedAbove) {
        getElement().setProperty("alignedAbove", alignedAbove);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to propagate the {@code theme} attribute from the host element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code theme} property from the webcomponent
     */
    protected String getThemeString() {
        return getElement().getProperty("theme");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to propagate the {@code theme} attribute from the host element.
     * </p>
     * 
     * @param theme
     *            the String value to set
     */
    protected void setTheme(String theme) {
        getElement().setProperty("theme", theme == null ? "" : theme);
    }

    protected void notifyResize() {
        getElement().callFunction("notifyResize");
    }

    @DomEvent("vaadin-combo-box-dropdown-closed")
    public static class VaadinComboBoxDropdownClosedEvent<R extends GeneratedVaadinComboBoxDropdown<R>>
            extends ComponentEvent<R> {
        public VaadinComboBoxDropdownClosedEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code vaadin-combo-box-dropdown-closed} events fired
     * by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addVaadinComboBoxDropdownClosedListener(
            ComponentEventListener<VaadinComboBoxDropdownClosedEvent<R>> listener) {
        return addListener(VaadinComboBoxDropdownClosedEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("vaadin-combo-box-dropdown-opened")
    public static class VaadinComboBoxDropdownOpenedEvent<R extends GeneratedVaadinComboBoxDropdown<R>>
            extends ComponentEvent<R> {
        public VaadinComboBoxDropdownOpenedEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code vaadin-combo-box-dropdown-opened} events fired
     * by the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addVaadinComboBoxDropdownOpenedListener(
            ComponentEventListener<VaadinComboBoxDropdownOpenedEvent<R>> listener) {
        return addListener(VaadinComboBoxDropdownOpenedEvent.class,
                (ComponentEventListener) listener);
    }

    public static class TemplateChangeEvent<R extends GeneratedVaadinComboBoxDropdown<R>>
            extends ComponentEvent<R> {
        private final JsonObject template;

        public TemplateChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.template = source.getTemplateJsonObject();
        }

        public JsonObject getTemplate() {
            return template;
        }
    }

    /**
     * Adds a listener for {@code template-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addTemplateChangeListener(
            ComponentEventListener<TemplateChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("template",
                        event -> listener.onComponentEvent(
                                new TemplateChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}