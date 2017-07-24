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
package com.vaadin.generated.paper.progress;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.data.HasValue;
import java.util.Objects;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design: [Progress &
 * activity](https://www.google.com/design/spec/components
 * /progress-activity.html)
 * 
 * The progress bars are for situations where the percentage completed can be
 * determined. They give users a quick sense of how much longer an operation
 * will take.
 * 
 * Example:
 * 
 * <paper-progress value="10"></paper-progress>
 * 
 * There is also a secondary progress which is useful for displaying
 * intermediate progress, such as the buffer level during a streaming playback
 * progress bar.
 * 
 * Example:
 * 
 * <paper-progress value="10" secondary-progress="30"></paper-progress>
 * 
 * ### Styling progress bar:
 * 
 * To change the active progress bar color:
 * 
 * paper-progress { --paper-progress-active-color: #e91e63; }
 * 
 * To change the secondary progress bar color:
 * 
 * paper-progress { --paper-progress-secondary-color: #f8bbd0; }
 * 
 * To change the progress bar background color:
 * 
 * paper-progress { --paper-progress-container-color: #64ffda; }
 * 
 * Add the class {@code transiting} to a paper-progress to animate the progress
 * bar when the value changed. You can also customize the transition:
 * 
 * paper-progress { --paper-progress-transition-duration: 0.08s;
 * --paper-progress-transition-timing-function: ease;
 * --paper-progress-transition-transition-delay: 0s; }
 * 
 * To change the duration of the indeterminate cycle:
 * 
 * paper-progress { --paper-progress-indeterminate-cycle-duration: 2s; }
 * 
 * The following mixins are available for styling:
 * 
 * Custom property | Description | Default
 * --------------------------------------
 * -----------|---------------------------------------------|--------------
 * {@code --paper-progress-container} | Mixin applied to container | {@code
 * {@code --paper-progress-transition-duration} | Duration of the transition |
 * {@code 0.008s} {@code --paper-progress-transition-timing-function} | The
 * timing function for the transition | {@code ease}
 * {@code --paper-progress-transition-delay} | delay for the transition |
 * {@code 0s} {@code --paper-progress-container-color} | Color of the container
 * | {@code --google-grey-300} {@code --paper-progress-active-color} | The color
 * of the active bar | {@code --google-green-500}
 * {@code --paper-progress-secondary-color} | The color of the secondary bar |
 * {@code --google-green-100} {@code --paper-progress-disabled-active-color} |
 * The color of the active bar if disabled | {@code --google-grey-500}
 * {@code --paper-progress-disabled-secondary-color} | The color of the
 * secondary bar if disabled | {@code --google-grey-300}
 * {@code --paper-progress-height} | The height of the progress bar |
 * {@code 4px} {@code --paper-progress-indeterminate-cycle-duration} | Duration
 * of an indeterminate cycle | {@code 2s}
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: paper-progress#2.0.1", "Flow#0.1.14-SNAPSHOT"})
@Tag("paper-progress")
@HtmlImport("frontend://bower_components/paper-progress/paper-progress.html")
public class GeneratedPaperProgress<R extends GeneratedPaperProgress<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasValue<R, Double> {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current value.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 */
	@Synchronize(property = "value", value = "value-changed")
	@Override
	public Double getValue() {
		return getElement().getProperty("value", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current value.
	 * 
	 * @param value
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	@Override
	public R setValue(java.lang.Double value) {
		Objects.requireNonNull(value,
				"GeneratedPaperProgress value must not be null");
		getElement().setProperty("value", value);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current value.
	 * 
	 * @param value
	 *            the Number value to set
	 * @see #setValue(Double)
	 * @return this instance, for method chaining
	 */
	public R setValue(java.lang.Number value) {
		setValue(value.doubleValue());
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the minimum value of the range.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'min-changed' event happens.
	 */
	@Synchronize(property = "min", value = "min-changed")
	public double getMin() {
		return getElement().getProperty("min", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the minimum value of the range.
	 * 
	 * @param min
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setMin(double min) {
		getElement().setProperty("min", min);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the maximum value of the range.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'max-changed' event happens.
	 */
	@Synchronize(property = "max", value = "max-changed")
	public double getMax() {
		return getElement().getProperty("max", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the maximum value of the range.
	 * 
	 * @param max
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setMax(double max) {
		getElement().setProperty("max", max);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the value granularity of the range's value.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'step-changed' event happens.
	 */
	@Synchronize(property = "step", value = "step-changed")
	public double getStep() {
		return getElement().getProperty("step", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the value granularity of the range's value.
	 * 
	 * @param step
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setStep(double step) {
		getElement().setProperty("step", step);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the ratio of the value.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'ratio-changed' event happens.
	 */
	@Synchronize(property = "ratio", value = "ratio-changed")
	public double getRatio() {
		return getElement().getProperty("ratio", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current secondary progress.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getSecondaryProgress() {
		return getElement().getProperty("secondaryProgress", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current secondary progress.
	 * 
	 * @param secondaryProgress
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public R setSecondaryProgress(double secondaryProgress) {
		getElement().setProperty("secondaryProgress", secondaryProgress);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The secondary ratio
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getSecondaryRatio() {
		return getElement().getProperty("secondaryRatio", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use an indeterminate progress indicator.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isIndeterminate() {
		return getElement().getProperty("indeterminate", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use an indeterminate progress indicator.
	 * 
	 * @param indeterminate
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setIndeterminate(boolean indeterminate) {
		getElement().setProperty("indeterminate", indeterminate);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the progress is disabled.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the progress is disabled.
	 * 
	 * @param disabled
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return get();
	}

	@DomEvent("min-changed")
	public static class MinChangeEvent
			extends
				ComponentEvent<GeneratedPaperProgress> {
		public MinChangeEvent(GeneratedPaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addMinChangeListener(
			ComponentEventListener<MinChangeEvent> listener) {
		return addListener(MinChangeEvent.class, listener);
	}

	@DomEvent("max-changed")
	public static class MaxChangeEvent
			extends
				ComponentEvent<GeneratedPaperProgress> {
		public MaxChangeEvent(GeneratedPaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addMaxChangeListener(
			ComponentEventListener<MaxChangeEvent> listener) {
		return addListener(MaxChangeEvent.class, listener);
	}

	@DomEvent("step-changed")
	public static class StepChangeEvent
			extends
				ComponentEvent<GeneratedPaperProgress> {
		public StepChangeEvent(GeneratedPaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addStepChangeListener(
			ComponentEventListener<StepChangeEvent> listener) {
		return addListener(StepChangeEvent.class, listener);
	}

	@DomEvent("ratio-changed")
	public static class RatioChangeEvent
			extends
				ComponentEvent<GeneratedPaperProgress> {
		public RatioChangeEvent(GeneratedPaperProgress source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addRatioChangeListener(
			ComponentEventListener<RatioChangeEvent> listener) {
		return addListener(RatioChangeEvent.class, listener);
	}
}