/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.server.connect.generator;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.vaadin.flow.internal.ReflectTools;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Methods related with creating schema declarations, used by
 * OpenApiObjectGenerator.
 */
class SchemaGenerator {
    private final OpenApiObjectGenerator openApiObjectGenerator;

    SchemaGenerator(OpenApiObjectGenerator openApiObjectGenerator) {
        this.openApiObjectGenerator = openApiObjectGenerator;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SchemaGenerator.class);
    }

    Schema createSingleSchema(String fullQualifiedName,
            TypeDeclaration<?> typeDeclaration) {
        Optional<String> description = typeDeclaration.getJavadoc()
                .map(javadoc -> javadoc.getDescription().toText());
        Schema schema = new ObjectSchema();
        schema.setName(fullQualifiedName);
        description.ifPresent(schema::setDescription);
        Map<String, Schema> properties = getPropertiesFromClassDeclaration(
                typeDeclaration);
        schema.properties(properties);
        List<String> requiredList = properties.entrySet().stream()
                .filter(stringSchemaEntry -> GeneratorUtils
                        .isNotTrue(stringSchemaEntry.getValue().getNullable()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        // Nullable is represented in requiredList instead.
        properties.values()
                .forEach(propertySchema -> propertySchema.nullable(null));
        schema.setRequired(requiredList);
        return schema;
    }

    Schema parseTypeToSchema(Type javaType, String description) {
        try {
            Schema schema = openApiObjectGenerator
                    .parseResolvedTypeToSchema(javaType.resolve());
            if (GeneratorUtils.isNotBlank(description)) {
                schema.setDescription(description);
            }
            return schema;
        } catch (Exception e) {
            getLogger().info(String.format(
                    "Can't resolve type '%s' for creating custom OpenAPI Schema. Using the default ObjectSchema instead.",
                    javaType.asString()), e);
        }
        return new ObjectSchema();
    }

    Schema createSingleSchemaFromResolvedType(
            ResolvedReferenceType resolvedType) {
        if (resolvedType.getTypeDeclaration().isEnum()) {
            List<String> entries = resolvedType.getTypeDeclaration().asEnum()
                    .getEnumConstants().stream()
                    .map(ResolvedEnumConstantDeclaration::getName)
                    .collect(Collectors.toList());
            StringSchema schema = new StringSchema();
            schema.name(resolvedType.getQualifiedName());
            schema.setEnum(entries);
            return schema;
        }
        Schema schema = new ObjectSchema()
                .name(resolvedType.getQualifiedName());
        Map<String, Boolean> fieldsOptionalMap = getFieldsAndOptionalMap(
                resolvedType);
        Set<ResolvedFieldDeclaration> serializableFields = resolvedType
                .getDeclaredFields().stream()
                .filter(resolvedFieldDeclaration -> fieldsOptionalMap
                        .containsKey(resolvedFieldDeclaration.getName()))
                .collect(Collectors.toSet());
        // Make sure the order is consistent in properties map
        schema.setProperties(new TreeMap<>());
        for (ResolvedFieldDeclaration resolvedFieldDeclaration : serializableFields) {
            String name = resolvedFieldDeclaration.getName();
            Schema type = openApiObjectGenerator.parseResolvedTypeToSchema(
                    resolvedFieldDeclaration.getType());
            if (!fieldsOptionalMap.get(name)) {
                schema.addRequiredItem(name);
            }
            schema.addProperties(name, type);
        }
        return schema;
    }

    /**
     * Because it's not possible to check the `transient` modifier and
     * annotation of a field using JavaParser API. We need this method to
     * reflect the type and get those information from the reflected object.
     *
     * @param resolvedType
     *            type of the class to get fields information
     * @return set of fields' name that we should generate.
     */
    private Map<String, Boolean> getFieldsAndOptionalMap(
            ResolvedReferenceType resolvedType) {
        if (!resolvedType.getTypeDeclaration().isClass()
                || resolvedType.getTypeDeclaration().isAnonymousClass()) {
            return Collections.emptyMap();
        }
        HashMap<String, Boolean> validFields = new HashMap<>();
        try {
            Class<?> aClass = openApiObjectGenerator
                    .getClassFromReflection(resolvedType);
            Arrays.stream(aClass.getDeclaredFields()).filter(field -> {

                int modifiers = field.getModifiers();
                return !Modifier.isStatic(modifiers)
                        && !Modifier.isTransient(modifiers)
                        && !field.isAnnotationPresent(JsonIgnore.class);
            }).forEach(field -> validFields.put(field.getName(),
                    field.isAnnotationPresent(Nullable.class)
                            || ReflectTools.hasAnnotationWithSimpleName(field, "Id")
                            || field.getType().equals(Optional.class)));
        } catch (ClassNotFoundException e) {

            String message = String.format(
                    "Can't get list of fields from class '%s'."
                            + "Please make sure that class '%s' is in your project's compile classpath. "
                            + "As the result, the generated TypeScript file will be empty.",
                    resolvedType.getQualifiedName(),
                    resolvedType.getQualifiedName());
            getLogger().info(message);
            getLogger().debug(message, e);
        }
        return validFields;
    }

    private Map<String, Schema> getPropertiesFromClassDeclaration(
            TypeDeclaration<?> typeDeclaration) {
        Map<String, Schema> properties = new TreeMap<>();
        for (FieldDeclaration field : typeDeclaration.getFields()) {
            if (field.isTransient() || field.isStatic()
                    || field.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            Optional<String> fieldDescription = field.getJavadoc()
                    .map(javadoc -> javadoc.getDescription().toText());
            field.getVariables().forEach(variableDeclarator -> {
                Schema propertySchema = parseTypeToSchema(
                        variableDeclarator.getType(),
                        fieldDescription.orElse(""));
                if (GeneratorUtils.isNotBlank(propertySchema.get$ref())) {
                    // Schema extensions, e. g., `x-annotations` we use, are
                    // not supported for the Reference Object Schema.
                    // Workaround: wrap in a composed object schema.
                    ComposedSchema wrapperSchema = new ComposedSchema();
                    wrapperSchema.name(propertySchema.getName());
                    wrapperSchema.addAllOfItem(propertySchema);
                    propertySchema = wrapperSchema;
                }
                if (field.isAnnotationPresent(Nullable.class) || field.isAnnotationPresent("Id")
                        || GeneratorUtils.isTrue(propertySchema.getNullable())) {
                    // Temporarily set nullable to indicate this property is
                    // not required
                    propertySchema.setNullable(true);
                }
                addFieldAnnotationsToSchema(field, propertySchema);
                properties.put(variableDeclarator.getNameAsString(),
                        propertySchema);
            });
        }
        return properties;
    }

    private void addFieldAnnotationsToSchema(FieldDeclaration field,
            Schema<?> schema) {
        Set<String> annotations = new LinkedHashSet<>();
        field.getAnnotations().stream().forEach(annotation -> {
            String str = annotation.toString()
                    // remove annotation character
                    .replaceFirst("@", "")
                    // change to json syntax
                    .replace(" = ", ":");
            // wrap arguments with curly if there are json key:value arguments
            if (str.contains(":")) {
                str = str.replaceFirst("\\(", "({").replaceFirst("\\)$", "})");
            }
            // append parenthesis if not already
            str += str.contains("(") ? "" : "()";

            if (str.matches(
                    "(Email|Null|NotNull|NotEmpty|NotBlank|AssertTrue|AssertFalse|Negative|NegativeOrZero|Positive|PositiveOrZero|Size|Past|Future|Digits|Min|Max|Pattern|DecimalMin|DecimalMax)\\(.+")) {
                annotations.add(str);
            }
        });
        if (!annotations.isEmpty()) {
            schema.addExtension(OpenApiObjectGenerator.CONSTRAINT_ANNOTATIONS,
                    annotations.stream()
                            .sorted((a, b) -> isAnnotationIndicatingRequired(a)
                                    ? -1
                                    : isAnnotationIndicatingRequired(b) ? 1
                                            : a.compareTo(b))
                            .collect(Collectors.toList()));
        }
    }

    private boolean isAnnotationIndicatingRequired(String str) {
        return str.matches("(NonNull|NotNull|NotEmpty|NotBlank)\\(.+")
                || str.matches("Size\\(\\{.*min:[^0].+");
    }
}
