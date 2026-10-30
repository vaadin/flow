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
package com.vaadin.flow.component.webnotification;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * What a notification shows besides its title, and how it behaves. Every
 * setting is optional; a setting that is not set uses the browser's default.
 * Create instances with {@link #builder()}:
 *
 * <pre>{@code
 * WebNotificationOptions options = WebNotificationOptions.builder()
 *         .body("Report for March is ready").tag("report").build();
 * }</pre>
 *
 * How a setting is presented is up to the browser and the operating system, and
 * some ignore settings they do not support — for example, Firefox and Safari
 * ignore {@code requireInteraction}.
 *
 * @param body
 *            the text shown below the title, or {@code null} for none
 * @param icon
 *            the URL of an image shown with the notification, or {@code null}
 *            for the browser's default
 * @param tag
 *            the group the notification belongs to, or {@code null} for none;
 *            showing a notification replaces a notification with the same tag
 *            that is still visible instead of adding another one
 * @param renotify
 *            whether replacing a notification with the same tag alerts the user
 *            again with sound or vibration, or {@code null} for the browser's
 *            default (no alert); {@code true} requires a {@code tag} and cannot
 *            be combined with {@code silent}
 * @param requireInteraction
 *            whether the notification stays visible until the user clicks or
 *            dismisses it, or {@code null} for the browser's default
 * @param silent
 *            whether the notification is shown without sound or vibration, or
 *            {@code null} for the browser's default
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebNotificationOptions(@Nullable String body,
        @Nullable String icon, @Nullable String tag, @Nullable Boolean renotify,
        @Nullable Boolean requireInteraction,
        @Nullable Boolean silent) implements Serializable {

    /**
     * Validates the combination of settings.
     *
     * @throws IllegalArgumentException
     *             if {@code renotify} is {@code true} without a {@code tag} or
     *             together with {@code silent}, which browsers reject
     */
    public WebNotificationOptions {
        if (Boolean.TRUE.equals(renotify) && (tag == null || tag.isEmpty())) {
            throw new IllegalArgumentException("renotify requires a tag");
        }
        if (Boolean.TRUE.equals(renotify) && Boolean.TRUE.equals(silent)) {
            throw new IllegalArgumentException(
                    "renotify cannot be combined with silent");
        }
    }

    /**
     * Creates a builder with no settings set.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds {@link WebNotificationOptions}. Every setter returns the builder
     * for chaining.
     */
    public static final class Builder implements Serializable {
        private @Nullable String body;
        private @Nullable String icon;
        private @Nullable String tag;
        private @Nullable Boolean renotify;
        private @Nullable Boolean requireInteraction;
        private @Nullable Boolean silent;

        private Builder() {
        }

        /**
         * Sets the text shown below the title.
         *
         * @param body
         *            the text, or {@code null} for none
         * @return this builder
         */
        public Builder body(@Nullable String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the URL of an image shown with the notification. A relative URL
         * is resolved against the page.
         *
         * @param icon
         *            the image URL, or {@code null} for the browser's default
         * @return this builder
         */
        public Builder icon(@Nullable String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Sets the group the notification belongs to. Showing a notification
         * replaces a notification with the same tag that is still visible, so
         * repeated updates about the same thing do not pile up.
         *
         * @param tag
         *            the tag, or {@code null} for none
         * @return this builder
         */
        public Builder tag(@Nullable String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Sets whether replacing a notification with the same tag alerts the
         * user again with sound or vibration. Without it the replacement is
         * silent. Requires a {@link #tag(String) tag}, and cannot be combined
         * with {@link #silent(boolean) silent}.
         *
         * @param renotify
         *            {@code true} to alert the user again
         * @return this builder
         */
        public Builder renotify(boolean renotify) {
            this.renotify = renotify;
            return this;
        }

        /**
         * Sets whether the notification stays visible until the user clicks or
         * dismisses it, instead of closing automatically after a while.
         *
         * @param requireInteraction
         *            {@code true} to keep the notification visible
         * @return this builder
         */
        public Builder requireInteraction(boolean requireInteraction) {
            this.requireInteraction = requireInteraction;
            return this;
        }

        /**
         * Sets whether the notification is shown without sound or vibration.
         *
         * @param silent
         *            {@code true} to show the notification silently
         * @return this builder
         */
        public Builder silent(boolean silent) {
            this.silent = silent;
            return this;
        }

        /**
         * Creates the options from the settings of this builder.
         *
         * @return the options
         * @throws IllegalArgumentException
         *             if {@code renotify} is set without a {@code tag} or
         *             together with {@code silent}
         */
        public WebNotificationOptions build() {
            return new WebNotificationOptions(body, icon, tag, renotify,
                    requireInteraction, silent);
        }
    }
}
