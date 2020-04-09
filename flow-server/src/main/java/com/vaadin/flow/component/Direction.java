/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.component;

/**
 * Specifies the direction for content.
 *
 * @author Vaadin Ltd
 * @since 3.1
 */
public enum Direction {

    RIGHT_TO_LEFT("rtl"), LEFT_TO_RIGHT("ltr");

    private final String clientName;

    Direction(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Gets the value applied as the {@code dir} attribute in html for
     * {@code document.documentElement}.
     *
     * @return the value applied as the "dir" attribute.
     */
    public String getClientName() {
        return clientName;
    }

}
