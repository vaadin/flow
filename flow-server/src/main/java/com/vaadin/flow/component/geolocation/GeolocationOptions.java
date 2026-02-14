/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.geolocation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Options for the browser's Geolocation API.
 * <p>
 * All fields are optional. When {@code null}, the browser's default values are
 * used.
 *
 * @param enableHighAccuracy
 *            whether to request high-accuracy position data (e.g. GPS), or
 *            {@code null} for the browser default ({@code false})
 * @param timeout
 *            the maximum time in milliseconds to wait for a position, or
 *            {@code null} for the browser default ({@code Infinity})
 * @param maximumAge
 *            the maximum age in milliseconds of a cached position that is
 *            acceptable, or {@code null} for the browser default ({@code 0})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeolocationOptions(Boolean enableHighAccuracy, Integer timeout,
        Integer maximumAge) {

    /**
     * Creates options with all values set to {@code null}, meaning the
     * browser's defaults will be used.
     */
    public GeolocationOptions() {
        this(null, null, null);
    }
}
