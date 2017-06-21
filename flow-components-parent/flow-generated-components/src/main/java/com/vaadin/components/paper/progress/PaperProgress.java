package com.vaadin.components.paper.progress;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: paper-progress#2.0.1", "Flow#0.1.10-SNAPSHOT"})
@Tag("paper-progress")
@HtmlImport("frontend://bower_components/paper-progress/paper-progress.html")
public class PaperProgress extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current value.
	 */
	public double getValue() {
		return getElement().getProperty("value", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current value.
	 * 
	 * @param value
	 */
	public void setValue(double value) {
		getElement().setProperty("value", value);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the minimum value of the range.
	 */
	public double getMin() {
		return getElement().getProperty("min", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the minimum value of the range.
	 * 
	 * @param min
	 */
	public void setMin(double min) {
		getElement().setProperty("min", min);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the maximum value of the range.
	 */
	public double getMax() {
		return getElement().getProperty("max", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that indicates the maximum value of the range.
	 * 
	 * @param max
	 */
	public void setMax(double max) {
		getElement().setProperty("max", max);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the value granularity of the range's value.
	 */
	public double getStep() {
		return getElement().getProperty("step", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specifies the value granularity of the range's value.
	 * 
	 * @param step
	 */
	public void setStep(double step) {
		getElement().setProperty("step", step);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the ratio of the value.
	 */
	public double getRatio() {
		return getElement().getProperty("ratio", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the ratio of the value.
	 * 
	 * @param ratio
	 */
	public void setRatio(double ratio) {
		getElement().setProperty("ratio", ratio);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number that represents the current secondary progress.
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
	 */
	public void setSecondaryProgress(double secondaryProgress) {
		getElement().setProperty("secondaryProgress", secondaryProgress);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The secondary ratio
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
	 */
	public void setSecondaryRatio(double secondaryRatio) {
		getElement().setProperty("secondaryRatio", secondaryRatio);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Use an indeterminate progress indicator.
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
	 */
	public void setIndeterminate(boolean indeterminate) {
		getElement().setProperty("indeterminate", indeterminate);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the progress is disabled.
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
	 */
	public void setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
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
}