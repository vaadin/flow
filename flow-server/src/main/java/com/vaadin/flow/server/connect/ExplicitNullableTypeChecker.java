/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.server.connect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * A checker for TypeScript null compatibility in Vaadin Connect service methods
 * parameter and return types.
 */
public class ExplicitNullableTypeChecker {
    /**
     * Validates the given value for the given expected method parameter
     * or return value type.
     *
     * @param value
     *          the value to validate
     */
    public String checkValueForType(Object value, Type expectedType) {
        if (expectedType instanceof ParameterizedType) {
            Class<?> clazz = (Class<?>) ((ParameterizedType) expectedType).getRawType();
            if (Optional.class.isAssignableFrom(clazz)) {
                if (value == null) {
                    return String.format(
                            "Got null value for type '%s', consider Optional.empty",
                            expectedType.getTypeName()
                    );
                } else {
                    return null;
                }
            }
        }

        if (value != null) {
            return null;
        }

        if (expectedType.equals(Void.TYPE)) {
            // Corner case: void methods return null value by design
            return null;
        }

        if (expectedType instanceof Class<?>
                && Void.class.isAssignableFrom((Class<?>) expectedType)) {
            // Corner case: explicit Void parameter
            return null;
        }

        return String.format(
                "Got null value for type '%s', which is neither Optional"
                        + " nor void",
                expectedType.getTypeName()
        );
    }
}
