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
package com.vaadin.ui.formlayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.helper.StringUtil;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasComponents;
import com.vaadin.ui.common.HasSize;
import com.vaadin.ui.common.JsonSerializable;
import com.vaadin.ui.html.Label;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Server-side component for the {@code <vaadin-form-layout>} element.
 *
 * @author Vaadin Ltd
 */
public class FormLayout extends GeneratedVaadinFormLayout<FormLayout>
        implements HasSize {

    /**
     * A class used in describing the responsive layouting behavior of a
     * {@link FormLayout}.
     *
     * @author Vaadin Ltd
     */
    public static class ResponsiveStep implements JsonSerializable {

        /**
         * Enum for describing the position of label components in a
         * {@link FormItem}.
         */
        public enum LabelsPosition {

            /**
             * Labels are displayed on the left hand side of the wrapped
             * component.
             */
            ASIDE,

            /**
             * Labels are displayed atop the wrapped component.
             */
            TOP;

            @Override
            public String toString() {
                return name().toLowerCase(Locale.ENGLISH);
            }
        }

        private static final String MIN_WIDTH_JSON_KEY = "minWidth";
        private static final String COLUMNS_JSON_KEY = "columns";
        private static final String LABELS_POSITION_JSON_KEY = "labelsPosition";

        private String minWidth;
        private int columns;
        private LabelsPosition labelsPosition;

        /**
         * Constructs a ResponsiveStep with the given minimum width and number
         * of columns.
         *
         * @param minWidth
         *            the minimum width as a CSS string value after which this
         *            responsive step is to be applied
         * @param columns
         *            the number of columns the layout should have
         */
        public ResponsiveStep(String minWidth, int columns) {
            this.minWidth = minWidth;
            this.columns = columns;
        }

        /**
         * Constructs a ResponsiveStep with the given minimum width, number of
         * columns and label position.
         *
         * @see LabelsPosition
         * @see FormItem
         *
         * @param minWidth
         *            the minimum width as a CSS string value after which this
         *            responsive step is to be applied
         * @param columns
         *            the number of columns the layout should have
         * @param labelsPosition
         *            the position where label components are to be displayed in
         *            {@link FormItem}s
         */
        public ResponsiveStep(String minWidth, int columns,
                LabelsPosition labelsPosition) {
            this.minWidth = minWidth;
            this.columns = columns;
            this.labelsPosition = labelsPosition;
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = Json.createObject();
            if (!StringUtil.isBlank(minWidth)) {
                json.put(MIN_WIDTH_JSON_KEY, minWidth);
            }
            json.put(COLUMNS_JSON_KEY, columns);
            if (labelsPosition != null) {
                json.put(LABELS_POSITION_JSON_KEY, labelsPosition.toString());
            }
            return json;
        }

        @Override
        public ResponsiveStep readJson(JsonObject value) {
            minWidth = value.getString(MIN_WIDTH_JSON_KEY);
            columns = (int) value.getNumber(COLUMNS_JSON_KEY);
            String labelsPositionString = value
                    .getString(LABELS_POSITION_JSON_KEY);
            if ("aside".equals(labelsPositionString)) {
                labelsPosition = LabelsPosition.ASIDE;
            } else if ("top".equals(labelsPositionString)) {
                labelsPosition = LabelsPosition.TOP;
            } else {
                labelsPosition = null;
            }
            return this;
        }
    }

    /**
     * Server-side component for the {@code <vaadin-form-item>} element. Used to
     * wrap components for display in a {@link FormLayout}.
     *
     * @author Vaadin Ltd
     */
    public static class FormItem extends GeneratedVaadinFormItem<FormItem> {

        /**
         * Constructs a FormItem with the given initial components to wrap.
         * Additional components can be added after construction with
         * {@link #add(Component...)}.
         *
         * @param components
         *            the initial components to wrap as a form item.
         * @see HasComponents#add(Component...)
         */
        public FormItem(Component... components) {
            super(components);
        }
    }

    /**
     * Constructs an empty layout. Components can be added with
     * {@link #add(Component...)}.
     */
    public FormLayout() {
    }

    /**
     * Constructs a FormLayout with the given initial components. Additional
     * components can be added after construction with
     * {@link #add(Component...)}.
     *
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public FormLayout(Component... components) {
        super(components);
    }

    /**
     * Get the list of {@link ResponsiveStep}s used to configure this layout.
     *
     * @see ResponsiveStep
     *
     * @return the list of {@link ResponsiveStep}s used to configure this layout
     */
    public List<ResponsiveStep> getResponsiveSteps() {
        JsonArray stepsJsonArray = (JsonArray) getElement()
                .getPropertyRaw("responsiveSteps");
        List<ResponsiveStep> steps = new ArrayList<>();
        for (int i = 0; i < stepsJsonArray.length(); i++) {
            steps.add(stepsJsonArray.get(i));
        }
        return steps;
    }

    /**
     * Configure the responsive steps used in this layout.
     *
     * @see ResponsiveStep
     *
     * @param steps
     *            list of {@link ResponsiveStep}s to set
     * @return this instance, for method chaining
     */
    public FormLayout setResponsiveSteps(List<ResponsiveStep> steps) {
        AtomicInteger index = new AtomicInteger();
        getElement().setPropertyJson("responsiveSteps",
                steps.stream().map(ResponsiveStep::toJson).collect(
                        () -> Json.createArray(),
                        (arr, value) -> arr.set(index.getAndIncrement(), value),
                        (arr, arrOther) -> {
                            int startIndex = arr.length();
                            for (int i = 0; i < arrOther.length(); i++) {
                                JsonValue value = arrOther.get(i);
                                arr.set(startIndex + i, value);
                            }
                        }));
        return get();
    }

    /**
     * Configure the responsive steps used in this layout.
     *
     * @see ResponsiveStep
     *
     * @param steps
     *            the {@link ResponsiveStep}s to set
     * @return this instance, for method chaining
     */
    public FormLayout setResponsiveSteps(ResponsiveStep... steps) {
        return setResponsiveSteps(Arrays.asList(steps));
    }

    /**
     * Convenience method for creating and adding a new FormItem to this layout
     * that wraps the given field with a label. Shorthand for
     * {@code addFormItem(field, new Label(label))}.
     *
     * @see #addFormItem(Component, Component)
     *
     * @param field
     *            the field component to wrap
     * @param label
     *            the label text to set
     * @return the created form item
     */
    public FormItem addFormItem(Component field, String label) {
        return addFormItem(field, new Label(label));
    }

    /**
     * Convenience method for creating and adding a new FormItem to this layout
     * that wraps the given field with a component as its label.
     *
     * @param field
     *            the field component to wrap
     * @param label
     *            the label component to set
     * @return the created form item
     */
    public FormItem addFormItem(Component field, Component label) {
        FormItem formItem = new FormItem(field);
        formItem.addToLabel(label);
        add(formItem);
        return formItem;
    }
}
