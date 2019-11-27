/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.component.datepicker;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import elemental.json.JsonObject;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.Component;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.DatePickerOverlayContentElement#UNKNOWN",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-date-picker-overlay-content")
@HtmlImport("frontend://bower_components/vaadin-date-picker/src/vaadin-date-picker-overlay-content.html")
public abstract class GeneratedVaadinDatePickerOverlayContent<R extends GeneratedVaadinDatePickerOverlayContent<R>>
        extends Component implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The value for this element.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'selected-date-changed' event happens.
     * </p>
     * 
     * @return the {@code selectedDate} property from the webcomponent
     */
    @Synchronize(property = "selectedDate", value = "selected-date-changed")
    protected JsonObject getSelectedDateJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("selectedDate");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The value for this element.
     * </p>
     * 
     * @param selectedDate
     *            the JsonObject value to set
     */
    protected void setSelectedDate(JsonObject selectedDate) {
        getElement().setPropertyJson("selectedDate", selectedDate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Date value which is focused using keyboard.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'focused-date-changed' event happens.
     * </p>
     * 
     * @return the {@code focusedDate} property from the webcomponent
     */
    @Synchronize(property = "focusedDate", value = "focused-date-changed")
    protected JsonObject getFocusedDateJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("focusedDate");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Date value which is focused using keyboard.
     * </p>
     * 
     * @param focusedDate
     *            the JsonObject value to set
     */
    protected void setFocusedDate(JsonObject focusedDate) {
        getElement().setPropertyJson("focusedDate", focusedDate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Date which should be visible when there is no value selected.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code initialPosition} property from the webcomponent
     */
    protected JsonObject getInitialPositionJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("initialPosition");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Date which should be visible when there is no value selected.
     * </p>
     * 
     * @param initialPosition
     *            the JsonObject value to set
     */
    protected void setInitialPosition(JsonObject initialPosition) {
        getElement().setPropertyJson("initialPosition", initialPosition);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code i18n} property from the webcomponent
     */
    protected JsonObject getI18nJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("i18n");
    }

    /**
     * @param i18n
     *            the JsonObject value to set
     */
    protected void setI18n(JsonObject i18n) {
        getElement().setPropertyJson("i18n", i18n);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code showWeekNumbers} property from the webcomponent
     */
    protected boolean isShowWeekNumbersBoolean() {
        return getElement().getProperty("showWeekNumbers", false);
    }

    /**
     * @param showWeekNumbers
     *            the boolean value to set
     */
    protected void setShowWeekNumbers(boolean showWeekNumbers) {
        getElement().setProperty("showWeekNumbers", showWeekNumbers);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The earliest date that can be selected. All earlier dates will be
     * disabled.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code minDate} property from the webcomponent
     */
    protected JsonObject getMinDateJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("minDate");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The earliest date that can be selected. All earlier dates will be
     * disabled.
     * </p>
     * 
     * @param minDate
     *            the JsonObject value to set
     */
    protected void setMinDate(JsonObject minDate) {
        getElement().setPropertyJson("minDate", minDate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The latest date that can be selected. All later dates will be disabled.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code maxDate} property from the webcomponent
     */
    protected JsonObject getMaxDateJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("maxDate");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The latest date that can be selected. All later dates will be disabled.
     * </p>
     * 
     * @param maxDate
     *            the JsonObject value to set
     */
    protected void setMaxDate(JsonObject maxDate) {
        getElement().setPropertyJson("maxDate", maxDate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Input label
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code label} property from the webcomponent
     */
    protected String getLabelString() {
        return getElement().getProperty("label");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Input label
     * </p>
     * 
     * @param label
     *            the String value to set
     */
    protected void setLabel(String label) {
        getElement().setProperty("label", label == null ? "" : label);
    }

    protected void announceFocusedDate() {
        getElement().callFunction("announceFocusedDate");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Focuses the cancel button
     * </p>
     */
    protected void focusCancel() {
        getElement().callFunction("focusCancel");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Scrolls the list to the given Date.
     * </p>
     * 
     * @param date
     *            Missing documentation!
     * @param animate
     *            Missing documentation!
     */
    protected void scrollToDate(JsonObject date, JsonObject animate) {
        getElement().callFunction("scrollToDate", date, animate);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Scrolls the month and year scrollers enough to reveal the given date.
     * </p>
     * 
     * @param date
     *            Missing documentation!
     */
    protected void revealDate(JsonObject date) {
        getElement().callFunction("revealDate", date);
    }

    @DomEvent("scroll-animation-finished")
    public static class ScrollAnimationFinishedEvent<R extends GeneratedVaadinDatePickerOverlayContent<R>>
            extends ComponentEvent<R> {
        private final double detailPosition;
        private final double detailOldPosition;

        public ScrollAnimationFinishedEvent(R source, boolean fromClient,
                @EventData("event.detail.position") double detailPosition,
                @EventData("event.detail.oldPosition") double detailOldPosition) {
            super(source, fromClient);
            this.detailPosition = detailPosition;
            this.detailOldPosition = detailOldPosition;
        }

        public double getDetailPosition() {
            return detailPosition;
        }

        public double getDetailOldPosition() {
            return detailOldPosition;
        }
    }

    /**
     * Adds a listener for {@code scroll-animation-finished} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addScrollAnimationFinishedListener(
            ComponentEventListener<ScrollAnimationFinishedEvent<R>> listener) {
        return addListener(ScrollAnimationFinishedEvent.class,
                (ComponentEventListener) listener);
    }

    public static class SelectedDateChangeEvent<R extends GeneratedVaadinDatePickerOverlayContent<R>>
            extends ComponentEvent<R> {
        private final JsonObject selectedDate;

        public SelectedDateChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.selectedDate = source.getSelectedDateJsonObject();
        }

        public JsonObject getSelectedDate() {
            return selectedDate;
        }
    }

    /**
     * Adds a listener for {@code selected-date-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addSelectedDateChangeListener(
            ComponentEventListener<SelectedDateChangeEvent<R>> listener) {
        return getElement().addPropertyChangeListener("selectedDate",
                event -> listener.onComponentEvent(
                        new SelectedDateChangeEvent<R>((R) this,
                                event.isUserOriginated())));
    }

    public static class FocusedDateChangeEvent<R extends GeneratedVaadinDatePickerOverlayContent<R>>
            extends ComponentEvent<R> {
        private final JsonObject focusedDate;

        public FocusedDateChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.focusedDate = source.getFocusedDateJsonObject();
        }

        public JsonObject getFocusedDate() {
            return focusedDate;
        }
    }

    /**
     * Adds a listener for {@code focused-date-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addFocusedDateChangeListener(
            ComponentEventListener<FocusedDateChangeEvent<R>> listener) {
        return getElement().addPropertyChangeListener("focusedDate",
                event -> listener.onComponentEvent(
                        new FocusedDateChangeEvent<R>((R) this,
                                event.isUserOriginated())));
    }
}