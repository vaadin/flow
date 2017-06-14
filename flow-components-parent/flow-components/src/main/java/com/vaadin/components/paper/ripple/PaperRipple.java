package com.vaadin.components.paper.ripple;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import elemental.json.JsonObject;
import elemental.json.JsonArray;
import com.vaadin.shared.Registration;
import com.vaadin.flow.dom.DomEventListener;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design: [Surface
 * reaction](https://www.google.com/design/spec/animation
 * /responsive-interaction.html#responsive-interaction-surface-reaction)
 * 
 * `paper-ripple` provides a visual effect that other paper elements can use to
 * simulate a rippling effect emanating from the point of contact. The effect
 * can be visualized as a concentric circle with motion.
 * 
 * Example:
 * 
 * <div style="position:relative"> <paper-ripple></paper-ripple> </div>
 * 
 * Note, it's important that the parent container of the ripple be relative
 * position, otherwise the ripple will emanate outside of the desired container.
 * 
 * `paper-ripple` listens to "mousedown" and "mouseup" events so it would
 * display ripple effect when touches on it. You can also defeat the default
 * behavior and manually route the down and up actions to the ripple element.
 * Note that it is important if you call `downAction()` you will have to make
 * sure to call `upAction()` so that `paper-ripple` would end the animation
 * loop.
 * 
 * Example:
 * 
 * <paper-ripple id="ripple" style="pointer-events: none;"></paper-ripple> ...
 * downAction: function(e) { this.$.ripple.downAction({detail: {x: e.x, y:
 * e.y}}); }, upAction: function(e) { this.$.ripple.upAction(); }
 * 
 * Styling ripple effect:
 * 
 * Use CSS color property to style the ripple:
 * 
 * paper-ripple { color: #4285f4; }
 * 
 * Note that CSS color property is inherited so it is not required to set it on
 * the `paper-ripple` element directly.
 * 
 * By default, the ripple is centered on the point of contact. Apply the
 * `recenters` attribute to have the ripple grow toward the center of its
 * container.
 * 
 * <paper-ripple recenters></paper-ripple>
 * 
 * You can also center the ripple inside its container from the start.
 * 
 * <paper-ripple center></paper-ripple>
 * 
 * Apply `circle` class to make the rippling effect within a circle.
 * 
 * <paper-ripple class="circle"></paper-ripple>
 */
@Generated("com.vaadin.generator.ComponentGenerator")
@Tag("paper-ripple")
public class PaperRipple extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * `null` to disable the listeners.
	 */
	public JsonObject getKeyEventTarget() {
		return (JsonObject) getElement().getPropertyRaw("keyEventTarget");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The EventTarget that will be firing relevant KeyboardEvents. Set it to
	 * `null` to disable the listeners.
	 * 
	 * @param keyEventTarget
	 */
	public void setKeyEventTarget(elemental.json.JsonObject keyEventTarget) {
		getElement().setPropertyJson("keyEventTarget", keyEventTarget);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, this property will cause the implementing element to
	 * automatically stop propagation on any handled KeyboardEvents.
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
	 */
	public void setStopKeyboardEventPropagation(
			boolean stopKeyboardEventPropagation) {
		getElement().setProperty("stopKeyboardEventPropagation",
				stopKeyboardEventPropagation);
	}

	public JsonObject getKeyBindings() {
		return (JsonObject) getElement().getPropertyRaw("keyBindings");
	}

	/**
	 * @param keyBindings
	 */
	public void setKeyBindings(elemental.json.JsonObject keyBindings) {
		getElement().setPropertyJson("keyBindings", keyBindings);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The initial opacity set on the wave.
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
	 */
	public void setInitialOpacity(double initialOpacity) {
		getElement().setProperty("initialOpacity", initialOpacity);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * How fast (opacity per second) the wave fades out.
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
	 */
	public void setOpacityDecayVelocity(double opacityDecayVelocity) {
		getElement().setProperty("opacityDecayVelocity", opacityDecayVelocity);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ripples will exhibit a gravitational pull towards the center of
	 * their container as they fade away.
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
	 */
	public void setRecenters(boolean recenters) {
		getElement().setProperty("recenters", recenters);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, ripples will center inside its container
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
	 */
	public void setCenter(boolean center) {
		getElement().setProperty("center", center);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A list of the visual ripples.
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
	 */
	public void setRipples(elemental.json.JsonArray ripples) {
		getElement().setPropertyJson("ripples", ripples);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when there are visible ripples animating within the element.
	 */
	public boolean isAnimating() {
		return getElement().getProperty("animating", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when there are visible ripples animating within the element.
	 * 
	 * @param animating
	 */
	public void setAnimating(boolean animating) {
		getElement().setProperty("animating", animating);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will remain in the "down" state until `holdDown` is
	 * set to false again.
	 */
	public boolean isHoldDown() {
		return getElement().getProperty("holdDown", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will remain in the "down" state until `holdDown` is
	 * set to false again.
	 * 
	 * @param holdDown
	 */
	public void setHoldDown(boolean holdDown) {
		getElement().setProperty("holdDown", holdDown);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will not generate a ripple effect via pointer
	 * interaction. Calling ripple's imperative api like `simulatedRipple` will
	 * still generate the ripple effect.
	 */
	public boolean isNoink() {
		return getElement().getProperty("noink", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the ripple will not generate a ripple effect via pointer
	 * interaction. Calling ripple's imperative api like `simulatedRipple` will
	 * still generate the ripple effect.
	 * 
	 * @param noink
	 */
	public void setNoink(boolean noink) {
		getElement().setProperty("noink", noink);
	}

	public JsonObject getTarget() {
		return (JsonObject) getElement().getPropertyRaw("target");
	}

	/**
	 * @param target
	 */
	public void setTarget(elemental.json.JsonObject target) {
		getElement().setPropertyJson("target", target);
	}

	public JsonObject getShouldKeepAnimating() {
		return (JsonObject) getElement().getPropertyRaw("shouldKeepAnimating");
	}

	/**
	 * @param shouldKeepAnimating
	 */
	public void setShouldKeepAnimating(
			elemental.json.JsonObject shouldKeepAnimating) {
		getElement()
				.setPropertyJson("shouldKeepAnimating", shouldKeepAnimating);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to imperatively add a key binding to the implementing
	 * element. This is the imperative equivalent of declaring a keybinding in
	 * the `keyBindings` prototype property.
	 * 
	 * @param eventString
	 * @param handlerName
	 */
	public void addOwnKeyBinding(elemental.json.JsonObject eventString,
			elemental.json.JsonObject handlerName) {
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
	 * Returns true if a keyboard event matches `eventString`.
	 * 
	 * @param event
	 * @param eventString
	 */
	public void keyboardEventMatchesKeys(elemental.json.JsonObject event,
			java.lang.String eventString) {
		getElement().callFunction("keyboardEventMatchesKeys", event,
				eventString);
	}

	public void simulatedRipple() {
		getElement().callFunction("simulatedRipple");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple down effect via a UI event, respecting the `noink`
	 * property.
	 * 
	 * @param event
	 */
	public void uiDownAction(elemental.json.JsonObject event) {
		getElement().callFunction("uiDownAction", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple down effect via a UI event, not* respecting the `noink`
	 * property.
	 * 
	 * @param event
	 */
	public void downAction(elemental.json.JsonObject event) {
		getElement().callFunction("downAction", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple up effect via a UI event, respecting the `noink`
	 * property.
	 * 
	 * @param event
	 */
	public void uiUpAction(elemental.json.JsonObject event) {
		getElement().callFunction("uiUpAction", event);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Provokes a ripple up effect via a UI event, not* respecting the `noink`
	 * property.
	 * 
	 * @param event
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

	public Registration addTransitionendListener(DomEventListener listener) {
		return getElement().addEventListener("transitionend", listener);
	}
}