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
package com.vaadin.generator;

import java.util.List;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import com.vaadin.flow.component.JsonSerializable;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.generator.exception.ComponentGenerationException;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentObjectType;
import com.vaadin.generator.metadata.ComponentObjectType.ComponentObjectTypeInnerType;

import elemental.json.JsonObject;

/**
 * Nested POJOs Generator.
 * <p>
 * Nested POJOs represent complex data structures. Those POJOs are meant to be
 * static nested classes of the main component class. The nested POJOs can be
 * used in method calls, property getters/setters and inside events.
 *
 * @since 1.0
 */
public class NestedClassGenerator {

    private ComponentObjectType type;
    private String nameHint;
    private boolean fluentSetters = true;

    /**
     * Sets the {@link ComponentObjectType} this generator will use to create
     * the getters and setters for the properties.
     *
     * @param type
     *            The ComponentObjectType instance containing the object
     *            definition.
     * @return this instance for method chaining.
     */
    public NestedClassGenerator withType(ComponentObjectType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the name hint that will be used to generate the final name of the
     * generated class. The name hint is can contain invalid characters,
     * multiple words and reserved words - all of those are properly formatted
     * before setting the final name for the class.
     *
     * @param nameHint
     *            The hint which is the base for the final name of the class.
     * @return this instance for method chaining.
     */
    public NestedClassGenerator withNameHint(String nameHint) {
        this.nameHint = nameHint;
        return this;
    }

    /**
     * Sets whether the generated POJO should contain fluent setters or not.
     * Fluent setters allow method chaining. The default is <code>true</code>.
     *
     * @param fluentSetters
     *            <code>true</code> to enable fluent setters, <code>false</code>
     *            otherwise.
     * @return this instance for method chaining.
     */
    public NestedClassGenerator withFluentSetters(boolean fluentSetters) {
        this.fluentSetters = fluentSetters;
        return this;
    }

    /**
     * Builds the Java class by using the defined settings.
     *
     * @return the final Java class source, ready to be embedded in the main
     *         class of the component.
     */
    public JavaClassSource build() {
        JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.addInterface(JsonSerializable.class).setPublic()
                .setStatic(true).setName(ComponentGeneratorUtils
                        .generateValidJavaClassName(nameHint));

        javaClass.addField().setType(JsonObject.class).setPrivate()
                .setName("internalObject");

        for (ComponentObjectTypeInnerType object : type.getInnerTypes()) {
            ComponentBasicType simpleType = getSimpleBasicType(
                    object.getType());

            generateGetter(javaClass, object, simpleType);
            generateSetter(javaClass, object, simpleType);
        }

        generateToJson(javaClass);
        generateReadJson(javaClass);

        return javaClass;
    }

    private void generateGetter(JavaClassSource javaClass,
            ComponentObjectTypeInnerType object,
            ComponentBasicType simpleType) {

        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnType(ComponentGeneratorUtils.toJavaType(simpleType))
                .setName(ComponentGeneratorUtils.generateMethodNameForProperty(
                        simpleType == ComponentBasicType.BOOLEAN ? "is" : "get",
                        object.getName()));

        String body;
        switch (simpleType) {
        case STRING:
            body = "return internalObject.getString(\"%s\");";
            break;
        case BOOLEAN:
            body = "return internalObject.getBoolean(\"%s\");";
            break;
        case NUMBER:
            body = "return internalObject.getNumber(\"%s\");";
            break;
        case OBJECT:
            body = "return internalObject.getObject(\"%s\");";
            break;
        case ARRAY:
            body = "return internalObject.getArray(\"%s\");";
            break;
        case UNDEFINED:
            body = "return internalObject.get(\"%s\");";
            break;
        case DATE:
            method.addAnnotation(NotSupported.class);
            method.setBody("return null;");
            return;
        default:
            throw new ComponentGenerationException(
                    "Unrecognized type: " + simpleType);
        }

        method.setBody(String.format(body, object.getName()));
    }

    private void generateSetter(JavaClassSource javaClass,
            ComponentObjectTypeInnerType object,
            ComponentBasicType simpleType) {

        Class<?> javaType = ComponentGeneratorUtils.toJavaType(simpleType);
        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnTypeVoid()
                .setName(ComponentGeneratorUtils.generateMethodNameForProperty(
                        "set", object.getName()));

        String formattedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(object.getName());
        ComponentGeneratorUtils.addMethodParameter(javaClass, method, javaType,
                formattedName);

        method.setBody(String.format("this.internalObject.put(\"%s\", %s);",
                object.getName(), formattedName));

        if (fluentSetters) {
            method.setReturnType(javaClass);
            method.setBody(method.getBody() + "return this;");
        }
    }

    private void generateToJson(JavaClassSource javaClass) {
        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnType(JsonObject.class).setName("toJson");

        method.addAnnotation(Override.class);
        method.setBody("return internalObject;");
    }

    private void generateReadJson(JavaClassSource javaClass) {
        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnType(javaClass).setName("readJson");

        method.addAnnotation(Override.class);
        ComponentGeneratorUtils.addMethodParameter(javaClass, method,
                JsonObject.class, "value");
        method.setBody("internalObject = value; return this;");
    }

    // multiple types are not supported. When multiple types are encountered,
    // the value is either object (if any of the types is object) or undefined
    private ComponentBasicType getSimpleBasicType(
            List<ComponentBasicType> types) {

        return types.stream().distinct().reduce((type1, type2) -> {
            if (type1 == ComponentBasicType.OBJECT
                    || type2 == ComponentBasicType.OBJECT) {
                return ComponentBasicType.OBJECT;
            }
            return ComponentBasicType.UNDEFINED;
        }).orElse(ComponentBasicType.UNDEFINED);
    }

}
