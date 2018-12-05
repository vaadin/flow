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

        StringBuilder viewport = new StringBuilder();

        final String delimiter = ", ";

        Method[] methods = viewportAnnotation.getClass().getMethods();
        for (Method method : methods) {
            String attributeName = convertMethodNameToViewportAttributeName(
                    method.getName());
            String attributeValue = null;

            // TODO: treat the reflection exception properly
            try {
                attributeValue = String
                        .valueOf(method.invoke(viewportAnnotation));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if (attributeValue != null && !attributeValue.isEmpty()) {
                viewport.append(attributeName);
                viewport.append("=");
                viewport.append(attributeValue);
                viewport.append(delimiter);
            }
        }

        if (viewport.length() >= delimiter.length()) {
            viewport.delete(viewport.length() - delimiter.length(),
                    viewport.length());
        }

        return viewport.toString();
    }

    private static String convertMethodNameToViewportAttributeName(
            final String methodName) {
        StringBuilder viewPortAttributeName = new StringBuilder();

        for (int i = 0, n = methodName.length(); i < n; i++) {
            char c = methodName.charAt(i);
            if (Character.isUpperCase(c)) {
                viewPortAttributeName.append('-');
                viewPortAttributeName.append(Character.toLowerCase(c));
            } else {
                viewPortAttributeName.append(c);
            }
        }

        return viewPortAttributeName.toString();
    }
}
