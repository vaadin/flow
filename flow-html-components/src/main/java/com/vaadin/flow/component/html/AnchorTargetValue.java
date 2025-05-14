/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents <code>target</code> attribute values for an <code>&lt;a&gt;</code>
 * element.
 *
 * @author Vaadin Ltd
 * @since
 *
 * @see AnchorTarget
 *
 */
@FunctionalInterface
public interface AnchorTargetValue extends Serializable {

    /**
     * Gets the string value representation.
     *
     * @return string value representation
     */
    String getValue();

    /**
     * Gets an object instance wrapping the {@code value} string representation.
     *
     * @param value
     *            the string value representation, not {@code null}
     * @return an object wrapping the string value
     */
    public static AnchorTargetValue forString(String value) {
        Optional<AnchorTarget> target = Stream.of(AnchorTarget.values()).filter(
                type -> type.getValue().equals(Objects.requireNonNull(value)))
                .findFirst();
        if (target.isPresent()) {
            return target.get();
        }
        return new AnchorTargetValue() {

            @Override
            public String getValue() {
                return value;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (obj == this) {
                    return true;
                }
                if (!obj.getClass().equals(getClass())) {
                    return false;
                }
                return value.equals(((AnchorTargetValue) obj).getValue());
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }
        };
    }
}
