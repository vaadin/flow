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
package com.vaadin.flow.component.grid;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import elemental.json.JsonObject;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * This Element is used internally by vaadin-grid.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: GridOuterScrollerElement#UNKNOWN", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-outer-scroller")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-outer-scroller.html")
public abstract class GeneratedVaadinGridOuterScroller<R extends GeneratedVaadinGridOuterScroller<R>>
        extends Component implements HasStyle {

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code scrollTarget} property from the webcomponent
     */
    protected JsonObject getScrollTargetJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("scrollTarget");
    }

    /**
     * @param scrollTarget
     *            the JsonObject value to set
     */
    protected void setScrollTarget(JsonObject scrollTarget) {
        getElement().setPropertyJson("scrollTarget", scrollTarget);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code scrollHandler} property from the webcomponent
     */
    protected JsonObject getScrollHandlerJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("scrollHandler");
    }

    /**
     * @param scrollHandler
     *            the JsonObject value to set
     */
    protected void setScrollHandler(JsonObject scrollHandler) {
        getElement().setPropertyJson("scrollHandler", scrollHandler);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code passthrough} property from the webcomponent
     */
    protected boolean isPassthroughBoolean() {
        return getElement().getProperty("passthrough", false);
    }

    /**
     * @param passthrough
     *            the boolean value to set
     */
    protected void setPassthrough(boolean passthrough) {
        getElement().setProperty("passthrough", passthrough);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code outerScrolling} property from the webcomponent
     */
    protected boolean isOuterScrollingBoolean() {
        return getElement().getProperty("outerScrolling", false);
    }

    /**
     * @param outerScrolling
     *            the boolean value to set
     */
    protected void setOuterScrolling(boolean outerScrolling) {
        getElement().setProperty("outerScrolling", outerScrolling);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code noScrollbars} property from the webcomponent
     */
    protected boolean isNoScrollbarsBoolean() {
        return getElement().getProperty("noScrollbars", false);
    }

    /**
     * @param noScrollbars
     *            the boolean value to set
     */
    protected void setNoScrollbars(boolean noScrollbars) {
        getElement().setProperty("noScrollbars", noScrollbars);
    }

    protected void syncOuterScroller() {
        getElement().callFunction("syncOuterScroller");
    }
}