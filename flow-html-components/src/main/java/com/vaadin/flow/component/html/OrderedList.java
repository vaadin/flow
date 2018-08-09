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
package com.vaadin.flow.component.html;

import java.util.stream.Stream;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;ol&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.OL)
public class OrderedList extends HtmlContainer
        implements ClickNotifier<OrderedList> {

    /**
     * Defines the numbering type of the list items.
     */
    public enum NumberingType {

        /**
         * The list items will be numbered with numbers (default).
         */
        NUMBER("1"),

        /**
         * The list items will be numbered with uppercase letters.
         */
        UPPERCASE_LETTER("A"),

        /**
         * The list items will be numbered with lowercase letters.
         */
        LOWERCASE_LETTER("a"),

        /**
         * The list items will be numbered with uppercase Roman numbers.
         */
        UPPERCASE_ROMAN("I"),

        /**
         * The list items will be numbered with lowercase Roman numbers.
         */
        LOWERCASE_ROMAN("i");

        private final String value;

        NumberingType(String value) {
            this.value = value;
        }

        private static NumberingType fromAttributeValue(String value) {
            return Stream.of(values()).filter(type -> type.value.equals(value))
                    .findFirst().orElseThrow(IllegalArgumentException::new);
        }
    }

    private static final PropertyDescriptor<String, String> typeDescriptor = PropertyDescriptors
            .attributeWithDefault("type", NumberingType.NUMBER.value);

    /**
     * Creates a new empty ordered list.
     */
    public OrderedList() {
        super();
    }

    /**
     * Creates a new empty ordered list with the specified
     * {@link NumberingType}.
     *
     * @param type
     *            the numbering type of the list items
     */
    public OrderedList(NumberingType type) {
        super();
        setType(type);
    }

    /**
     * Creates a new ordered list with the given list items.
     *
     * @param items
     *            the list items
     */
    public OrderedList(ListItem... items) {
        super(items);
    }

    public NumberingType getType() {
        String value = get(typeDescriptor);
        try {
            return NumberingType.fromAttributeValue(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "The attribute type has an illegal value: " + value, e);
        }
    }

    public void setType(NumberingType type) {
        set(typeDescriptor, type.value);
    }
}
