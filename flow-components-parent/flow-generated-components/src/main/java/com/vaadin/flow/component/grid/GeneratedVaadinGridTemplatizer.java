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
package com.vaadin.flow.component.grid;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import elemental.json.JsonObject;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code vaadin-grid-templatizer} is a helper element for the
 * {@code vaadin-grid} that is preparing and stamping instances of cells and
 * columns templates
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.GridTemplatizer#UNKNOWN", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-grid-templatizer")
@HtmlImport("frontend://bower_components/vaadin-grid/src/vaadin-grid-templatizer.html")
public abstract class GeneratedVaadinGridTemplatizer<R extends GeneratedVaadinGridTemplatizer<R>>
        extends Component implements HasStyle {

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code dataHost} property from the webcomponent
     */
    protected JsonObject getDataHostJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("dataHost");
    }

    /**
     * @param dataHost
     *            the JsonObject value to set
     */
    protected void setDataHost(JsonObject dataHost) {
        getElement().setPropertyJson("dataHost", dataHost);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code template} property from the webcomponent
     */
    protected JsonObject getTemplateJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("template");
    }

    /**
     * @param template
     *            the JsonObject value to set
     */
    protected void setTemplate(JsonObject template) {
        getElement().setPropertyJson("template", template);
    }

    protected void createInstance() {
        getElement().callFunction("createInstance");
    }

    /**
     * @param instance
     *            Missing documentation!
     */
    protected void addInstance(JsonObject instance) {
        getElement().callFunction("addInstance", instance);
    }

    /**
     * @param instance
     *            Missing documentation!
     */
    protected void removeInstance(JsonObject instance) {
        getElement().callFunction("removeInstance", instance);
    }
}