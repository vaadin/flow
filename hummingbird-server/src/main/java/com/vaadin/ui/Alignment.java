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

/**
 * Class containing information about alignment of a component. Use the
 * pre-instantiated classes.
 */
public enum Alignment {

    TOP_LEFT(""), TOP_CENTER("top-center"), TOP_RIGHT("top-right"),

    MIDDLE_LEFT("middle-left"), MIDDLE_CENTER("middle-center"), MIDDLE_RIGHT(
            "middle-right"),

    BOTTOM_LEFT("bottom-left"), BOTTOM_CENTER("bottom-center"), BOTTOM_RIGHT(
            "bottom-right");

    private String className;

    private Alignment(String className) {
        this.className = className;
    }

    /**
     * Returns the class name for this alignment
     *
     * @return the class name for this alignment
     */
    public String getClassName() {
        return className;
    }

}
