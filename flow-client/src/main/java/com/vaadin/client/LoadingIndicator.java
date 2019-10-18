/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.client;

import com.google.gwt.user.client.Timer;
import com.vaadin.flow.internal.nodefeature.LoadingIndicatorConfigurationMap;
import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.dom.Element;

/**
 * The loading indicator for Vaadin applications representation.
 * <p>
 * The loading indicator has four states: "triggered", "first", "second" and
 * "third". When {@link #trigger()} is called the indicator moves to its
 * "triggered" state and then transitions from one state to the next when the
 * timeouts specified using the set*StateDelay methods occur.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class LoadingIndicator {

    private static final String PRIMARY_STYLE_NAME = "v-loading-indicator";

    private static final String DEFAULT_THEMING = "@-webkit-keyframes v-progress-start {" +
            "0% {width: 0%;}" +
            "100% {width: 50%;}}" +
            "@-moz-keyframes v-progress-start {" +
            "0% {width: 0%;}" +
            "100% {width: 50%;}}" +
            "@keyframes v-progress-start {" +
            "0% {width: 0%;}" +
            "100% {width: 50%;}}" +
            "@keyframes v-progress-delay {" +
            "0% {width: 50%;}" +
            "100% {width: 90%;}}" +
            "@keyframes v-progress-wait {" +
            "0% {width: 90%;height: 4px;}" +
            "3% {width: 91%;height: 7px;}" +
            "100% {width: 96%;height: 7px;}}" +
            "@-webkit-keyframes v-progress-wait-pulse {" +
            "0% {opacity: 1;}" +
            "50% {opacity: 0.1;}" +
            "100% {opacity: 1;}}" +
            "@-moz-keyframes v-progress-wait-pulse {" +
            "0% {opacity: 1;}" +
            "50% {opacity: 0.1;}" +
            "100% {opacity: 1;}}" +
            "@keyframes v-progress-wait-pulse {" +
            "0% {opacity: 1;}" +
            "50% {opacity: 0.1;}" +
            "100% {opacity: 1;}}" +
            ".v-loading-indicator {" +
            "position: fixed !important;" +
            "z-index: 99999;" +
            "left: 0;" +
            "right: auto;" +
            "top: 0;" +
            "width: 50%;" +
            "opacity: 1;" +
            "height: 4px;" +
            "background-color: var(--lumo-primary-color, var(--material-primary-color, blue));" +
            "pointer-events: none;" +
            "transition: none;" +
            "animation: v-progress-start 1000ms 200ms both;}" +
            ".v-loading-indicator[style*=\"none\"] {" +
            "display: block !important;" +
            "width: 100% !important;" +
            "opacity: 0;" +
            "animation: none !important;" +
            "transition: opacity 500ms 300ms, width 300ms;}" +
            ".v-loading-indicator.second {" +
            "width: 90%;" +
            "animation: v-progress-delay 3.8s forwards;}" +
            ".v-loading-indicator.third {" +
            "width: 96%;" +
            "animation: v-progress-wait 5s forwards, v-progress-wait-pulse 1s 4s infinite backwards;}";

    private int firstDelay = LoadingIndicatorConfigurationMap.FIRST_DELAY_DEFAULT;
    private int secondDelay = LoadingIndicatorConfigurationMap.SECOND_DELAY_DEFAULT;
    private int thirdDelay = LoadingIndicatorConfigurationMap.THIRD_DELAY_DEFAULT;

    private Timer firstTimer = new Timer() {
        @Override
        public void run() {
            show();
        }
    };
    private Timer secondTimer = new Timer() {
        @Override
        public void run() {
            getElement().setClassName(PRIMARY_STYLE_NAME);
            getElement().getClassList().add("second");
        }
    };
    private Timer thirdTimer = new Timer() {
        @Override
        public void run() {
            getElement().setClassName(PRIMARY_STYLE_NAME);
            getElement().getClassList().add("third");
        }
    };

    private Element element;

    private Element styleElement;
    private boolean applyDefaultTheme = true;

    /**
     * Returns the delay (in ms) which must pass before the loading indicator
     * moves into the "first" state and is shown to the user
     *
     * @return The delay (in ms) until moving into the "first" state. Counted
     * from when {@link #trigger()} is called.
     */
    public int getFirstDelay() {
        return firstDelay;
    }

    /**
     * Sets the delay (in ms) which must pass before the loading indicator moves
     * into the "first" state and is shown to the user
     *
     * @param firstDelay
     *         The delay (in ms) until moving into the "first" state. Counted
     *         from when {@link #trigger()} is called.
     */
    public void setFirstDelay(int firstDelay) {
        this.firstDelay = firstDelay;
    }

    /**
     * Returns the delay (in ms) which must pass before the loading indicator
     * moves to its "second" state.
     *
     * @return The delay (in ms) until the loading indicator moves into its
     * "second" state. Counted from when {@link #trigger()} is called.
     */
    public int getSecondDelay() {
        return secondDelay;
    }

    /**
     * Sets the delay (in ms) which must pass before the loading indicator moves
     * to its "second" state.
     *
     * @param secondDelay
     *         The delay (in ms) until the loading indicator moves into its
     *         "second" state. Counted from when {@link #trigger()} is
     *         called.
     */
    public void setSecondDelay(int secondDelay) {
        this.secondDelay = secondDelay;
    }

    /**
     * Returns the delay (in ms) which must pass before the loading indicator
     * moves to its "third" state.
     *
     * @return The delay (in ms) until the loading indicator moves into its
     * "third" state. Counted from when {@link #trigger()} is called.
     */
    public int getThirdDelay() {
        return thirdDelay;
    }

    /**
     * Sets the delay (in ms) which must pass before the loading indicator moves
     * to its "third" state.
     *
     * @param thirdDelay
     *         The delay (in ms) from the event until changing the loading
     *         indicator into its "third" state. Counted from when
     *         {@link #trigger()} is called.
     */
    public void setThirdDelay(int thirdDelay) {
        this.thirdDelay = thirdDelay;
    }

    /**
     * Triggers displaying of this loading indicator. The loading indicator will
     * actually be shown by {@link #show()} when the "first" delay (as specified
     * by {@link #getFirstDelay()}) has passed.
     * <p>
     * The loading indicator will be hidden if shown when calling this method.
     * </p>
     */
    public void trigger() {
        hide();
        firstTimer.schedule(getFirstDelay());
    }

    /**
     * Triggers displaying of this loading indicator unless it's already visible
     * or scheduled to be shown after a delay.
     *
     */
    public void ensureTriggered() {
        if (!isVisible() && !firstTimer.isRunning()) {
            trigger();
        }
    }

    /**
     * Shows the loading indicator in its standard state and triggers timers for
     * transitioning into the "second" and "third" states.
     */
    public void show() {
        // Reset possible style name and display mode
        getElement().setClassName(PRIMARY_STYLE_NAME);
        getElement().getClassList().add("first");
        getElement().getStyle().setDisplay(Display.BLOCK);

        // Schedule the "second" loading indicator
        int secondTimerDelay = getSecondDelay() - getFirstDelay();
        if (secondTimerDelay >= 0) {
            secondTimer.schedule(secondTimerDelay);
        }

        // Schedule the "third" loading indicator
        int thirdTimerDelay = getThirdDelay() - getFirstDelay();
        if (thirdTimerDelay >= 0) {
            thirdTimer.schedule(thirdTimerDelay);
        }
    }

    /**
     * Hides the loading indicator (if visible). Cancels any possibly running
     * timers.
     */
    public void hide() {
        firstTimer.cancel();
        secondTimer.cancel();
        thirdTimer.cancel();

        getElement().getStyle().setDisplay(Display.NONE);
    }

    /**
     * Returns whether or not the loading indicator is showing.
     *
     * @return true if the loading indicator is visible, false otherwise
     */
    public boolean isVisible() {
        if (getElement().getStyle().getDisplay().equals(Display.NONE)) {
            return false;
        }

        return true;
    }

    /**
     * Returns the root element of the loading indicator.
     *
     * @return The loading indicator DOM element
     */
    public Element getElement() {
        if (element == null) {
            setupTheming();

            element = Browser.getDocument().createElement("div");
            Browser.getDocument().getBody().appendChild(element);
        }
        return element;
    }

    private void setupTheming() {
        if (styleElement == null) {
            styleElement = Browser.getDocument().createStyleElement();
            styleElement.setAttribute("type", "text/css");
            styleElement.setInnerHTML(DEFAULT_THEMING);
        }

        boolean attached = styleElement.getParentElement() != null;
        if (isApplyDefaultTheme() && !attached) {
            Browser.getDocument().getHead().appendChild(styleElement);
        } else if (!isApplyDefaultTheme() && attached) {
            styleElement.getParentElement().removeChild(styleElement);
        }
    }

    /**
     * Sets whether the default theming should be applied for the loading indicator or not.
     * <p>
     * Default is {@code true}.
     *
     * @param applyDefaultTheme
     *         {@code true} for applying the default theming, {@code false} for not
     */
    public void setApplyDefaultTheme(boolean applyDefaultTheme) {
        this.applyDefaultTheme = applyDefaultTheme;
        setupTheming();
    }

    /**
     * Returns whether the default theming should be applied for the loading indicator or not.
     * <p>
     * Default is {@code true}.
     *
     * @return {@code true} for applying the default theming, {@code false} for not
     */
    public boolean isApplyDefaultTheme() {
        return applyDefaultTheme;
    }
}
