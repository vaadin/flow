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
package com.vaadin.generated.paper.ripple;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.generated.paper.ripple.GeneratedPaperRipple;
import elemental.json.JsonArray;
import com.vaadin.components.NotSupported;
import com.vaadin.components.JsonSerializable;
import com.vaadin.annotations.EventData;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design: [Surface
 * reaction](https://www.google.com/design/spec/animation
 * /responsive-interaction.html#responsive-interaction-surface-reaction)
 * 
 * {@code paper-ripple} provides a visual effect that other paper elements can
 * use to simulate a rippling effect emanating from the point of contact. The
 * effect can be visualized as a concentric circle with motion.
 * 
 * Example:
 * 
 * <div style="position:relative"> <paper-ripple></paper-ripple> </div>
 * 
 * Note, it's important that the parent container of the ripple be relative
 * position, otherwise the ripple will emanate outside of the desired container.
 * 
 * {@code paper-ripple} listens to "mousedown" and "mouseup" events so it would
 * display ripple effect when touches on it. You can also defeat the default
 * behavior and manually route the down and up actions to the ripple element.
 * Note that it is important if you call {@code downAction()} you will have to
 * make sure to call {@code upAction()} so that {@code paper-ripple} would end
 * the animation loop.
 * 
 * Example:
 * 
 * <paper-ripple id="ripple" style="pointer-events: none;"></paper-ripple> ...
 * downAction: function(e) { this.$.ripple.downAction(e.detail); }, upAction:
 * function(e) { this.$.ripple.upAction(); }
 * 
 * Styling ripple effect:
 * 
 * Use CSS color property to style the ripple:
 * 
 * paper-ripple { color: #4285f4; }
 * 
 * Note that CSS color property is inherited so it is not required to set it on
 * the {@code paper-ripple} element directly.
 * 
 * By default, the ripple is centered on the point of contact. Apply the
 * {@code recenters} attribute to have the ripple grow toward the center of its
 * container.
 * 
 * <paper-ripple recenters></paper-ripple>
 * 
 * You can also center the ripple inside its container from the start.
 * 
 * <paper-ripple center></paper-ripple>
 * 
 * Apply {@code circle} class to make the rippling effect within a circle.
 * 
 * <paper-ripple class="circle"></paper-ripple>
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: paper-ripple#2.0.1", "Flow#0.1.14-SNAPSHOT"})
@Tag("paper-ripple")
@HtmlImport("frontend://bower_components/paper-ripple/paper-ripple.html")
public class GeneratedPaperRipple extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getKeyEventTarget() {
		return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * {@code null} to disable the listeners.
	 * 
	 * @param keyEventTarget
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setKeyEventTarget(
			elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isStopKeyboardEventPropagation() {
		return getElement().getProperty("stopKeyboardEventPropagation", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
	 * 
	 * @param stopKeyboardEventPropagation
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setKeyBindings(
			elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial opacity set on the wave.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getInitialOpacity() {
		return getElement().getProperty("initialOpacity", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial opacity set on the wave.
	 * 
	 * @param initialOpacity
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setInitialOpacity(
			double initialOpacity) {
		getElement().setProperty("initialOpacity", initialOpacity);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * How fast (opacity per second) the wave fades out.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getOpacityDecayVelocity() {
		return getElement().getProperty("opacityDecayVelocity", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * How fast (opacity per second) the wave fades out.
	 * 
	 * @param opacityDecayVelocity
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setOpacityDecayVelocity(
			double opacityDecayVelocity) {
		getElement().setProperty("opacityDecayVelocity", opacityDecayVelocity);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ripples will exhibit a gravitational pull towards the center of
	 * their container as they fade away.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRecenters() {
		return getElement().getProperty("recenters", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ripples will exhibit a gravitational pull towards the center of
	 * their container as they fade away.
	 * 
	 * @param recenters
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setRecenters(boolean recenters) {
		getElement().setProperty("recenters", recenters);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ripples will center inside its container
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isCenter() {
		return getElement().getProperty("center", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ripples will center inside its container
	 * 
	 * @param center
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setCenter(boolean center) {
		getElement().setProperty("center", center);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A list of the visual ripples.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonArray getRipples() {
		return (JsonArray) getElement().getPropertyRaw("ripples");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A list of the visual ripples.
	 * 
	 * @param ripples
	 *            the JsonArray value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setRipples(
			elemental.json.JsonArray ripples) {
		getElement().setPropertyJson("ripples", ripples);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when there are visible ripples animating within the element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAnimating() {
		return getElement().getProperty("animating", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will remain in the "down" state until
	 * {@code holdDown} is set to false again.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isHoldDown() {
		return getElement().getProperty("holdDown", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will remain in the "down" state until
	 * {@code holdDown} is set to false again.
	 * 
	 * @param holdDown
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setHoldDown(boolean holdDown) {
		getElement().setProperty("holdDown", holdDown);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will not generate a ripple effect via pointer
	 * interaction. Calling ripple's imperative api like {@code simulatedRipple}
	 * will still generate the ripple effect.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoink() {
		return getElement().getProperty("noink", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will not generate a ripple effect via pointer
	 * interaction. Calling ripple's imperative api like {@code simulatedRipple}
	 * will still generate the ripple effect.
	 * 
	 * @param noink
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setNoink(boolean noink) {
		getElement().setProperty("noink", noink);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getTarget() {
		return (JsonObject) getElement().getPropertyRaw("target");
	}

	/**
	 * @param target
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setTarget(
			elemental.json.JsonObject target) {
		getElement().setPropertyJson("target", target);
		return getSelf();
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getShouldKeepAnimating() {
		return (JsonObject) getElement().getPropertyRaw("shouldKeepAnimating");
	}

	/**
	 * @param shouldKeepAnimating
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedPaperRipple> R setShouldKeepAnimating(
			elemental.json.JsonObject shouldKeepAnimating) {
		getElement()
				.setPropertyJson("shouldKeepAnimating", shouldKeepAnimating);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to imperatively add a key binding to the implementing
	 * element. This is the imperative equivalent of declaring a keybinding in
	 * the {@code keyBindings} prototype property.
	 * 
	 * @param eventString
	 *            Missing documentation!
	 * @param handlerName
	 *            Missing documentation!
	 */
	public void addOwnKeyBinding(java.lang.String eventString,
			java.lang.String handlerName) {
		getElement().callFunction("addOwnKeyBinding", eventString, handlerName);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When called, will remove all imperatively-added key bindings.
	 */
	public void removeOwnKeyBindings() {
		getElement().callFunction("removeOwnKeyBindings");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if a keyboard event matches {@code eventString}.
	 * 
	 * @param event
	 *            Missing documentation!
	 * @param eventString
	 *            Missing documentation!
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
	}

	public void simulatedRipple() {
		getElement().callFunction("simulatedRipple");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple down effect via a UI event, respecting the
	 * {@code noink} property.
	 * 
	 * @param event
	 *            Missing documentation!
	 */
	public void uiDownAction(elemental.json.JsonObject event) {
		getElement().callFunction("uiDownAction", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple down effect via a UI event, not* respecting the
	 * {@code noink} property.
	 * 
	 * @param event
	 *            Missing documentation!
	 */
	public void downAction(elemental.json.JsonObject event) {
		getElement().callFunction("downAction", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple up effect via a UI event, respecting the {@code noink}
	 * property.
	 * 
	 * @param event
	 *            Missing documentation!
	 */
	public void uiUpAction(elemental.json.JsonObject event) {
		getElement().callFunction("uiUpAction", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple up effect via a UI event, not* respecting the
	 * {@code noink} property.
	 * 
	 * @param event
	 *            Missing documentation!
	 */
	public void upAction(elemental.json.JsonObject event) {
		getElement().callFunction("upAction", event);
	}

	public void onAnimationComplete() {
		getElement().callFunction("onAnimationComplete");
	}

	public void addRipple() {
		getElement().callFunction("addRipple");
	}

	/**
	 * @param ripple
	 *            Missing documentation!
	 */
	public void removeRipple(elemental.json.JsonObject ripple) {
		getElement().callFunction("removeRipple", ripple);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This conflicts with Element#antimate().
	 * https://developer.mozilla.org/en-US/docs/Web/API/Element/animate
	 */
	public void animate() {
		getElement().callFunction("animate");
	}

	/**
	 * Class that encapsulates the data received on the 'detail' property of
	 * @{link TransitionendEvent} events, from the @{link GeneratedPaperRipple}
	 * component.
	 */
	public static class TransitionendDetail implements JsonSerializable {
		private JsonObject internalObject;

		public JsonObject getNode() {
			return internalObject.getObject("node");
		}

		public TransitionendDetail setNode(elemental.json.JsonObject node) {
			this.internalObject.put("node", node);
			return this;
		}

		@Override
		public JsonObject toJson() {
			return internalObject;
		}

		@Override
		public TransitionendDetail readJson(elemental.json.JsonObject value) {
			internalObject = value;
			return this;
		}
	}

	@DomEvent("transitionend")
	public static class TransitionendEvent
			extends
				ComponentEvent<GeneratedPaperRipple> {
		private final JsonObject detail;

		public TransitionendEvent(GeneratedPaperRipple source,
				boolean fromClient,
				@EventData("event.detail") elemental.json.JsonObject detail) {
			super(source, fromClient);
			this.detail = detail;
		}

		public TransitionendDetail getDetail() {
			return new TransitionendDetail().readJson(detail);
		}
	}

	public Registration addTransitionendListener(
			ComponentEventListener<TransitionendEvent> listener) {
		return addListener(TransitionendEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends GeneratedPaperRipple> R getSelf() {
		return (R) this;
	}
}