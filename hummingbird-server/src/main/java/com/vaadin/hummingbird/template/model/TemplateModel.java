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

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.TemplateModelData;
import com.vaadin.ui.Template;

/**
 * Interface for a {@link Template}'s model. Extending this interface and adding
 * getters and setters makes it possible to easily bind data to a template.
 * <p>
 * Currently supported property types:
 * <ul>
 * <li>NONE</li>
 * </ul>
 *
 * @author Vaadin Ltd
 */
public interface TemplateModel extends Serializable {

    /**
     * Gets the model for the given template.
     *
     * @param stateNode
     *            the state node of the template
     * @param templateType
     *            the type of the template
     * @return the template's model
     */
    static TemplateModel getTemplateModel(StateNode stateNode,
            Class<? extends Template> templateType) {
        assert stateNode.hasFeature(
                TemplateModelData.class) : "State node doesn't belong to a template's root element";

        TemplateModelData templateModelData = stateNode
                .getFeature(TemplateModelData.class);

        return templateModelData.getModel().orElse(TemplateModel
                .createAndStoreTemplateModelInstance(stateNode, templateType));
    }

    /**
     * Creates a new model for the given template and stores it in the
     * template's state node.
     * <p>
     * To fetch a previously created model for template, use
     * {@link #getTemplateModel(StateNode, Class)} instead.
     *
     * @param stateNode
     *            the state node of the template
     * @param templateType
     *            the type of the template
     * @return the template's model
     */
    static TemplateModel createAndStoreTemplateModelInstance(
            StateNode stateNode, Class<? extends Template> templateType) {
        Class<? extends TemplateModel> modelType = TemplateModelTypeParser
                .getType(templateType);

        TemplateModel templateModel = TemplateModelProxy.createProxy(modelType);

        stateNode.getFeature(TemplateModelData.class).setModel(templateModel);

        return templateModel;
    }

}
