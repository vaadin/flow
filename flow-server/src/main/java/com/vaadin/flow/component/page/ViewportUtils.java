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

package com.vaadin.flow.component.page;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Util class for {@link Viewport}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ViewportUtils {

    /**
     * Generates the the viewport tag content from the secondary fields.
     *
     * @param viewportAnnotation
     *            viewport annotation
     * @return viewport the viewport tag content
     */
    public static String generateViewport(Viewport viewportAnnotation) {
        Objects.requireNonNull(viewportAnnotation);

        List<String> viewportAttributes = new ArrayList<>();

        Method[] methods = viewportAnnotation.getClass().getMethods();
        String attributeName, attributeValue;

        for (Method method : methods) {
            attributeName = convertFromCamelToHyphenStyle(
                    method.getName());
            try {
                attributeValue = String
                        .valueOf(method.invoke(viewportAnnotation));
                viewportAttributes.add(attributeName + "=" + attributeValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        return String.join(", ", viewportAttributes);
    }

    private static String convertFromCamelToHyphenStyle(
            final String camel) {

        return camel.replaceAll("(?=[A-Z][a-z])","-").toLowerCase();
    }
}
