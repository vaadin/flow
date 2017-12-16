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
package com.vaadin.flow.template.angular.model;

import java.lang.reflect.Method;

import com.vaadin.flow.template.angular.AngularTemplate;
import com.vaadin.flow.util.ReflectionCache;

/**
 * Parses and stores template model type information.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class TemplateModelTypeParser {

    protected static final ReflectionCache<AngularTemplate, Class<? extends TemplateModel>> cache = new ReflectionCache<>(
            TemplateModelTypeParser::readTemplateModelType);

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
    public static Class<? extends TemplateModel> getType(
            Class<? extends AngularTemplate> templateType) {
        return cache.get(templateType);
    }

    static Class<? extends TemplateModel> readTemplateModelType(
            final Class<? extends AngularTemplate> templateType) {

        Class<?> type = templateType;
        while (type != AngularTemplate.class) {
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
