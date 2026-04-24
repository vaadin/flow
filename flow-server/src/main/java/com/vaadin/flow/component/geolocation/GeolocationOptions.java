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

import java.io.Serializable;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Tuning knobs for a geolocation request — controls the accuracy / battery /
 * speed / freshness trade-off of a single {@link Geolocation#get} or
 * {@link Geolocation#track} call.
 * <p>
 * Every field is optional. A {@code null} field means "let the browser decide":
 * high accuracy defaults to {@code false}, timeout defaults to no timeout at
 * all, and cached readings are never accepted unless explicitly allowed.
 * <p>
 * Hand-written code should use {@link #builder()} rather than the canonical
 * constructor: the builder labels each setting at the call site and accepts
 * {@link Duration} instead of raw milliseconds.
 *
 * <pre>
 * // High accuracy, give up after 10 seconds, refuse cached readings:
 * GeolocationOptions opts = GeolocationOptions.builder().highAccuracy(true)
 *         .timeout(Duration.ofSeconds(10)).maximumAge(Duration.ZERO).build();
 *
 * // Low-battery mode: accept a cached reading up to five minutes old:
 * GeolocationOptions cached = GeolocationOptions.builder()
 *         .maximumAge(Duration.ofMinutes(5)).build();
 * </pre>
 *
 * The canonical record constructor accepts raw millisecond values and is mainly
 * useful when building an instance from deserialised data.
 *
 * @param enableHighAccuracy
 *            when {@code true}, asks the browser for the most accurate reading
 *            it can produce (typically GPS on mobile devices). This may use
 *            more battery and take longer. When {@code null}, the browser
 *            default ({@code false}) applies and a coarse network-based reading
 *            is returned
 * @param timeout
 *            the maximum time in milliseconds the browser is allowed to spend
 *            before reporting a {@link GeolocationErrorCode#TIMEOUT} error.
 *            When {@code null}, there is no timeout — the request will wait
 *            indefinitely for a positioning answer
 * @param maximumAge
 *            the maximum age in milliseconds of a cached reading that the
 *            browser is allowed to return without querying the positioning
 *            hardware again. {@code 0} means "never use a cached reading";
 *            {@code null} also means {@code 0}. Larger values save battery and
 *            return faster at the cost of freshness
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeolocationOptions(@Nullable Boolean enableHighAccuracy,
        @Nullable Integer timeout,
        @Nullable Integer maximumAge) implements Serializable {

    /**
     * Canonical constructor. Rejects negative {@code timeout} and
     * {@code maximumAge} values — both must be non-negative or {@code null}.
     *
     * @param enableHighAccuracy
     *            see the record component
     * @param timeout
     *            see the record component
     * @param maximumAge
     *            see the record component
     * @throws IllegalArgumentException
     *             if {@code timeout} or {@code maximumAge} is negative
     */
    public GeolocationOptions(@Nullable Boolean enableHighAccuracy,
            @Nullable Integer timeout, @Nullable Integer maximumAge) {
        if (timeout != null && timeout < 0) {
            throw new IllegalArgumentException(
                    "timeout must be non-negative, was " + timeout);
        }
        if (maximumAge != null && maximumAge < 0) {
            throw new IllegalArgumentException(
                    "maximumAge must be non-negative, was " + maximumAge);
        }
        this.enableHighAccuracy = enableHighAccuracy;
        this.timeout = timeout;
        this.maximumAge = maximumAge;
    }

    /**
     * Starts building a {@link GeolocationOptions} instance. Only the settings
     * the caller explicitly sets are included; everything else falls back to
     * the browser default.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GeolocationOptions}. Accepts {@link Duration} for the
     * time-based fields and {@code boolean} for the high-accuracy flag so that
     * call sites read naturally. Any setter that is not called leaves the
     * corresponding field {@code null}, which means "use the browser default"
     * when the request runs.
     */
    public static final class Builder implements Serializable {
        private @Nullable Boolean enableHighAccuracy;
        private @Nullable Integer timeout;
        private @Nullable Integer maximumAge;

        private Builder() {
        }

        /**
         * Requests the most accurate reading the device can produce (typically
         * GPS on mobile, coarse network lookup on desktop). High accuracy
         * usually costs more battery and takes longer.
         *
         * @param highAccuracy
         *            {@code true} to ask for the best available accuracy
         * @return this builder
         */
        public Builder highAccuracy(boolean highAccuracy) {
            this.enableHighAccuracy = highAccuracy;
            return this;
        }

        /**
         * Sets the maximum time to wait for a position before the request fails
         * with {@link GeolocationErrorCode#TIMEOUT}. If {@code null} is passed
         * (or this setter is never called), the browser waits indefinitely.
         *
         * @param timeout
         *            the timeout, or {@code null} to wait indefinitely
         * @return this builder
         */
        public Builder timeout(@Nullable Duration timeout) {
            this.timeout = timeout == null ? null
                    : Math.toIntExact(timeout.toMillis());
            return this;
        }

        /**
         * Sets the maximum time in milliseconds to wait for a position before
         * the request fails with {@link GeolocationErrorCode#TIMEOUT}.
         * Convenience overload for callers that already have a millisecond
         * value on hand.
         *
         * @param timeoutMillis
         *            the timeout in milliseconds; must be non-negative
         * @return this builder
         * @throws IllegalArgumentException
         *             if {@code timeoutMillis} is negative
         */
        public Builder timeout(int timeoutMillis) {
            if (timeoutMillis < 0) {
                throw new IllegalArgumentException(
                        "timeout must be non-negative, was " + timeoutMillis);
            }
            this.timeout = timeoutMillis;
            return this;
        }

        /**
         * Sets how old a cached reading may be while still being acceptable as
         * an answer to this request. {@link Duration#ZERO} (or {@code null}, or
         * never calling this setter) means the browser must query the
         * positioning hardware on every request. Larger values save battery and
         * return faster.
         *
         * @param maximumAge
         *            the maximum acceptable age, or {@code null} to refuse
         *            cached readings
         * @return this builder
         */
        public Builder maximumAge(@Nullable Duration maximumAge) {
            this.maximumAge = maximumAge == null ? null
                    : Math.toIntExact(maximumAge.toMillis());
            return this;
        }

        /**
         * Sets how old a cached reading may be, in milliseconds. Convenience
         * overload for callers that already have a millisecond value on hand.
         * {@code 0} means the browser must query the positioning hardware on
         * every request.
         *
         * @param maximumAgeMillis
         *            the maximum acceptable age in milliseconds; must be
         *            non-negative
         * @return this builder
         * @throws IllegalArgumentException
         *             if {@code maximumAgeMillis} is negative
         */
        public Builder maximumAge(int maximumAgeMillis) {
            if (maximumAgeMillis < 0) {
                throw new IllegalArgumentException(
                        "maximumAge must be non-negative, was "
                                + maximumAgeMillis);
            }
            this.maximumAge = maximumAgeMillis;
            return this;
        }

        /**
         * Creates a new {@link GeolocationOptions} from this builder's current
         * state.
         *
         * @return a new options instance
         */
        public GeolocationOptions build() {
            return new GeolocationOptions(enableHighAccuracy, timeout,
                    maximumAge);
        }
    }
}
