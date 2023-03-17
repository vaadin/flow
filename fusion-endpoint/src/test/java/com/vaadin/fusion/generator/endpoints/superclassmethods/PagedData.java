/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.superclassmethods;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vaadin.fusion.EndpointExposed;

import org.checkerframework.checker.nullness.qual.NonNull;

@EndpointExposed
public interface PagedData<T> {
    default int size() {
        return 0;
    }

    default List<T> getPage(int pageSize, int pageNumber) {
        return Collections.emptyList();
    }

    default List<@NonNull T> getNonNullablePage(int pageSize, int pageNumber,
            Map<String, @NonNull T> parameters) {
        return Collections.emptyList();
    }
}
