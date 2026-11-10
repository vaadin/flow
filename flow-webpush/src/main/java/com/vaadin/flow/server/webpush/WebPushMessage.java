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
package com.vaadin.flow.server.webpush;

import java.io.Serializable;
import java.util.Objects;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;

/**
 * Web Push message object containing an information to be shown in the
 * notification.
 * <p>
 * Tapping the notification opens the application, or focuses it when it is
 * already open. To open a specific view instead, set {@code data.url} in the
 * options to its URL relative to the application's root, such as
 * {@code "orders/42"}. An open application window is then navigated to it.
 *
 * @param title
 *            the notification title
 * @param options
 *            the notification options as an ObjectNode
 * @since 24.2
 */
public record WebPushMessage(String title,
        ObjectNode options) implements Serializable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new Web Push notification message with the specified title and
     * various options fetched from a given Java object.
     *
     * @param title
     *            the notification title
     * @param options
     *            any {@code Serializable} Java object representing custom
     *            settings to apply to the notification
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerRegistration/showNotification#parameters">
     *      showNotification parameters</a>
     * @since 24.6
     */
    public WebPushMessage(String title, Serializable options) {
        this(title, objectMapper.convertValue(options, ObjectNode.class));
    }

    /**
     * Creates a new Web Push notification message with just a title and body.
     *
     * @param title
     *            notification title
     * @param body
     *            notification body
     * @since 24.6
     */
    public WebPushMessage(String title, String body) {
        this(title, getBodyOption(body));
    }

    /**
     * Creates a new Web Push notification message with just a title.
     *
     * @param title
     *            notification title
     * @since 24.6
     */
    public WebPushMessage(String title) {
        this(title, (ObjectNode) null);
    }

    /**
     * Starts building a Web Push message with the given title, for setting the
     * common notification options such as the body, the tag or the view that
     * opens when the notification is tapped.
     *
     * @param title
     *            the notification title, not {@code null}
     * @return a new builder, not {@code null}
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

    @Override
    public String toString() {
        return toJson();
    }

    /**
     * Converts this Web Push message into JSON format string.
     *
     * @return JSON representation of this message
     */
    public String toJson() {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("title", title);
        if (options != null) {
            json.set("options", options);
        }
        return json.toString();
    }

    private static ObjectNode getBodyOption(String body) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        if (body != null) {
            objectNode.put("body", body);
        }
        return objectNode;
    }

    /**
     * Builds a {@link WebPushMessage} from the notification options that most
     * applications need, including the view that opens when the user taps the
     * notification. Create one with {@link WebPushMessage#builder(String)}.
     *
     * <pre>
     * WebPushMessage message = WebPushMessage.builder("Sauna is ready")
     *         .body("80 °C")
     *         .route(DeviceView.class, new RouteParameters("id", "SAUN"))
     *         .tag("device-SAUN").renotify(true).build();
     * </pre>
     *
     * Options not covered here can be given as JSON with
     * {@link WebPushMessage#WebPushMessage(String, ObjectNode)}.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerRegistration/showNotification#parameters">
     *      showNotification parameters</a>
     */
    public static final class Builder implements Serializable {

        private final String title;
        private String body;
        private String icon;
        private String tag;
        private boolean renotify;
        private String url;

        private Builder(String title) {
            this.title = Objects.requireNonNull(title,
                    "title must not be null");
        }

        /**
         * Sets the text shown below the title of the notification.
         *
         * @param body
         *            the notification body, or {@code null} for none
         * @return this builder
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the URL of the image shown in the notification, relative to the
         * application's root URL.
         *
         * @param icon
         *            the icon URL, or {@code null} to use the browser default
         * @return this builder
         */
        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Sets the tag of the notification. A notification replaces an earlier
         * one with the same tag instead of being shown beside it, for example
         * to keep a single notification per device or conversation up to date.
         *
         * @param tag
         *            the notification tag, or {@code null} for none
         * @return this builder
         */
        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Sets whether the user is alerted again, with sound and vibration,
         * when the notification replaces an earlier one with the same tag.
         * Without it, a replacing notification updates silently. Requires a
         * {@linkplain #tag(String) tag}.
         *
         * @param renotify
         *            {@code true} to alert the user on replacement
         * @return this builder
         */
        public Builder renotify(boolean renotify) {
            this.renotify = renotify;
            return this;
        }

        /**
         * Sets the URL that opens when the user taps the notification. The URL
         * is resolved against the application's root URL like a link, so
         * {@code "device/SAUN"} works the same with any context path, while
         * {@code "/device/SAUN"} is relative to the server root. If the
         * application is already open, its window is focused and navigated to
         * the URL instead of opening a second one.
         * <p>
         * Without a URL, tapping the notification opens the application at its
         * root URL, or focuses it as it is when it is already open.
         *
         * @param url
         *            the URL to open, or {@code null} for the application's
         *            root
         * @return this builder
         * @see #route(Class, RouteParameters)
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the view that opens when the user taps the notification, as for
         * {@link #url(String)}.
         * <p>
         * The URL of the view is resolved when this method is called, from the
         * application scope routes, so a {@code VaadinService} has to be
         * available, as it is in a request or within {@code UI.access}. In a
         * background thread without one, resolve the URL with
         * {@link RouteConfiguration#forRegistry} and pass it to
         * {@link #url(String)}.
         *
         * @param navigationTarget
         *            the view to open, not {@code null}
         * @return this builder
         * @throws com.vaadin.flow.router.NotFoundException
         *             if the view has no route without parameters
         * @see #route(Class, RouteParameters)
         */
        public Builder route(Class<? extends Component> navigationTarget) {
            return route(navigationTarget, RouteParameters.empty());
        }

        /**
         * Sets the view that opens when the user taps the notification, with
         * the given route parameters, as for {@link #url(String)}.
         * <p>
         * The URL of the view is resolved when this method is called, from the
         * application scope routes, so a {@code VaadinService} has to be
         * available, as it is in a request or within {@code UI.access}. In a
         * background thread without one, resolve the URL with
         * {@link RouteConfiguration#forRegistry} and pass it to
         * {@link #url(String)}.
         *
         * @param navigationTarget
         *            the view to open, not {@code null}
         * @param parameters
         *            the route parameters of the view, not {@code null}
         * @return this builder
         * @throws com.vaadin.flow.router.NotFoundException
         *             if the view has no route that matches the parameters
         * @see #route(Class)
         */
        public Builder route(Class<? extends Component> navigationTarget,
                RouteParameters parameters) {
            return url(RouteConfiguration.forApplicationScope()
                    .getUrl(navigationTarget, parameters));
        }

        /**
         * Creates the message with the options set on this builder.
         *
         * @return a new message, not {@code null}
         * @throws IllegalStateException
         *             if renotify is set without a tag
         */
        public WebPushMessage build() {
            if (renotify && tag == null) {
                throw new IllegalStateException(
                        "renotify requires a tag, as only a notification that replaces another one with the same tag can alert again");
            }
            ObjectNode options = JacksonUtils.createObjectNode();
            if (body != null) {
                options.put("body", body);
            }
            if (icon != null) {
                options.put("icon", icon);
            }
            if (tag != null) {
                options.put("tag", tag);
            }
            if (renotify) {
                options.put("renotify", true);
            }
            if (url != null) {
                options.putObject("data").put("url", url);
            }
            return new WebPushMessage(title, options);
        }
    }
}
