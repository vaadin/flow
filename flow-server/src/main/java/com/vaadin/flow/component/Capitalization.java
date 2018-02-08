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
package com.vaadin.flow.component;

/**
 * Capitalization options for the {@code autocapitalize} attribute.
 */
public enum Capitalization {

    /**
     * Completely disable automatic capitalization.
     */
    NONE("none"),

    /**
     * Automatically capitalize the first letter of sentences.
     */
    SENTENCES("sentences"),

    /**
     * Automatically capitalize the first letter of words.
     */
    WORDS("words"),

    /**
     * Automatically capitalize all characters.
     */
    CHARACTERS("characters");

    final String value;

    Capitalization(String value) {
        this.value = value;
    }
}
