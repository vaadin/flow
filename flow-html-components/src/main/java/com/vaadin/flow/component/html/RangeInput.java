/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.Arrays;
import java.util.Objects;

/**
 * Creates a new input element with type "range".
 * <p>
 * </p>
 * Note: Slider doesn't support the read-only mode and will disable itself
 * instead.
 */
@Tag(Tag.INPUT)
public class RangeInput extends AbstractSinglePropertyField<RangeInput, Double>
        implements Focusable<RangeInput>, HasSize, HasStyle, HasValueChangeMode,
        HasAriaLabel {
    private static final PropertyDescriptor<String, String> typeDescriptor = PropertyDescriptors
            .attributeWithDefault("type", "text");
    private static final PropertyDescriptor<String, String> minDescriptor = PropertyDescriptors
            .attributeWithDefault("min", "0");
    private static final PropertyDescriptor<String, String> maxDescriptor = PropertyDescriptors
            .attributeWithDefault("max", "100");
    private static final PropertyDescriptor<String, String> stepDescriptor = PropertyDescriptors
            .attributeWithDefault("step", "1");
    private static final PropertyDescriptor<String, String> orientDescriptor = PropertyDescriptors
            .attributeWithDefault("orient", Orientation.HORIZONTAL.getValue());

    /**
     * The orientation of the range slider.
     */
    public enum Orientation {
        HORIZONTAL("horizontal"), VERTICAL("vertical");

        private final String value;

        Orientation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private int valueChangeTimeout = DEFAULT_CHANGE_TIMEOUT;

    private ValueChangeMode currentMode;

    /**
     * Creates a new slider, with {@link ValueChangeMode#ON_CHANGE ON_CHANGE}
     * value change mode.
     */
    public RangeInput() {
        this(ValueChangeMode.ON_CHANGE);
    }

    /**
     * Creates a new slider.
     *
     * @param valueChangeMode
     *            initial value change mode, or <code>null</code> to disable the
     *            value synchronization
     */
    public RangeInput(ValueChangeMode valueChangeMode) {
        super("value", 0.0, false);
        setValueChangeMode(valueChangeMode);
        set(typeDescriptor, "range");
    }

    @Override
    public ValueChangeMode getValueChangeMode() {
        return currentMode;
    }

    @Override
    public void setValueChangeMode(ValueChangeMode valueChangeMode) {
        currentMode = valueChangeMode;
        setSynchronizedEvent(
                ValueChangeMode.eventForMode(valueChangeMode, "input"));
        applyChangeTimeout();
    }

    @Override
    public void setValueChangeTimeout(int valueChangeTimeout) {
        this.valueChangeTimeout = valueChangeTimeout;
        applyChangeTimeout();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default value is {@link HasValueChangeMode#DEFAULT_CHANGE_TIMEOUT}.
     */
    @Override
    public int getValueChangeTimeout() {
        return valueChangeTimeout;
    }

    private void applyChangeTimeout() {
        ValueChangeMode.applyChangeTimeout(currentMode, valueChangeTimeout,
                getSynchronizationRegistration());
    }

    /**
     * Gets the minimum value.
     *
     * @return the minimum value, defaults to 0.
     */
    public double getMin() {
        return Double.parseDouble(get(minDescriptor));
    }

    /**
     * Sets the new minimum value.
     *
     * @param min
     *            the minimum value.
     */
    public void setMin(double min) {
        set(minDescriptor, "" + min);
    }

    /**
     * Gets the maximum value.
     *
     * @return the maximum value, defaults to 100.
     */
    public double getMax() {
        return Double.parseDouble(get(maxDescriptor));
    }

    /**
     * Sets the new maximum value.
     *
     * @param max
     *            the maximum value.
     */
    public void setMax(double max) {
        set(maxDescriptor, "" + max);
    }

    /**
     * The step attribute is a number that specifies the granularity that the
     * value must adhere to.
     * <p>
     * </p>
     * The step attribute can also be set to null. This step value means that no
     * stepping interval is implied and any value is allowed in the specified
     * range
     * <p>
     * </p>
     * The default stepping value for range inputs is 1, allowing only integers
     * to be entered, unless the stepping base is not an integer; for example,
     * if you set min to -10 and value to 1.5, then a step of 1 will allow only
     * values such as 1.5, 2.5, 3.5,… in the positive direction and -0.5, -1.5,
     * -2.5,… in the negative direction.
     *
     * @return the current step value, defaults to 1.
     */
    public Double getStep() {
        final String step = get(stepDescriptor);
        return "any".equals(step) ? null : Double.parseDouble(step);
    }

    /**
     * The step attribute is a number that specifies the granularity that the
     * value must adhere to.
     * <p>
     * </p>
     * The step attribute can also be set to null. This step value means that no
     * stepping interval is implied and any value is allowed in the specified
     * range
     * <p>
     * </p>
     * The default stepping value for range inputs is 1, allowing only integers
     * to be entered, unless the stepping base is not an integer; for example,
     * if you set min to -10 and value to 1.5, then a step of 1 will allow only
     * values such as 1.5, 2.5, 3.5,… in the positive direction and -0.5, -1.5,
     * -2.5,… in the negative direction.
     *
     * @param step
     *            the new step value, may be null.
     */
    public void setStep(Double step) {
        set(stepDescriptor, step == null ? "any" : "" + step);
    }

    /**
     * Sets the orientation of the range slider.
     * <p>
     * </p>
     * <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/range#non-standard_attributes">Non-standard
     * Attribute</a>. Since the vertical orientation is not standardized yet,
     * this feature is not guaranteed to work on every browser. We found this
     * feature to work on Firefox 120+, Chromium 119+, Edge 119+ and Safari
     * 17.1+.
     * <p>
     * </p>
     * The orient attribute defines the orientation of the range slider. Values
     * include horizontal, meaning the range is rendered horizontally, and
     * vertical, where the range is rendered vertically.
     *
     * @param orientation
     *            the orientation, not null. Defaults to
     *            {@link Orientation#HORIZONTAL}.
     */
    public void setOrientation(Orientation orientation) {
        Objects.requireNonNull(orientation);
        // Fix support for individual browsers
        // See
        // https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/range#creating_vertical_range_controls
        // for more details

        // support for Firefox
        set(orientDescriptor, orientation.getValue());
        if (orientation == Orientation.VERTICAL) {
            // Support for Chrome and Safari
            getStyle().set("-webkit-appearance", "slider-vertical");
            getStyle().set("appearance", "slider-vertical");
            // Support for Edge
            getStyle().set("writing-mode", "bt-lr");
        } else {
            getStyle().remove("-webkit-appearance");
            getStyle().remove("appearance");
            getStyle().remove("writing-mode");
        }
    }

    /**
     * Gets the orientation of the range slider.
     * <p>
     * </p>
     * <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/range#non-standard_attributes">Non-standard
     * Attribute</a>.
     * <p>
     * </p>
     * The orient attribute defines the orientation of the range slider. Values
     * include horizontal, meaning the range is rendered horizontally, and
     * vertical, where the range is rendered vertically.
     *
     * @return the current orientation, never null.
     */
    public Orientation getOrientation() {
        final String orientation = get(orientDescriptor);
        return Arrays.stream(Orientation.values())
                .filter(it -> it.getValue().equals(orientation)).findAny()
                .orElse(Orientation.HORIZONTAL);
    }

    /**
     * Range input element doesn't support the "read-only" attribute or
     * property. We'll disable the component instead.
     */
    private boolean readOnly = false;
    private boolean enabled = true;

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        updateEnabled();
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateEnabled();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    private void updateEnabled() {
        super.setEnabled(enabled && !readOnly);
    }
}
