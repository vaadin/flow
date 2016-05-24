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
package com.vaadin.hummingbird.template.model;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.ui.Template;

/**
 * Parses and stores template model type information.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelTypeParser {

    protected static final Map<Class<? extends Template>, Class<? extends TemplateModel>> templateToModelTypeMap = new HashMap<>();

    private TemplateModelTypeParser() {
        // NOOP
    }

    /**
     * Gets the corresponding {@link TemplateModel} type for the given template
     * type.
     *
     * @param templateType
     *            the template type
     * @return the model type of the template
     */
    protected static Class<? extends TemplateModel> getType(
            Class<? extends Template> templateType) {
        return templateToModelTypeMap.computeIfAbsent(templateType,
                TemplateModelTypeParser::readTemplateModelType);
    }

    private static Class<? extends TemplateModel> readTemplateModelType(
            final Class<? extends Template> templateType) {

        Class<?> type = templateType;
        while (type != Template.class) {
            try {
                Method method = type.getDeclaredMethod("getModel");
                return method.getReturnType().asSubclass(TemplateModel.class);
            } catch (NoSuchMethodException e) {
                type = type.getSuperclass();
            }
        }
        return TemplateModel.class;
    }

}
