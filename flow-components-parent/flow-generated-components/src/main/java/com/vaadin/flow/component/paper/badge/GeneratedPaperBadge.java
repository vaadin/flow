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
package com.vaadin.flow.component.paper.badge;

import javax.annotation.Generated;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentSupplier;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;

import elemental.json.JsonObject;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-badge#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-badge")
@HtmlImport("frontend://bower_components/paper-badge/paper-badge.html")
public class GeneratedPaperBadge<R extends GeneratedPaperBadge<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The id of the element that the badge is anchored to. This element must be
     * a sibling of the badge.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code for} property from the webcomponent
     */
    public String getFor() {
        return getElement().getProperty("for");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The id of the element that the badge is anchored to. This element must be
     * a sibling of the badge.
     * </p>
     * 
     * @param _for
     *            the String value to set
     */
    public void setFor(String _for) {
        getElement().setProperty("for", _for == null ? "" : _for);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label displayed in the badge. The label is centered, and ideally
     * should have very few characters.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code label} property from the webcomponent
     */
    public String getLabel() {
        return getElement().getProperty("label");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The label displayed in the badge. The label is centered, and ideally
     * should have very few characters.
     * </p>
     * 
     * @param label
     *            the String value to set
     */
    public void setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An iron-icon ID. When given, the badge content will use an
     * {@code <iron-icon>} element displaying the given icon ID rather than the
     * label text. However, the label text will still be used for accessibility
     * purposes.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code icon} property from the webcomponent
     */
    public String getIcon() {
        return getElement().getProperty("icon");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * An iron-icon ID. When given, the badge content will use an
     * {@code <iron-icon>} element displaying the given icon ID rather than the
     * label text. However, the label text will still be used for accessibility
     * purposes.
     * </p>
     * 
     * @param icon
     *            the String value to set
     */
    public void setIcon(String icon) {
        getElement().setProperty("icon", icon == null ? "" : icon);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the target element that this badge is anchored to. It is either
     * the element given by the {@code for} attribute, or the immediate parent
     * of the badge.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code target} property from the webcomponent
     */
    protected JsonObject protectedGetTarget() {
        return (JsonObject) getElement().getPropertyRaw("target");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the target element that this badge is anchored to. It is either
     * the element given by the {@code for} attribute, or the immediate parent
     * of the badge.
     * </p>
     * 
     * @param target
     *            the JsonObject value to set
     */
    protected void setTarget(JsonObject target) {
        getElement().setPropertyJson("target", target);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Can be called to manually notify a resizable and its descendant
     * resizables of a resize change.
     * </p>
     */
    public void notifyResize() {
        getElement().callFunction("notifyResize");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to assign the closest resizable ancestor to this resizable if the
     * ancestor detects a request for notifications.
     * </p>
     * 
     * @param parentResizable
     *            Missing documentation!
     */
    protected void assignParentResizable(JsonObject parentResizable) {
        getElement().callFunction("assignParentResizable", parentResizable);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to remove a resizable descendant from the list of descendants that
     * should be notified of a resize change.
     * </p>
     * 
     * @param target
     *            Missing documentation!
     */
    protected void stopResizeNotificationsFor(JsonObject target) {
        getElement().callFunction("stopResizeNotificationsFor", target);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * This method can be overridden to filter nested elements that should or
     * should not be notified by the current element. Return true if an element
     * should be notified, or false if it should not be notified.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>boolean</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param element
     *            A candidate descendant element that implements
     *            `IronResizableBehavior`.
     */
    @NotSupported
    protected void resizerShouldNotify(JsonObject element) {
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Repositions the badge relative to its anchor element. This is called
     * automatically when the badge is attached or an {@code iron-resize} event
     * is fired (for exmaple if the window has resized, or your target is a
     * custom element that implements IronResizableBehavior).
     * </p>
     * <p>
     * You should call this in all other cases when the achor's position might
     * have changed (for example, if it's visibility has changed, or you've
     * manually done a page re-layout).
     * </p>
     */
    public void updatePosition() {
        getElement().callFunction("updatePosition");
    }
}