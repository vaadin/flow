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
package com.vaadin.flow.uitest.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.template.angular.AngularTemplate;
import com.vaadin.flow.template.angular.model.TemplateModel;

/**
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the model type for this inline template
 */
public class InlineTemplate<T extends TemplateModel> extends AngularTemplate {

    private Class<T> modelType;

    /**
     * Creates a template instance with the given template HTML text and model
     * type.
     * <p>
     * Note: super constructor uses the {@code String} parameter as a file path,
     * not as a content.
     *
     * @param templateHtml
     *            the template HTML
     * @param modelType
     *            the type of the model to use with this template
     */
    public InlineTemplate(String templateHtml, Class<T> modelType) {
        super(new ByteArrayInputStream(
                templateHtml.getBytes(StandardCharsets.UTF_8)));
        this.modelType = modelType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getModel() {
        // Exposed as public
        return (T) super.getModel();
    }

    @Override
    protected Class<? extends TemplateModel> getModelType() {
        return modelType;
    }

}
