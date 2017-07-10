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
package com.vaadin.components.paper.progress;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-progress#2.0.1", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-progress")
@HtmlImport("frontend://bower_components/paper-progress/paper-progress.html")
public class PaperProgress<R extends PaperProgress<R>> extends Component
		implements
			HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current value.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'value-changed' event happens.
	 */
	@Synchronize(property = "value", value = "value-changed")
	public double getValue() {
		return getElement().getProperty("value", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current value.
	 * 
	 * @param value
	 * @return this instance, for method chaining
	 */
	public R setValue(double value) {
		getElement().setProperty("value", value);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setMin(double min) {
		getElement().setProperty("min", min);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setMax(double max) {
		getElement().setProperty("max", max);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setStep(double step) {
		getElement().setProperty("step", step);
		return getSelf();
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
	 * Returns the ratio of the value.
	 * 
	 * @param ratio
	 * @return this instance, for method chaining
	 */
	public R setRatio(double ratio) {
		getElement().setProperty("ratio", ratio);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setSecondaryProgress(double secondaryProgress) {
		getElement().setProperty("secondaryProgress", secondaryProgress);
		return getSelf();
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
	 * The secondary ratio
	 * 
	 * @param secondaryRatio
	 * @return this instance, for method chaining
	 */
	public R setSecondaryRatio(double secondaryRatio) {
		getElement().setProperty("secondaryRatio", secondaryRatio);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setIndeterminate(boolean indeterminate) {
		getElement().setProperty("indeterminate", indeterminate);
		return getSelf();
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
	 * @return this instance, for method chaining
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return getSelf();
	}

	@DomEvent("value-changed")
	public static class ValueChangedEvent extends ComponentEvent<PaperProgress> {
		public ValueChangedEvent(PaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addValueChangedListener(
			ComponentEventListener<ValueChangedEvent> listener) {
		return addListener(ValueChangedEvent.class, listener);
	}

	@DomEvent("min-changed")
	public static class MinChangedEvent extends ComponentEvent<PaperProgress> {
		public MinChangedEvent(PaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addMinChangedListener(
			ComponentEventListener<MinChangedEvent> listener) {
		return addListener(MinChangedEvent.class, listener);
	}

	@DomEvent("max-changed")
	public static class MaxChangedEvent extends ComponentEvent<PaperProgress> {
		public MaxChangedEvent(PaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addMaxChangedListener(
			ComponentEventListener<MaxChangedEvent> listener) {
		return addListener(MaxChangedEvent.class, listener);
	}

	@DomEvent("step-changed")
	public static class StepChangedEvent extends ComponentEvent<PaperProgress> {
		public StepChangedEvent(PaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addStepChangedListener(
			ComponentEventListener<StepChangedEvent> listener) {
		return addListener(StepChangedEvent.class, listener);
	}

	@DomEvent("ratio-changed")
	public static class RatioChangedEvent extends ComponentEvent<PaperProgress> {
		public RatioChangedEvent(PaperProgress source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addRatioChangedListener(
			ComponentEventListener<RatioChangedEvent> listener) {
		return addListener(RatioChangedEvent.class, listener);
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