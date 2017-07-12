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
package com.vaadin.components.neon.animation.animations;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.neon.animation.animations.SlideUpAnimation;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <slide-up-animation>} animates the transform of an element from
 * {@code translateY(0)} to {@code translateY(-100%)}. The
 * {@code transformOrigin} defaults to {@code 50% 0}.
 * 
 * Configuration: {@code }` { name: 'slide-up-animation', node: <node>,
 * transformOrigin: <transform-origin>, timing: <animation-timing> } {@code }`
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: slide-up-animation#UNKNOWN", "Flow#0.1.13-SNAPSHOT"})
@Tag("slide-up-animation")
@HtmlImport("frontend://bower_components/neon-animation/animations/slide-up-animation.html")
public class SlideUpAnimation extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Defines the animation timing.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getAnimationTiming() {
		return (JsonObject) getElement().getPropertyRaw("animationTiming");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Defines the animation timing.
	 * 
	 * @param animationTiming
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends SlideUpAnimation> R setAnimationTiming(
			elemental.json.JsonObject animationTiming) {
		getElement().setPropertyJson("animationTiming", animationTiming);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to determine that elements implement this behavior.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isIsNeonAnimation() {
		return getElement().getProperty("isNeonAnimation", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to determine that elements implement this behavior.
	 * 
	 * @param isNeonAnimation
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends SlideUpAnimation> R setIsNeonAnimation(
			boolean isNeonAnimation) {
		getElement().setProperty("isNeonAnimation", isNeonAnimation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the animation timing by mixing in properties from {@code config}
	 * to the defaults defined by the animation.
	 * 
	 * @param config
	 *            Missing documentation!
	 */
	public void timingFromConfig(elemental.json.JsonObject config) {
		getElement().callFunction("timingFromConfig", config);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Sets {@code transform} and {@code transformOrigin} properties along with
	 * the prefixed versions.
	 * 
	 * @param node
	 *            Missing documentation!
	 * @param property
	 *            Missing documentation!
	 * @param value
	 *            Missing documentation!
	 */
	public void setPrefixedProperty(elemental.json.JsonObject node,
			elemental.json.JsonObject property, elemental.json.JsonObject value) {
		getElement().callFunction("setPrefixedProperty", node, property, value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Called when the animation finishes.
	 */
	public void complete() {
		getElement().callFunction("complete");
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends SlideUpAnimation> R getSelf() {
		return (R) this;
	}
}