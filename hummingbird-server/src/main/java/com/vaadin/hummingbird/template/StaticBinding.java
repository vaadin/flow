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
package com.vaadin.hummingbird.template;

import com.vaadin.hummingbird.StateNode;

/**
 * A template binding that always produces the same value.
 *
 * @since
 * @author Vaadin Ltd
 */
public class StaticBinding implements TemplateBinding {
    private final String value;

    /**
     * Creates a binding with the given value.
     *
     * @param value
     *            the value of the binding
     */
    public StaticBinding(String value) {
        this.value = value;
    }

    @Override
    public Object getValue(StateNode node) {
        return value;
    }
}
