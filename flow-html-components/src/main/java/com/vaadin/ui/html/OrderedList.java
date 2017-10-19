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
package com.vaadin.ui.html;

import com.vaadin.ui.common.HtmlContainer;
import com.vaadin.ui.common.PropertyDescriptor;
import com.vaadin.ui.common.PropertyDescriptors;
import com.vaadin.ui.event.ClickNotifier;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

/**
 * Component representing a <code>&lt;ol&gt;</code> element.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.OL)
public class OrderedList extends HtmlContainer implements ClickNotifier {

    public static enum ItemType {

        /**
         * The list items will be numbered with numbers (default).
         */
        NUMBERS("1"),

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

        private ItemType(String value) {
            this.value = value;
        }

        private static ItemType fromAttributeValue(String value) {
            if (ItemType.NUMBERS.value.equals(value)) {
                return ItemType.NUMBERS;
            } else if (ItemType.UPPERCASE_LETTER.value.equals(value)) {
                return ItemType.UPPERCASE_LETTER;
            } else if (ItemType.LOWERCASE_LETTER.value.equals(value)) {
                return ItemType.LOWERCASE_LETTER;
            } else if (ItemType.UPPERCASE_ROMAN.value.equals(value)) {
                return ItemType.UPPERCASE_ROMAN;
            } else if (ItemType.LOWERCASE_ROMAN.value.equals(value)) {
                return ItemType.LOWERCASE_ROMAN;
            } else {
                throw new IllegalArgumentException(value);
            }
        }
    }

    private static final PropertyDescriptor<String, String> typeDescriptor = PropertyDescriptors
            .attributeWithDefault("type", ItemType.NUMBERS.value);

    /**
     * Creates a new empty ordered list.
     */
    public OrderedList() {
        super();
    }

    /**
     * Creates a new empty ordered list with the specified {@link ItemType}.
     */
    public OrderedList(ItemType type) {
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

    public ItemType getType() {
        String value = get(typeDescriptor);
        try {
            return ItemType.fromAttributeValue(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "The attribute type has an illegal value: " + value, e);
        }
    }

    public void setType(ItemType type) {
        set(typeDescriptor, type.value);
    }
}
