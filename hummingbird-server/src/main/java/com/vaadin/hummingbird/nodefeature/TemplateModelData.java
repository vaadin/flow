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
package com.vaadin.hummingbird.nodefeature;

import java.util.Optional;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.template.model.TemplateModel;

/**
 * A server side only node feature for storing a template's {@link TemplateModel
 * model}.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelData extends ServerSideFeature {

    private TemplateModel model;

    /**
     * Creates an instance of this node feature.
     *
     * @param node
     *            the node this feature belongs to
     */
    public TemplateModelData(StateNode node) {
        super(node);
    }

    /**
     * Gets the model for this template, if any.
     *
     * @return the model, or an empty optional if none has been set
     */
    public Optional<TemplateModel> getModel() {
        return Optional.ofNullable(model);
    }

    /**
     * Assigns the given model to this node.
     *
     * @param model
     *            the model to assign, or <code>null</code> to remove a
     *            previously assigned model
     */
    public void setModel(TemplateModel model) {
        this.model = model;
    }

}
