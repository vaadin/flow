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
package com.vaadin.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.components.JsonSerializable;
import com.vaadin.external.jsoup.helper.StringUtil;
import com.vaadin.generated.vaadin.form.layout.GeneratedVaadinFormItem;
import com.vaadin.generated.vaadin.form.layout.GeneratedVaadinFormLayout;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Server-side component for the {@code <vaadin-form-layout>} element.
 * <p>
 * TODO
 * 
 * @author Vaadin Ltd
 */
public class VaadinFormLayout
        extends GeneratedVaadinFormLayout<VaadinFormLayout> {

    /**
     * 
     * @author Vaadin Ltd
     */
    public static class ResponsiveStep implements JsonSerializable {

        public enum LabelsPosition {
            ASIDE, TOP;

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
         * TODO
         * 
         * @param minWidth
         * @param columns
         */
        public ResponsiveStep(String minWidth, int columns) {
            this.minWidth = minWidth;
            this.columns = columns;
        }

        /**
         * TODO
         * 
         * @param minWidth
         * @param columns
         * @param labelsPosition
         */
        public ResponsiveStep(String minWidth, int columns,
                LabelsPosition labelsPosition) {
            this.minWidth = minWidth;
            this.columns = columns;
            this.labelsPosition = labelsPosition;
        }

        /**
         * 
         * @param value
         */
        private ResponsiveStep(JsonObject value) {
            readJson(value);
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
     * Server-side component for the {@code <vaadin-form-item>} element.
     * <p>
     * TODO
     * 
     * @author Vaadin Ltd
     */
    public static class VaadinFormItem
            extends GeneratedVaadinFormItem<VaadinFormItem> {

        /**
         * TODO
         * 
         * @param components
         *            the components to add
         * @see HasComponents#add(Component...)
         */
        public VaadinFormItem(com.vaadin.ui.Component... components) {
            super(components);
        }

        /**
         * TODO
         */
        public VaadinFormItem() {
            super();
        }
    }

    /**
     * TODO
     * 
     * @param components
     *            the components to add
     * @see HasComponents#add(Component...)
     */
    public VaadinFormLayout(com.vaadin.ui.Component... components) {
        super(components);
    }

    /**
     * TODO
     */
    public VaadinFormLayout() {
        super();
    }

    /**
     * TODO
     * 
     * @return
     */
    public List<ResponsiveStep> getResponsiveStepsWrapped() {
        JsonArray stepsJsonArray = (JsonArray) getElement()
                .getPropertyRaw("responsiveSteps");
        List<ResponsiveStep> steps = new ArrayList<>();
        for (int i = 0; i < stepsJsonArray.length(); i++) {
            steps.add(stepsJsonArray.get(i));
        }
        return steps;
    }

    /**
     * TODO
     * 
     * @param steps
     * @return
     */
    public VaadinFormLayout setResponsiveSteps(List<ResponsiveStep> steps) {
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
     * TODO
     * 
     * @param steps
     * @return
     */
    public VaadinFormLayout setResponsiveSteps(ResponsiveStep... steps) {
        return setResponsiveSteps(Arrays.asList(steps));
    }

    /**
     * TODO
     */
    @Override
    public JsonObject getResponsiveSteps() {
        throw new UnsupportedOperationException("");
    }

    /**
     * TODO
     */
    @Override
    public VaadinFormLayout setResponsiveSteps(
            elemental.json.JsonObject responsiveSteps) {
        throw new UnsupportedOperationException("");
    }
}
