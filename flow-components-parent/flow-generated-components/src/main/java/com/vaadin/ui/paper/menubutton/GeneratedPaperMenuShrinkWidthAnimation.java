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
package com.vaadin.ui.paper.menubutton;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-menu-shrink-width-animation#UNKNOWN",
        "Flow#1.0-SNAPSHOT" })
@Tag("paper-menu-shrink-width-animation")
@HtmlImport("frontend://bower_components/paper-menu-button/paper-menu-button-animations.html")
public class GeneratedPaperMenuShrinkWidthAnimation<R extends GeneratedPaperMenuShrinkWidthAnimation<R>>
        extends Component implements ComponentSupplier<R>, HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Defines the animation timing.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code animationTiming} property from the webcomponent
     */
    protected JsonObject protectedGetAnimationTiming() {
        return (JsonObject) getElement().getPropertyRaw("animationTiming");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Defines the animation timing.
     * </p>
     * 
     * @param animationTiming
     *            the JsonObject value to set
     */
    protected void setAnimationTiming(
            elemental.json.JsonObject animationTiming) {
        getElement().setPropertyJson("animationTiming", animationTiming);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Can be used to determine that elements implement this behavior.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code isNeonAnimation} property from the webcomponent
     */
    public boolean isNeonAnimation() {
        return getElement().getProperty("isNeonAnimation", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Can be used to determine that elements implement this behavior.
     * </p>
     * 
     * @param isNeonAnimation
     *            the boolean value to set
     */
    public void setIsNeonAnimation(boolean isNeonAnimation) {
        getElement().setProperty("isNeonAnimation", isNeonAnimation);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the animation timing by mixing in properties from {@code config}
     * to the defaults defined by the animation.
     * </p>
     * 
     * @param config
     *            Missing documentation!
     */
    protected void timingFromConfig(elemental.json.JsonObject config) {
        getElement().callFunction("timingFromConfig", config);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Sets {@code transform} and {@code transformOrigin} properties along with
     * the prefixed versions.
     * </p>
     * 
     * @param node
     *            Missing documentation!
     * @param property
     *            Missing documentation!
     * @param value
     *            Missing documentation!
     */
    protected void setPrefixedProperty(elemental.json.JsonObject node,
            elemental.json.JsonObject property,
            elemental.json.JsonObject value) {
        getElement().callFunction("setPrefixedProperty", node, property, value);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Called when the animation finishes.
     * </p>
     */
    public void complete() {
        getElement().callFunction("complete");
    }
}