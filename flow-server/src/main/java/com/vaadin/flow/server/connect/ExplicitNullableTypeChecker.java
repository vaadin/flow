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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

/**
 * A checker for TypeScript null compatibility in Vaadin Connect service methods
 * parameter and return types.
 */
public class ExplicitNullableTypeChecker {
    ExplicitNullableTypeChecker recursiveInstance = this;

    /**
     * Validates the given value for the given expected method parameter or
     * return value type.
     *
     * @param value
     *            the value to validate
     * @param expectedType
     *            the declared type expected for the value
     * @return error message when the value is null while the expected type does
     *         not explicitly allow null, or null meaning the value is OK.
     */
    public String checkValueForType(Object value, Type expectedType) {
        Class<?> clazz = (Class<?>) expectedType;

        if (value != null) {
            if (Collection.class.isAssignableFrom(clazz)
                    && checkCollection((Collection) value, clazz) != null) {
                return String.format(
                        "Unexpected null item in collection type '%s'",
                        expectedType.getTypeName());
            }

            return null;
        }

        if (expectedType.equals(Void.TYPE)) {
            // Corner case: void methods return null value by design
            return null;
        }

        if (Void.class.isAssignableFrom(clazz)) {
            // Corner case: explicit Void parameter
            return null;
        }

        if (Optional.class.isAssignableFrom(clazz)) {
            return String.format(
                    "Got null value for type '%s', consider Optional.empty",
                    expectedType.getTypeName());
        }

        return String.format(
                "Got null value for type '%s', which is neither Optional"
                        + " nor void",
                expectedType.getTypeName());
    }

    private String checkCollection(Collection value, Class<?> clazz) {
        for (Type type : clazz.getTypeParameters()[0].getBounds()) {
            for (Object item : value) {
                String error = recursiveInstance.checkValueForType(item, type);
                if (error != null) {
                    return error;
                }
            }
        }

        return null;
    }
}
