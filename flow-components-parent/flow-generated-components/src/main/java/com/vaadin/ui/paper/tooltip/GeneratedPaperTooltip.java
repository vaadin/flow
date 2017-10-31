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
package com.vaadin.ui.paper.tooltip;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * Material design: <a
 * href="https://www.google.com/design/spec/components/tooltips.html"
 * >Tooltips</a>
 * </p>
 * <p>
 * {@code <paper-tooltip>} is a label that appears on hover and focus when the
 * user hovers over an element with the cursor or with the keyboard. It will be
 * centered to an anchor element specified in the {@code for} attribute, or, if
 * that doesn't exist, centered to the parent node containing it. Note that as
 * of {@code paper-tooltip#2.0.0}, you must explicitely include the
 * {@code web-animations} polyfill if you want this element to work on browsers
 * not implementing the WebAnimations spec.
 * </p>
 * <p>
 * Example: // polyfill <link rel="import"
 * href="../../neon-animation/web-animations.html">
 * </p>
 * 
 * <pre>
 * <code>&lt;div style=&quot;display:inline-block&quot;&gt;
 *   &lt;button&gt;Click me!&lt;/button&gt;
 *   &lt;paper-tooltip&gt;Tooltip text&lt;/paper-tooltip&gt;
 * &lt;/div&gt;
 * 
 * &lt;div&gt;
 *   &lt;button id=&quot;btn&quot;&gt;Click me!&lt;/button&gt;
 *   &lt;paper-tooltip for=&quot;btn&quot;&gt;Tooltip text&lt;/paper-tooltip&gt;
 * &lt;/div&gt;
 * </code>
 * </pre>
 * <p>
 * The tooltip can be positioned on the top|bottom|left|right of the anchor
 * using the {@code position} attribute. The default position is bottom.
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-tooltip for=&quot;btn&quot; position=&quot;left&quot;&gt;Tooltip text&lt;/paper-tooltip&gt;
 * &lt;paper-tooltip for=&quot;btn&quot; position=&quot;top&quot;&gt;Tooltip text&lt;/paper-tooltip&gt;
 * </code>
 * </pre>
 * 
 * <h3>Styling</h3>
 * <p>
 * The following custom properties and mixins are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --paper-tooltip-background}</td>
 * <td>The background color of the tooltip</td>
 * <td>{@code #616161}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-tooltip-opacity}</td>
 * <td>The opacity of the tooltip</td>
 * <td>{@code 0.9}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-tooltip-text-color}</td>
 * <td>The text color of the tooltip</td>
 * <td>{@code white}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-tooltip}</td>
 * <td>Mixin applied to the tooltip</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: paper-tooltip#2.0.0", "Flow#1.0-SNAPSHOT"})
