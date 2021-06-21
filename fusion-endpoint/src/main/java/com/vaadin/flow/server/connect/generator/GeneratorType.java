package com.vaadin.flow.server.connect.generator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;

import com.vaadin.flow.server.connect.ExplicitNullableTypeChecker;

class GeneratorType {
    private final Type type;
    private final ResolvedType resolvedType;
    private final boolean isResolvable;

    GeneratorType(Type type) {
        this.type = type;
        resolvedType = type.resolve();
        isResolvable = true;
    }

    GeneratorType(ResolvedType resolvedType) {
        this.type = null;
        this.resolvedType = resolvedType;
        isResolvable = true;
    }

    /**
     * Applicable for template types (like T or U).
     *
     * @param type
     * @param resolvedType
     * @param resolvedTypeParametersMap
     */
    GeneratorType(Type type, ResolvedType resolvedType,
            ResolvedTypeParametersMap resolvedTypeParametersMap) {
        this.type = type;
        this.resolvedType = resolvedTypeParametersMap.replaceAll(resolvedType);
        this.isResolvable = false;
    }

    boolean hasType() {
        return type != null;
    }

    boolean isArray() {
        return resolvedType.isArray();
    }

    boolean isBoolean() {
        if (resolvedType.isPrimitive()) {
            return resolvedType.asPrimitive() == ResolvedPrimitiveType.BOOLEAN;
        }

        return isType(Boolean.class);
    }

    boolean isCollection() {
        return !resolvedType.isPrimitive()
                && (isType(Collection.class) || isType(Iterable.class));
    }

    boolean isDate() {
        return resolvedType.isReferenceType()
                && isType(Date.class, LocalDate.class);
    }

    boolean isDateTime() {
        return resolvedType.isReferenceType()
                && isType(LocalDateTime.class, Instant.class, LocalTime.class);
    }

    boolean isEnum() {
        return isType(Enum.class);
    }

    boolean isMap() {
        return !resolvedType.isPrimitive() && isType(Map.class);
    }

    boolean isNumber() {
        if (resolvedType.isPrimitive()) {
            ResolvedPrimitiveType resolvedPrimitiveType = resolvedType
                    .asPrimitive();
            return resolvedPrimitiveType != ResolvedPrimitiveType.BOOLEAN
                    && resolvedPrimitiveType != ResolvedPrimitiveType.CHAR;
        } else {
            return isType(Number.class);
        }
    }

    boolean isOptional() {
        return resolvedType.isReferenceType() && isType(Optional.class);
    }

    boolean isPrimitive() {
        return resolvedType.isPrimitive();
    }

    boolean isReference() {
        return resolvedType.isReferenceType();
    }

    boolean isRequired() {
        return isPrimitive() || hasType() && isResolvable
                && ExplicitNullableTypeChecker.isRequired(type.getAnnotations());
    }

    boolean isString() {
        if (resolvedType.isPrimitive()) {
            return resolvedType.asPrimitive() == ResolvedPrimitiveType.CHAR;
        }

        return isType(String.class, Character.class);
    }

    boolean isUnhandled() {
        return resolvedType.isReferenceType() && resolvedType.asReferenceType()
                .getQualifiedName().startsWith("java.");
    }

    /**
     * Checks if the given type can be cast to one of the given classes.
     *
     * @param clazz
     *            the classes to match with
     * @return true if the type can be cast to one of the given classes, false
     *         otherwise
     */
    boolean isType(Class<?>... clazz) {
        if (!resolvedType.isReferenceType()) {
            return false;
        }

        List<String> classes = Arrays.stream(clazz).map(Class::getName)
                .collect(Collectors.toList());

        ResolvedReferenceType resolvedReferenceType = resolvedType
                .asReferenceType();

        return classes.contains(resolvedReferenceType.getQualifiedName())
                || resolvedReferenceType.getAllAncestors().stream()
                        .map(ResolvedReferenceType::getQualifiedName)
                        .anyMatch(classes::contains);
    }

    ResolvedType asResolvedType() {
        return resolvedType;
    }

    Optional<Type> asType() {
        return Optional.ofNullable(type);
    }

    GeneratorType getItemType() {
        return hasType() && isResolvable
                ? new GeneratorType(type.asArrayType().getComponentType())
                : new GeneratorType(
                        resolvedType.asArrayType().getComponentType());
    }

    List<GeneratorType> getTypeArguments() {
        return hasType() && isResolvable
                ? type.asClassOrInterfaceType().getTypeArguments()
                        .map(nodeList -> nodeList.stream()
                                .map(GeneratorType::new)
                                .collect(Collectors.toList()))
                        .orElseGet(this::getTypeArgumentsFallback)
                : getTypeArgumentsFallback();
    }

    private List<GeneratorType> getTypeArgumentsFallback() {
        return resolvedType.asReferenceType().getTypeParametersMap().stream()
                .map(parameter -> new GeneratorType(parameter.b))
                .collect(Collectors.toList());
    }
}