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
package com.vaadin.ui.paper.progress;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.common.HasValue;
import java.util.Objects;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-progress#2.0.1", "Flow#1.0-SNAPSHOT" })
@Tag("paper-progress")
@HtmlImport("frontend://bower_components/paper-progress/paper-progress.html")
public class GeneratedPaperProgress<R extends GeneratedPaperProgress<R>>
        extends Component
        implements HasStyle, ComponentSupplier<R>, HasValue<R, Double> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that represents the current value.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'value-changed' event happens.
     * </p>
     * 
     * @return the {@code value} property from the webcomponent
     */
    @Synchronize(property = "value", value = "value-changed")
    @Override
    public Double getValue() {
        return getElement().getProperty("value", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that represents the current value.
     * </p>
     * 
     * @param value
     *            the double value to set
     */
    @Override
    public void setValue(Double value) {
        Objects.requireNonNull(value,
                "GeneratedPaperProgress value must not be null");
        if (!Objects.equals(value, getValue())) {
            getElement().setProperty("value", value);
        }
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that represents the current value.
     * </p>
     * 
     * @param value
     *            the Number value to set
     * @see #setValue(Double)
     */
    public void setValue(Number value) {
        Objects.requireNonNull(value,
                "GeneratedPaperProgress value must not be null");
        if (!Objects.equals(value, getValue())) {
            setValue(value.doubleValue());
        }
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that indicates the minimum value of the range.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'min-changed' event happens.
     * </p>
     * 
     * @return the {@code min} property from the webcomponent
     */
    @Synchronize(property = "min", value = "min-changed")
    public double getMin() {
        return getElement().getProperty("min", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that indicates the minimum value of the range.
     * </p>
     * 
     * @param min
     *            the double value to set
     */
    public void setMin(double min) {
        getElement().setProperty("min", min);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that indicates the maximum value of the range.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'max-changed' event happens.
     * </p>
     * 
     * @return the {@code max} property from the webcomponent
     */
    @Synchronize(property = "max", value = "max-changed")
    public double getMax() {
        return getElement().getProperty("max", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that indicates the maximum value of the range.
     * </p>
     * 
     * @param max
     *            the double value to set
     */
    public void setMax(double max) {
        getElement().setProperty("max", max);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the value granularity of the range's value.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'step-changed' event happens.
     * </p>
     * 
     * @return the {@code step} property from the webcomponent
     */
    @Synchronize(property = "step", value = "step-changed")
    public double getStep() {
        return getElement().getProperty("step", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Specifies the value granularity of the range's value.
     * </p>
     * 
     * @param step
     *            the double value to set
     */
    public void setStep(double step) {
        getElement().setProperty("step", step);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Returns the ratio of the value.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'ratio-changed' event happens.
     * </p>
     * 
     * @return the {@code ratio} property from the webcomponent
     */
    @Synchronize(property = "ratio", value = "ratio-changed")
    public double getRatio() {
        return getElement().getProperty("ratio", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that represents the current secondary progress.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code secondaryProgress} property from the webcomponent
     */
    public double getSecondaryProgress() {
        return getElement().getProperty("secondaryProgress", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number that represents the current secondary progress.
     * </p>
     * 
     * @param secondaryProgress
     *            the double value to set
     */
    public void setSecondaryProgress(double secondaryProgress) {
        getElement().setProperty("secondaryProgress", secondaryProgress);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The secondary ratio
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code secondaryRatio} property from the webcomponent
     */
    public double getSecondaryRatio() {
        return getElement().getProperty("secondaryRatio", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Use an indeterminate progress indicator.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code indeterminate} property from the webcomponent
     */
    public boolean isIndeterminate() {
        return getElement().getProperty("indeterminate", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Use an indeterminate progress indicator.
     * </p>
     * 
     * @param indeterminate
     *            the boolean value to set
     */
    public void setIndeterminate(boolean indeterminate) {
        getElement().setProperty("indeterminate", indeterminate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the progress is disabled.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code disabled} property from the webcomponent
     */
    public boolean isDisabled() {
        return getElement().getProperty("disabled", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the progress is disabled.
     * </p>
     * 
     * @param disabled
     *            the boolean value to set
     */
    public void setDisabled(boolean disabled) {
        getElement().setProperty("disabled", disabled);
    }

    @DomEvent("min-changed")
    public static class MinChangeEvent<R extends GeneratedPaperProgress<R>>
            extends ComponentEvent<R> {
        public MinChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code min-changed} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addMinChangeListener(
            ComponentEventListener<MinChangeEvent<R>> listener) {
        return addListener(MinChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("max-changed")
    public static class MaxChangeEvent<R extends GeneratedPaperProgress<R>>
            extends ComponentEvent<R> {
        public MaxChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code max-changed} events fired by the webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addMaxChangeListener(
            ComponentEventListener<MaxChangeEvent<R>> listener) {
        return addListener(MaxChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("step-changed")
    public static class StepChangeEvent<R extends GeneratedPaperProgress<R>>
            extends ComponentEvent<R> {
        public StepChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code step-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addStepChangeListener(
            ComponentEventListener<StepChangeEvent<R>> listener) {
        return addListener(StepChangeEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("ratio-changed")
    public static class RatioChangeEvent<R extends GeneratedPaperProgress<R>>
            extends ComponentEvent<R> {
        public RatioChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code ratio-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addRatioChangeListener(
            ComponentEventListener<RatioChangeEvent<R>> listener) {
        return addListener(RatioChangeEvent.class,
                (ComponentEventListener) listener);
    }
}