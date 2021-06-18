/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

class SchemaResolver {

    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";
    private final Map<String, GeneratorType> usedTypes;

    SchemaResolver(Map<String, GeneratorType> usedTypes) {
        this.usedTypes = usedTypes;
    }

    /**
     * This method is needed because the {@link Schema#set$ref(String)} method
     * won't append "#/components/schemas/" if the ref contains `.`.
     *
     * @param qualifiedName
     *            full qualified name of the class
     * @return the ref in format of "#/components/schemas/com.my.example.Model"
     */
    static String getFullQualifiedNameRef(String qualifiedName) {
        return SCHEMA_REF_PREFIX + qualifiedName;
    }

    static String getSimpleRef(String ref) {
        if (GeneratorUtils.contains(ref, SCHEMA_REF_PREFIX)) {
            return GeneratorUtils.substringAfter(ref, SCHEMA_REF_PREFIX);
        }
        return ref;
    }

    Schema parseResolvedTypeToSchema(GeneratorType type) {
        if (type.isArray()) {
            return createNullableWrapper(createArraySchema(type));
        }

        if (type.isNumber()) {
            return createNullableWrapper(new NumberSchema(),
                    !type.isPrimitive());
        }

        if (type.isString()) {
            return createNullableWrapper(new StringSchema());
        }

        if (type.isCollection()) {
            return createNullableWrapper(createCollectionSchema(type));
        }

        if (type.isBoolean()) {
            return createNullableWrapper(new BooleanSchema(),
                    !type.isPrimitive());
        }

        if (type.isMap()) {
            return createNullableWrapper(createMapSchema(type));
        }

        if (type.isDate()) {
            return createNullableWrapper(new DateSchema());
        }

        if (type.isDateTime()) {
            return createNullableWrapper(new DateTimeSchema());
        }

        if (type.isOptional()) {
            return createOptionalSchema(type);
        }

        if (type.isUnhandled()) {
            return createNullableWrapper(new ObjectSchema());
        }

        if (type.isEnum()) {
            return createNullableWrapper(createEnumTypeSchema(type));
        }

        return createNullableWrapper(createUserBeanSchema(type));
    }

    private Schema createArraySchema(GeneratorType type) {
        ArraySchema array = new ArraySchema();
        array.items(parseResolvedTypeToSchema(type.getItemType()));
        return array;
    }

    private Schema createCollectionSchema(GeneratorType type) {
        ArraySchema array = new ArraySchema();
        List<GeneratorType> typeArguments = type.getTypeArguments();

        if (!typeArguments.isEmpty()) {
            array.items(parseResolvedTypeToSchema(typeArguments.get(0)));
        }

        return array;
    }

    private Schema createOptionalSchema(GeneratorType type) {
        Schema nestedTypeSchema = parseResolvedTypeToSchema(
                type.getTypeArguments().get(0));
        return createNullableWrapper(nestedTypeSchema);
    }

    private Schema createNullableWrapper(Schema nestedTypeSchema) {
        return createNullableWrapper(nestedTypeSchema, true);
    }

    private Schema createNullableWrapper(Schema nestedTypeSchema,
            boolean shouldBeNullable) {
        if (!shouldBeNullable) {
            return nestedTypeSchema;
        }

        if (nestedTypeSchema.get$ref() == null) {
            nestedTypeSchema.setNullable(true);
            return nestedTypeSchema;
        }

        ComposedSchema nullableSchema = new ComposedSchema();
        nullableSchema.setNullable(true);
        nullableSchema.setAllOf(Collections.singletonList(nestedTypeSchema));
        return nullableSchema;
    }

    private Schema createMapSchema(GeneratorType type) {
        Schema mapSchema = new MapSchema();
        List<GeneratorType> typeArguments = type.getTypeArguments();

        if (typeArguments.size() == 2) {
            // Assumed that Map always has the first type parameter as `String`
            // and the second is for its value type
            mapSchema.additionalProperties(
                    parseResolvedTypeToSchema(typeArguments.get(1)));
        }
        return mapSchema;
    }

    private Schema createEnumTypeSchema(GeneratorType type) {
        ResolvedReferenceType resolvedReferenceType = type.asResolvedType()
                .asReferenceType();
        List<String> entries = resolvedReferenceType.getTypeDeclaration()
                .asEnum().getEnumConstants().stream()
                .map(ResolvedEnumConstantDeclaration::getName)
                .collect(Collectors.toList());
        String qualifiedName = resolvedReferenceType.getQualifiedName();
        usedTypes.put(qualifiedName, type);
        StringSchema schema = new StringSchema();
        schema.name(qualifiedName);
        schema.setEnum(entries);
        schema.$ref(getFullQualifiedNameRef(qualifiedName));
        return schema;
    }

    private Schema createUserBeanSchema(GeneratorType type) {
        if (type.isReference()) {
            ResolvedReferenceType resolvedReferenceType = type.asResolvedType()
                    .asReferenceType();
            String qualifiedName = resolvedReferenceType.getQualifiedName();
            usedTypes.put(qualifiedName, type);
            return new ObjectSchema().name(qualifiedName)
                    .$ref(getFullQualifiedNameRef(qualifiedName));
        }
        return new ObjectSchema();
    }
}
