/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NonnullEntity {
    @Nonnull
    private final List<@Nonnull String> nonNullableField = new ArrayList<>();

    @Nonnull
    public String nonNullableMethod(
            @Nonnull Map<String, @Nonnull String> nonNullableParameter) {
        return nonNullableParameter.getOrDefault("test", "");
    }
}
