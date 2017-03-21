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
package com.vaadin.hummingbird.template;

import java.lang.reflect.Type;
import java.util.List;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.template.model.ModelDescriptor;
import com.vaadin.hummingbird.template.model.ModelType;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

/**
 * Component for an HTML element declared as a polymer component. The HTML
 * markup should be loaded using the {@link HtmlImport @HtmlImport} annotation
 * and the components should be associated with the web component element using
 * the {@link Tag @Tag} annotation.
 *
 * @param <M>
 *            a model class that will be used for template data propagation
 *
 * @see HtmlImport
 * @see Tag
 *
 * @author Vaadin Ltd
 */
public abstract class PolymerTemplate<M extends TemplateModel>
        extends AbstractTemplate<M> {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // initialize the model so that all properties are sent to the client
        getModel();
    }

    @Override
    protected void updateModelDescriptor(
            ModelDescriptor<? extends M> currentDescriptor) {

        getStateNode()
                .runWhenAttached(ui -> registerModel(ui, currentDescriptor));
    }

    private void registerModel(UI ui, ModelDescriptor<? extends M> descriptor) {
        StringBuilder jsCode = new StringBuilder("var existingProperties=[];");
        descriptor.getPropertyNames()
                .forEach(property -> jsCode.append(String.format(
                        "if ($0.constructor.__classProperties['%s'] !== undefined){ existingProperties.push('%s');} "
                                + "else { %s } ",
                        property, property,
                        generateRegistrationCode(property, descriptor))));
        jsCode.append("for( i=0; i<existingProperties.length; i++){ ");
        jsCode.append(
                "console.warn('Property \"'+existingProperties[i]+'\" is already defined on the client side.");
        jsCode.append(
                " Check your polymer class definition for the element \"");
        jsCode.append(getElement().getTag());
        jsCode.append("\". The property will not be redefined'); } ");
        ui.getPage().executeJavaScript(jsCode.toString(), getElement());
    }

    private String generateRegistrationCode(String property,
            ModelDescriptor<? extends M> descriptor) {
        StringBuilder registrationCode = new StringBuilder();
        ModelType modelType = descriptor.getPropertyType(property);
        Type type = modelType.getJavaType();

        String jsType = "Object";
        String jsValue = "null";
        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            jsType = "Boolean";
            jsValue = "false";
        } else if (type.equals(String.class)) {
            jsType = "String";
        } else if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isPrimitive()
                    && Number.class.isAssignableFrom(
                            ReflectTools.convertPrimitiveType(clazz))
                    || Number.class.isAssignableFrom(clazz)) {
                jsType = "Number";
                jsValue = "0";
            }
        } else if (List.class
                .isAssignableFrom(GenericTypeReflector.erase(type))) {
            jsType = "Array";
            jsValue = "[]";
        }
        registrationCode.append(String.format(
                "$0.constructor.__classProperties['%s'] = {}; "
                        + "$0.constructor.__classProperties['%s'].type=%s;"
                        + "$0.constructor.__classProperties['%s'].value=%s;",
                property, property, jsType, property, jsValue));

    // @formatter:off  
        /*
         * Plain property doesn't need to be created in a specific way. The code
         * above is enough.
         * 
         * Properties with different effects are created like this:
         * 
             function createPropertyFromConfig(proto, name, info, allProps) {
              // computed forces readOnly...
              if (info.computed) {
                info.readOnly = true;
              }
              // Note, since all computed properties are readOnly, this prevents
              // adding additional computed property effects (which leads to a confusing
              // setup where multiple triggers for setting a property)
              // While we do have `hasComputedEffect` this is set on the property's
              // dependencies rather than itself.
              if (info.computed  && !proto._hasReadOnlyEffect(name)) {
                proto._createComputedProperty(name, info.computed, allProps);
              }
              if (info.readOnly && !proto._hasReadOnlyEffect(name)) {
                proto._createReadOnlyProperty(name, !info.computed);
              }
              if (info.reflectToAttribute && !proto._hasReflectEffect(name)) {
                proto._createReflectedProperty(name);
              }
              if (info.notify && !proto._hasNotifyEffect(name)) {
                proto._createNotifyingProperty(name);
              }
              // always add observer
              if (info.observer) {
                proto._createPropertyObserver(name, info.observer, allProps[info.observer]);
              }
            }
            
            The default value is set depending on whether the property has an effect or not:
            
            if (this._hasPropertyEffect(p)) {
              this._setProperty(p, value)
            } else {
              this[p] = value;
            }
         */
        // @formatter:on
        registrationCode
                .append(String.format("$0['%s']=%s;", property, jsValue));
        return registrationCode.toString();
    }

}
