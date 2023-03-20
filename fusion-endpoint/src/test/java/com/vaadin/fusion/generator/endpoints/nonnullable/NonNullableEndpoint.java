/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.nonnullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.vaadin.fusion.Endpoint;

@Endpoint
public class NonNullableEndpoint {

    public int getNonNullableIndex() {
        return 0;
    }

    @Nonnull
    public String getNonNullableString(String input) {
        return "";
    }

    public NonNullableModel echoNonNullModel(
            @Nonnull NonNullableModel[] nonNullableModels) {
        return new NonNullableModel();
    }

    @Nonnull
    public Map<String, NonNullableModel> echoMap(boolean shouldBeNotNull) {
        return Collections.emptyMap();
    }

    @Nonnull
    public Map<String, @NonNull NonNullableModel> echoNonNullableMap(
            @Nonnull List<@NonNull String> nonNullableList) {
        return Collections.emptyMap();
    }

    @com.vaadin.fusion.Nonnull
    public Map<String, @com.vaadin.fusion.Nonnull VaadinNonNullableModel> echoVaadinNonNullableMap(
            @com.vaadin.fusion.Nonnull List<@com.vaadin.fusion.Nonnull String> nonNullableParameter) {
        return Collections.emptyMap();
    }

    public NonNullableEndpoint.ReturnType getNotNullReturnType() {
        return new ReturnType();
    }

    public void sendParameterType(
            NonNullableEndpoint.ParameterType parameterType) {
    }

    public String stringNullable() {
        return "";
    }

    public static class NonNullableModel {
        int[] integers;
        List<Integer> integersList;
        @Nonnull
        String foo;
        int shouldBeNotNullByDefault;
        int first, second, third;
        Optional<Integer> nullableInteger;
        List<@NonNull Map<String, @NonNull String>> listOfMapNullable;
        List<Map<String, String>> listOfMapNullableNotNull;
    }

    public static class VaadinNonNullableModel {
        @com.vaadin.fusion.Nonnull
        String foo;
        @com.vaadin.fusion.Nonnull
        List<@com.vaadin.fusion.Nonnull Integer> nonNullableList;
        @com.vaadin.fusion.Nonnull
        Map<String, @com.vaadin.fusion.Nonnull String> nonNullableMap;
    }

    public static class ReturnType {
        @NonNull
        String foo;
    }

    public static class ParameterType {
        @Nonnull
        String foo;
    }
}
