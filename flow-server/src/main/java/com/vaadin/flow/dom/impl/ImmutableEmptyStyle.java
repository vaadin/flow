/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.dom.impl;

import java.util.stream.Stream;

import com.vaadin.flow.dom.Style;

/**
 * A style implementation which is empty and immutable.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ImmutableEmptyStyle implements Style {

    static final String CANT_MODIFY_MESSAGE = "This instance is immutable";

    @Override
    public String get(String name) {
        return null;
    }

    @Override
    public Style set(String name, String value) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Style remove(String name) {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public Style clear() {
        throw new UnsupportedOperationException(CANT_MODIFY_MESSAGE);
    }

    @Override
    public boolean has(String name) {
        return false;
    }

    @Override
    public Stream<String> getNames() {
        return Stream.empty();
    }
}
