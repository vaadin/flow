/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui.webcomponent;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasText;

/**
 * An integration of https://github.com/tehapo/progress-bubble.
 *
 * @author Vaadin Ltd
 */
@Tag("progress-bubble")
@HtmlImport(PolyGit.BASE_URL + "progress-bubble/progress-bubble.html")
public class ProgressBubble extends Component
        implements HasValue, HasMax, HasText {

    /**
     * Creates a new progress bubble with the default value (0) and max value
     * (100).
     */
    public ProgressBubble() {
        // Using default values
    }

    /**
     * Creates a new progress bubble with the given value and max value.
     *
     * @param value
     *            the initial value to use
     * @param max
     *            the max value to use
     */
    public ProgressBubble(int value, int max) {
        setValue(value);
        setMax(max);
    }

    @Override
    public void setValue(int value) {
        HasValue.super.setValue(value);
        setText(value + " %");
    }

}
