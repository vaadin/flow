/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import com.vaadin.annotations.HTML;
import com.vaadin.annotations.Tag;

/**
 * A component for selecting a numerical value within a range.
 *
 * @author Vaadin Ltd.
 */
@Tag("paper-slider")
@HTML("vaadin://bower_components/paper-slider/paper-slider.html")
public class Slider extends AbstractField<Double> {

    /**
     * Default slider constructor. Sets all values to defaults and the slide
     * handle at minimum value.
     *
     */
    public Slider() {
        super();
        setValue(getMin());
        getElement().addClass("x-scope paper-slider-0");
    }

    /**
     * Create a new slider with the caption given as parameter.
     *
     * The range of the slider is set to 0-100 and only integer values are
     * allowed.
     *
     * @param caption
     *            The caption for this slider (e.g. "Volume").
     */
    public Slider(String caption) {
        this();
        setCaption(caption);
    }

    /**
     * Create a new slider with the given range. The slider only allows integer
     * values by default (step is 1)
     *
     * @param min
     *            The minimum value of the slider
     * @param max
     *            The maximum value of the slider
     */
    public Slider(double min, double max) {
        this();
        setMin(min);
        setMax(max);
    }

    /**
     * Create a new slider with the given range and step.
     *
     * @param min
     *            The minimum value of the slider
     * @param max
     *            The maximum value of the slider
     * @param step
     *            the minimum distance the handle can move
     */
    public Slider(double min, double max, double step) {
        this();
        setStep(step);
        setMin(min);
        setMax(max);
    }

    /**
     * Create a new slider with the given caption and range. The slider only
     * allows integer values by default (step is 1)
     *
     * @param caption
     *            The caption for the slider
     * @param min
     *            The minimum value of the slider
     * @param max
     *            The maximum value of the slider
     */
    public Slider(String caption, double min, double max) {
        this(min, max);
        setCaption(caption);
    }

    /**
     * Gets the maximum slider value
     *
     * @return the largest value the slider can have
     */
    public double getMax() {
        return getElement().getAttribute("max", 100.0);
    }

    /**
     * Set the maximum slider value. If the current value of the slider is
     * larger than this, the value is set to the new maximum.
     *
     * @param max
     *            The new maximum slider value
     */
    public void setMax(double max) {
        double roundedMax = roundValue(max);
        getElement().setAttribute("max", max);

        if (getMin() > roundedMax) {
            setMin(roundedMax);
        }

        if (getValue() > roundedMax) {
            setValue(roundedMax);
        }
    }

    /**
     * Gets the minimum slider value
     *
     * @return the smallest value the slider can have
     */
    public double getMin() {
        return getElement().getAttribute("min", 0.0);
    }

    /**
     * Set the minimum slider value. If the current value of the slider is
     * smaller than this, the value is set to the new minimum.
     *
     * @param min
     *            The new minimum slider value
     */
    public void setMin(double min) {
        double roundedMin = roundValue(min);
        getElement().setAttribute("min", roundedMin);

        if (getMax() < roundedMin) {
            setMax(roundedMin);
        }

        if (getValue() < roundedMin) {
            setValue(roundedMin);
        }
    }

    /**
     * Sets the value of the slider.
     *
     * @param value
     *            The new value of the slider.
     * @param repaintIsNotNeeded
     *            If true, client-side is not requested to repaint itself.
     * @throws ValueOutOfBoundsException
     *             If the given value is not inside the range of the slider.
     * @see #setMin(double) {@link #setMax(double)}
     */
    @Override
    protected void setValue(Double value, boolean repaintIsNotNeeded) {
        double newValue = roundValue(value);

        if (getMin() > newValue || getMax() < newValue) {
            throw new ValueOutOfBoundsException(newValue);
        }
        super.setValue(newValue, repaintIsNotNeeded);
    }

    private double roundValue(double value) {
        double range = getMax() - getMin();
        double totalSteps = range / getStep();
        double relativeValue = (value - getMin()) / range;
        double currentStep = relativeValue * totalSteps;
        return Math.round(currentStep * getStep()) + getMin();
    }

    /**
     * Sets the minimum length you can move the slider i.e. the distance between
     * invisible tick marks evenly spaced on the slider.
     * <p>
     * Default 1, meaning only integers can be selected
     *
     * @param step
     *            the minimum distance the handle can move
     */
    public void setStep(double step) {
        getElement().setAttribute("step", step);
        setValue(getValue());
    }

    /**
     * Gets the minimum length you can move the slider i.e. the distance between
     * invisible tick marks evenly spaced on the slider.
     * <p>
     * Default 1, meaning only integers can be selected
     *
     * @return the minimum distance the handle can move
     */
    public double getStep() {
        return getElement().getAttribute("step", 1.0);
    }

    /**
     * If true, a pin with numeric value label is shown when the slider thumb is
     * pressed.
     *
     * @param pin
     *            true to show the value when the slider is pressed, false
     *            otherwise
     */
    public void setPin(boolean pin) {
        getElement().setAttribute("pin", pin);
    }

    /**
     * Checks if a pin with numeric value label is shown when the slider thumb
     * is pressed.
     *
     * @return true if the value is shown when the slider is pressed, false
     *         otherwise
     */
    public boolean isPin() {
        return getElement().hasAttribute("pin");

    }

    @Override
    protected void setInternalValue(Double newValue) {
        super.setInternalValue(newValue);
        if (newValue == null) {
            newValue = 0.0;
        }
        getElement().setAttribute("value", roundValue(newValue));
    }

    /**
     * Thrown when the value of the slider is about to be set to a value that is
     * outside the valid range of the slider.
     *
     * @author Vaadin Ltd.
     *
     */
    public class ValueOutOfBoundsException extends RuntimeException {

        private final Double value;

        /**
         * Constructs an <code>ValueOutOfBoundsException</code> with the
         * specified detail message.
         *
         * @param valueOutOfBounds
         */
        public ValueOutOfBoundsException(Double valueOutOfBounds) {
            super(String.format("Value %s is out of bounds: [%s, %s]",
                    valueOutOfBounds, getMin(), getMax()));
            value = valueOutOfBounds;
        }

        /**
         * Gets the value that is outside the valid range of the slider.
         *
         * @return the value that is out of bounds
         */
        public Double getValue() {
            return value;
        }
    }

    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public void clear() {
        super.setValue(getMin());
    }

    @Override
    public boolean isEmpty() {
        // Slider is never really "empty"
        return false;
    }

    @Override
    public void addValueChangeListener(
            com.vaadin.data.Property.ValueChangeListener listener) {
        if (!hasListeners(ValueChangeListener.class)) {
            getElement().addEventData("change", "element.value");
            getElement().addEventListener("change", e -> {
                setValue(e.getNumber("element.value"));
            });
        }
        super.addValueChangeListener(listener);
    }
}
