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
package com.vaadin.shared;

public enum Position {
    TOP_LEFT("top", "left"), TOP_CENTER("top", "center"), TOP_RIGHT("top",
            "right"), MIDDLE_LEFT("left"), MIDDLE_CENTER(
                    "center"), MIDDLE_RIGHT("right"), BOTTOM_LEFT(
                            "bottom left"), BOTTOM_CENTER(
                                    "bottom center"), BOTTOM_RIGHT(
                                            "bottom right"),

    /**
     * Position that is only accessible for assistive devices, invisible for
     * visual users.
     **/
    ASSISTIVE;

    private String[] classNames;

    private Position(String... classNames) {
        this.classNames = classNames;
    }

    public String[] getClassNames() {
        return classNames;
    }

    public static String[] getAllClassNames() {
        return new String[] { "top", "bottom", "left", "center", "right" };
    }
}
