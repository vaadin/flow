package com.vaadin.generator;

import java.util.List;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import com.vaadin.components.JsonSerializable;
import com.vaadin.components.NotSupported;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentObjectType;

import elemental.json.JsonObject;

public class NestedClassGenerator {

    private List<ComponentObjectType> type;
    private String nameHint;
    private boolean fluentSetters = true;

    public NestedClassGenerator withType(List<ComponentObjectType> type) {
        this.type = type;
        return this;
    }

    public NestedClassGenerator withNameHint(String nameHint) {
        this.nameHint = nameHint;
        return this;
    }

    public NestedClassGenerator withFluentSetters(boolean fluentSetters) {
        this.fluentSetters = fluentSetters;
        return this;
    }

    public JavaClassSource build() {
        JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.addInterface(JsonSerializable.class).setPublic()
                .setStatic(true).setName(ComponentGeneratorUtils
                        .generateValidJavaClassName(nameHint));

        javaClass.addField().setType(JsonObject.class).setPrivate()
                .setName("internalObject");

        for (ComponentObjectType object : type) {
            ComponentBasicType simpleType = getSimpleBasicType(
                    object.getType());

            generateGetter(javaClass, object, simpleType);
            generateSetter(javaClass, object, simpleType);
        }

        generateToJson(javaClass);
        generateFromJson(javaClass);

        return javaClass;
    }

    private void generateGetter(JavaClassSource javaClass,
            ComponentObjectType object, ComponentBasicType simpleType) {

        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnType(ComponentGeneratorUtils.toJavaType(simpleType))
                .setName(ComponentGeneratorUtils.generateMethodNameForProperty(
                        simpleType == ComponentBasicType.BOOLEAN ? "is" : "get",
                        object.getName()));

        switch (simpleType) {
        case STRING:
            method.setBody(
                    String.format("return internalObject.getString(\"%s\");",
                            object.getName()));
            break;
        case BOOLEAN:
            method.setBody(
                    String.format("return internalObject.getBoolean(\"%s\");",
                            object.getName()));
            break;
        case NUMBER:
            method.setBody(
                    String.format("return internalObject.getNumber(\"%s\");",
                            object.getName()));
            break;
        case OBJECT:
            method.setBody(
                    String.format("return internalObject.getObject(\"%s\");",
                            object.getName()));
            break;
        case ARRAY:
            method.setBody(
                    String.format("return internalObject.getArray(\"%s\");",
                            object.getName()));
            break;
        case UNDEFINED:
            method.setBody(String.format("return internalObject.get(\"%s\");",
                    object.getName()));
            break;
        case DATE:
            method.addAnnotation(NotSupported.class);
            method.setBody("return null;");
            break;
        }
    }

    private void generateSetter(JavaClassSource javaClass,
            ComponentObjectType object, ComponentBasicType simpleType) {

        Class<?> javaType = ComponentGeneratorUtils.toJavaType(simpleType);
        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnTypeVoid()
                .setName(ComponentGeneratorUtils.generateMethodNameForProperty(
                        "set", object.getName()));

        String formattedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(object.getName());
        method.addParameter(javaType, formattedName);

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

    private void generateFromJson(JavaClassSource javaClass) {
        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnType(javaClass).setName("fromJson");

        method.addAnnotation(Override.class);
        method.addParameter(JsonObject.class, "value");
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
        }).get();
    }

}