@Tag("paper-tooltip")
@HtmlImport("frontend://bower_components/paper-tooltip/paper-tooltip.html")
public class GeneratedPaperTooltip<R extends GeneratedPaperTooltip<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle, HasComponents {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The entry and exit animations that will be played when showing and hiding
	 * the tooltip. If you want to override this, you must ensure that your
	 * animationConfig has the exact format below.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code animationConfig} property from the webcomponent
	 */
	protected JsonObject protectedGetAnimationConfig() {
		return (JsonObject) getElement().getPropertyRaw("animationConfig");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The entry and exit animations that will be played when showing and hiding
	 * the tooltip. If you want to override this, you must ensure that your
	 * animationConfig has the exact format below.
	 * </p>
	 * 
	 * @param animationConfig
	 *            the JsonObject value to set
	 */
	protected void setAnimationConfig(elemental.json.JsonObject animationConfig) {
		getElement().setPropertyJson("animationConfig", animationConfig);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Convenience property for setting an 'entry' animation. Do not set
	 * {@code animationConfig.entry} manually if using this. The animated node
	 * is set to {@code this} if using this property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code entryAnimation} property from the webcomponent
	 */
	public String getEntryAnimation() {
		return getElement().getProperty("entryAnimation");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Convenience property for setting an 'entry' animation. Do not set
	 * {@code animationConfig.entry} manually if using this. The animated node
	 * is set to {@code this} if using this property.
	 * </p>
	 * 
	 * @param entryAnimation
	 *            the String value to set
	 */
	public void setEntryAnimation(java.lang.String entryAnimation) {
		getElement().setProperty("entryAnimation",
				entryAnimation == null ? "" : entryAnimation);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Convenience property for setting an 'exit' animation. Do not set
	 * {@code animationConfig.exit} manually if using this. The animated node is
	 * set to {@code this} if using this property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code exitAnimation} property from the webcomponent
	 */
	public String getExitAnimation() {
		return getElement().getProperty("exitAnimation");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Convenience property for setting an 'exit' animation. Do not set
	 * {@code animationConfig.exit} manually if using this. The animated node is
	 * set to {@code this} if using this property.
	 * </p>
	 * 
	 * @param exitAnimation
	 *            the String value to set
	 */
	public void setExitAnimation(java.lang.String exitAnimation) {
		getElement().setProperty("exitAnimation",
				exitAnimation == null ? "" : exitAnimation);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The id of the element that the tooltip is anchored to. This element must
	 * be a sibling of the tooltip.
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
	 * The id of the element that the tooltip is anchored to. This element must
	 * be a sibling of the tooltip.
	 * </p>
	 * 
	 * @param _for
	 *            the String value to set
	 */
	public void setFor(java.lang.String _for) {
		getElement().setProperty("for", _for == null ? "" : _for);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set this to true if you want to manually control when the tooltip is
	 * shown or hidden.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code manualMode} property from the webcomponent
	 */
	public boolean isManualMode() {
		return getElement().getProperty("manualMode", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set this to true if you want to manually control when the tooltip is
	 * shown or hidden.
	 * </p>
	 * 
	 * @param manualMode
	 *            the boolean value to set
	 */
	public void setManualMode(boolean manualMode) {
		getElement().setProperty("manualMode", manualMode);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Positions the tooltip to the top, right, bottom, left of its content.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code position} property from the webcomponent
	 */
	public String getPosition() {
		return getElement().getProperty("position");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Positions the tooltip to the top, right, bottom, left of its content.
	 * </p>
	 * 
	 * @param position
	 *            the String value to set
	 */
	public void setPosition(java.lang.String position) {
		getElement().setProperty("position", position == null ? "" : position);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * If true, no parts of the tooltip will ever be shown offscreen.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code fitToVisibleBounds} property from the webcomponent
	 */
	public boolean isFitToVisibleBounds() {
		return getElement().getProperty("fitToVisibleBounds", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * If true, no parts of the tooltip will ever be shown offscreen.
	 * </p>
	 * 
	 * @param fitToVisibleBounds
	 *            the boolean value to set
	 */
	public void setFitToVisibleBounds(boolean fitToVisibleBounds) {
		getElement().setProperty("fitToVisibleBounds", fitToVisibleBounds);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The spacing between the top of the tooltip and the element it is anchored
	 * to.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code offset} property from the webcomponent
	 */
	public double getOffset() {
		return getElement().getProperty("offset", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The spacing between the top of the tooltip and the element it is anchored
	 * to.
	 * </p>
	 * 
	 * @param offset
	 *            the double value to set
	 */
	public void setOffset(double offset) {
		getElement().setProperty("offset", offset);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This property is deprecated, but left over so that it doesn't break
	 * exiting code. Please use {@code offset} instead. If both {@code offset}
	 * and {@code marginTop} are provided, {@code marginTop} will be ignored.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code marginTop} property from the webcomponent
	 */
	public double getMarginTop() {
		return getElement().getProperty("marginTop", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This property is deprecated, but left over so that it doesn't break
	 * exiting code. Please use {@code offset} instead. If both {@code offset}
	 * and {@code marginTop} are provided, {@code marginTop} will be ignored.
	 * </p>
	 * 
	 * @param marginTop
	 *            the double value to set
	 */
	public void setMarginTop(double marginTop) {
		getElement().setProperty("marginTop", marginTop);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The delay that will be applied before the {@code entry} animation is
	 * played when showing the tooltip.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code animationDelay} property from the webcomponent
	 */
	public double getAnimationDelay() {
		return getElement().getProperty("animationDelay", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The delay that will be applied before the {@code entry} animation is
	 * played when showing the tooltip.
	 * </p>
	 * 
	 * @param animationDelay
	 *            the double value to set
	 */
	public void setAnimationDelay(double animationDelay) {
		getElement().setProperty("animationDelay", animationDelay);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Returns the target element that this tooltip is anchored to. It is either
	 * the element given by the {@code for} attribute, or the immediate parent
	 * of the tooltip.
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
	 * Returns the target element that this tooltip is anchored to. It is either
	 * the element given by the {@code for} attribute, or the immediate parent
	 * of the tooltip.
	 * </p>
	 * 
	 * @param target
	 *            the JsonObject value to set
	 */
	protected void setTarget(elemental.json.JsonObject target) {
		getElement().setPropertyJson("target", target);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * An element implementing {@code Polymer.NeonAnimationRunnerBehavior} calls
	 * this method to configure an animation with an optional type. Elements
	 * implementing {@code Polymer.NeonAnimatableBehavior} should define the
	 * property {@code animationConfig}, which is either a configuration object
	 * or a map of animation type to array of configuration objects.
	 * </p>
	 * 
	 * @param type
	 *            Missing documentation!
	 */
	protected void getAnimationConfig(elemental.json.JsonObject type) {
		getElement().callFunction("getAnimationConfig", type);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Plays an animation with an optional {@code type}.
	 * </p>
	 * 
	 * @param type
	 *            Missing documentation!
	 * @param cookie
	 *            Missing documentation!
	 */
	protected void playAnimation(elemental.json.JsonObject type,
			elemental.json.JsonObject cookie) {
		getElement().callFunction("playAnimation", type, cookie);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Cancels the currently running animations.
	 * </p>
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
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedPaperTooltip(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedPaperTooltip() {
	}
}