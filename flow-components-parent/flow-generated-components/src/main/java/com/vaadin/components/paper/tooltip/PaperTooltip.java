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
package com.vaadin.components.paper.tooltip;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.paper.tooltip.PaperTooltip;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design:
 * [Tooltips](https://www.google.com/design/spec/components/tooltips.html)
 * 
 * {@code <paper-tooltip>} is a label that appears on hover and focus when the
 * user hovers over an element with the cursor or with the keyboard. It will be
 * centered to an anchor element specified in the {@code for} attribute, or, if
 * that doesn't exist, centered to the parent node containing it. Note that as
 * of {@code paper-tooltip#2.0.0}, you must explicitely include the
 * {@code web-animations} polyfill if you want this element to work on browsers
 * not implementing the WebAnimations spec.
 * 
 * Example: // polyfill <link rel="import"
 * href="../../neon-animation/web-animations.html">
 * 
 * <div style="display:inline-block"> <button>Click me!</button>
 * <paper-tooltip>Tooltip text</paper-tooltip> </div>
 * 
 * <div> <button id="btn">Click me!</button> <paper-tooltip for="btn">Tooltip
 * text</paper-tooltip> </div>
 * 
 * The tooltip can be positioned on the top|bottom|left|right of the anchor
 * using the {@code position} attribute. The default position is bottom.
 * 
 * <paper-tooltip for="btn" position="left">Tooltip text</paper-tooltip>
 * <paper-tooltip for="btn" position="top">Tooltip text</paper-tooltip>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --paper-tooltip-background}
 * | The background color of the tooltip | {@code #616161}
 * {@code --paper-tooltip-opacity} | The opacity of the tooltip | {@code 0.9}
 * {@code --paper-tooltip-text-color} | The text color of the tooltip |
 * {@code white} {@code --paper-tooltip} | Mixin applied to the tooltip |
 * {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-tooltip#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-tooltip")
@HtmlImport("frontend://bower_components/paper-tooltip/paper-tooltip.html")
public class PaperTooltip extends Component implements HasStyle, HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The entry and exit animations that will be played when showing and hiding
	 * the tooltip. If you want to override this, you must ensure that your
	 * animationConfig has the exact format below.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getAnimationConfig() {
		return (JsonObject) getElement().getPropertyRaw("animationConfig");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The entry and exit animations that will be played when showing and hiding
	 * the tooltip. If you want to override this, you must ensure that your
	 * animationConfig has the exact format below.
	 * 
	 * @param animationConfig
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setAnimationConfig(
			elemental.json.JsonObject animationConfig) {
		getElement().setPropertyJson("animationConfig", animationConfig);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'entry' animation. Do not set
	 * {@code animationConfig.entry} manually if using this. The animated node
	 * is set to {@code this} if using this property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getEntryAnimation() {
		return getElement().getProperty("entryAnimation");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'entry' animation. Do not set
	 * {@code animationConfig.entry} manually if using this. The animated node
	 * is set to {@code this} if using this property.
	 * 
	 * @param entryAnimation
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setEntryAnimation(
			java.lang.String entryAnimation) {
		getElement().setProperty("entryAnimation",
				entryAnimation == null ? "" : entryAnimation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'exit' animation. Do not set
	 * {@code animationConfig.exit} manually if using this. The animated node is
	 * set to {@code this} if using this property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getExitAnimation() {
		return getElement().getProperty("exitAnimation");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Convenience property for setting an 'exit' animation. Do not set
	 * {@code animationConfig.exit} manually if using this. The animated node is
	 * set to {@code this} if using this property.
	 * 
	 * @param exitAnimation
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setExitAnimation(
			java.lang.String exitAnimation) {
		getElement().setProperty("exitAnimation",
				exitAnimation == null ? "" : exitAnimation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The id of the element that the tooltip is anchored to. This element must
	 * be a sibling of the tooltip.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getFor() {
		return getElement().getProperty("for");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The id of the element that the tooltip is anchored to. This element must
	 * be a sibling of the tooltip.
	 * 
	 * @param _for
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setFor(java.lang.String _for) {
		getElement().setProperty("for", _for == null ? "" : _for);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to true if you want to manually control when the tooltip is
	 * shown or hidden.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isManualMode() {
		return getElement().getProperty("manualMode", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to true if you want to manually control when the tooltip is
	 * shown or hidden.
	 * 
	 * @param manualMode
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setManualMode(boolean manualMode) {
		getElement().setProperty("manualMode", manualMode);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Positions the tooltip to the top, right, bottom, left of its content.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPosition() {
		return getElement().getProperty("position");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Positions the tooltip to the top, right, bottom, left of its content.
	 * 
	 * @param position
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setPosition(java.lang.String position) {
		getElement().setProperty("position", position == null ? "" : position);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, no parts of the tooltip will ever be shown offscreen.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isFitToVisibleBounds() {
		return getElement().getProperty("fitToVisibleBounds", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, no parts of the tooltip will ever be shown offscreen.
	 * 
	 * @param fitToVisibleBounds
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setFitToVisibleBounds(
			boolean fitToVisibleBounds) {
		getElement().setProperty("fitToVisibleBounds", fitToVisibleBounds);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The spacing between the top of the tooltip and the element it is anchored
	 * to.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getOffset() {
		return getElement().getProperty("offset", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The spacing between the top of the tooltip and the element it is anchored
	 * to.
	 * 
	 * @param offset
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setOffset(double offset) {
		getElement().setProperty("offset", offset);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, but left over so that it doesn't break
	 * exiting code. Please use {@code offset} instead. If both {@code offset}
	 * and {@code marginTop} are provided, {@code marginTop} will be ignored.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getMarginTop() {
		return getElement().getProperty("marginTop", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This property is deprecated, but left over so that it doesn't break
	 * exiting code. Please use {@code offset} instead. If both {@code offset}
	 * and {@code marginTop} are provided, {@code marginTop} will be ignored.
	 * 
	 * @param marginTop
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setMarginTop(double marginTop) {
		getElement().setProperty("marginTop", marginTop);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The delay that will be applied before the {@code entry} animation is
	 * played when showing the tooltip.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getAnimationDelay() {
		return getElement().getProperty("animationDelay", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The delay that will be applied before the {@code entry} animation is
	 * played when showing the tooltip.
	 * 
	 * @param animationDelay
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setAnimationDelay(double animationDelay) {
		getElement().setProperty("animationDelay", animationDelay);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the target element that this tooltip is anchored to. It is either
	 * the element given by the {@code for} attribute, or the immediate parent
	 * of the tooltip.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getTarget() {
		return (JsonObject) getElement().getPropertyRaw("target");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the target element that this tooltip is anchored to. It is either
	 * the element given by the {@code for} attribute, or the immediate parent
	 * of the tooltip.
	 * 
	 * @param target
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperTooltip> R setTarget(elemental.json.JsonObject target) {
		getElement().setPropertyJson("target", target);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An element implementing {@code Polymer.NeonAnimationRunnerBehavior} calls
	 * this method to configure an animation with an optional type. Elements
	 * implementing {@code Polymer.NeonAnimatableBehavior} should define the
	 * property {@code animationConfig}, which is either a configuration object
	 * or a map of animation type to array of configuration objects.
	 * 
	 * @param type
	 *            Missing documentation!
	 */
	public void getAnimationConfig(elemental.json.JsonObject type) {
		getElement().callFunction("getAnimationConfig", type);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Plays an animation with an optional {@code type}.
	 * 
	 * @param type
	 *            Missing documentation!
	 * @param cookie
	 *            Missing documentation!
	 */
	public void playAnimation(elemental.json.JsonObject type,
			elemental.json.JsonObject cookie) {
		getElement().callFunction("playAnimation", type, cookie);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Cancels the currently running animations.
	 */
	public void cancelAnimation() {
		getElement().callFunction("cancelAnimation");
	}

	public void show() {
		getElement().callFunction("show");
	}

	public void hide() {
		getElement().callFunction("hide");
	}

	public void updatePosition() {
		getElement().callFunction("updatePosition");
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends PaperTooltip> R getSelf() {
		return (R) this;
	}
}