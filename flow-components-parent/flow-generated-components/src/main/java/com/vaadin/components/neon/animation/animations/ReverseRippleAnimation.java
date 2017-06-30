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

/**
 * 
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: reverse-ripple-animation#UNKNOWN",
		"Flow#0.1.12-SNAPSHOT"})
@Tag("reverse-ripple-animation")
@HtmlImport("frontend://bower_components/neon-animation/animations/reverse-ripple-animation.html")
public class ReverseRippleAnimation<R extends ReverseRippleAnimation<R>>
		extends
			Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Defines the animation timing.
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
	 * @return This instance, for method chaining.
	 */
	public R setAnimationTiming(elemental.json.JsonObject animationTiming) {
		getElement().setPropertyJson("animationTiming", animationTiming);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to determine that elements implement this behavior.
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
	 * @return This instance, for method chaining.
	 */
	public R setIsNeonAnimation(boolean isNeonAnimation) {
		getElement().setProperty("isNeonAnimation", isNeonAnimation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Cached copy of shared elements.
	 */
	public JsonObject getSharedElements() {
		return (JsonObject) getElement().getPropertyRaw("sharedElements");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Cached copy of shared elements.
	 * 
	 * @param sharedElements
	 * @return This instance, for method chaining.
	 */
	public R setSharedElements(elemental.json.JsonObject sharedElements) {
		getElement().setPropertyJson("sharedElements", sharedElements);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the animation timing by mixing in properties from {@code config}
	 * to the defaults defined by the animation.
	 * 
	 * @param config
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
	 * @param property
	 * @param value
	 */
	public void setPrefixedProperty(elemental.json.JsonObject node,
			elemental.json.JsonObject property, elemental.json.JsonObject value) {
		getElement().callFunction("setPrefixedProperty", node, property, value);
	}

	public void complete() {
		getElement().callFunction("complete");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Finds shared elements based on {@code config}.
	 * 
	 * @param config
	 */
	public void findSharedElements(elemental.json.JsonObject config) {
		getElement().callFunction("findSharedElements", config);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected R getSelf() {
		return (R) this;
	}
}