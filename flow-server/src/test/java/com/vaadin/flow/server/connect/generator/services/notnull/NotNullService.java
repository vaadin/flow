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
package com.vaadin.flow.server.connect.generator.services.notnull;

import javax.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.server.connect.VaadinService;

@VaadinService
public class NotNullService {
    @NotNull
    public String getNonNullString(@NotNull String input) {
        return "";
    }

    public NonNullModel echoNonNullMode(@NotNull NonNullModel[] nonNullModels) {
        return new NonNullModel();
    }

    public Map<String, NonNullModel> echoMap(boolean shouldBeNotNull) {
        return Collections.emptyMap();
    }

    @NotNull
    public NotNullService.ReturnType getNotNullReturnType() {
        return new ReturnType();
    }

    public void sendParameterType(
            @NotNull NotNullService.ParameterType parameterType) {

    }

    public static class NonNullModel {
        String foo;
        @NotNull
        String bar;
        int shouldBeNotNullByDefault;
        Optional<Integer> nullableInteger;
        List<Map<String, String>> listOfMapNullable;
        @NotNull
        List<Map<String, String>> listOfMapNullableNotNull;
    }

    public static class ReturnType {
        String foo;
    }

    public static class ParameterType {
        String foo;
    }
}
