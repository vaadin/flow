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

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;

/**
 * Builds a {@link WebPushMessage} from the notification options that most
 * applications need, including the view that opens when the user taps the
 * notification. Create one with {@link WebPushMessage#builder(String)}.
 *
 * <pre>
 * WebPushMessage message = WebPushMessage.builder("Sauna is ready")
 *         .withBody("80 °C")
 *         .withRoute(DeviceView.class, new RouteParameters("id", "SAUN"))
 *         .withTag("device-SAUN").withRenotify(true).build();
 * </pre>
 *
 * Options not covered here can be given as JSON with
 * {@link WebPushMessage#WebPushMessage(String, ObjectNode)}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerRegistration/showNotification#parameters">
 *      showNotification parameters</a>
 */
public final class WebPushMessageBuilder implements Serializable {

    private final String title;
    private String body;
    private String icon;
    private String tag;
    private boolean renotify;
    private String url;

    WebPushMessageBuilder(String title) {
        this.title = Objects.requireNonNull(title, "title must not be null");
    }

    /**
     * Sets the text shown below the title of the notification.
     *
     * @param body
     *            the notification body, or {@code null} for none
     * @return this builder
     */
    public WebPushMessageBuilder withBody(String body) {
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
    public WebPushMessageBuilder withIcon(String icon) {
        this.icon = icon;
        return this;
    }

    /**
     * Sets the tag of the notification. A notification replaces an earlier one
     * with the same tag instead of being shown beside it, for example to keep a
     * single notification per device or conversation up to date.
     *
     * @param tag
     *            the notification tag, or {@code null} for none
     * @return this builder
     */
    public WebPushMessageBuilder withTag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * Sets whether the user is alerted again, with sound and vibration, when
     * the notification replaces an earlier one with the same tag. Without it, a
     * replacing notification updates silently. Requires a
     * {@linkplain #withTag(String) tag}.
     *
     * @param renotify
     *            {@code true} to alert the user on replacement
     * @return this builder
     */
    public WebPushMessageBuilder withRenotify(boolean renotify) {
        this.renotify = renotify;
        return this;
    }

    /**
     * Sets the URL that opens when the user taps the notification. A relative
     * URL is resolved against the application's root URL, so
     * {@code "device/SAUN"} works the same with any context path. If the
     * application is already open, its window is focused and navigated to the
     * URL instead of opening a second one.
     * <p>
     * Without a URL, tapping the notification opens the application at its root
     * URL, or focuses it as it is when it is already open.
     *
     * @param url
     *            the URL to open, or {@code null} for the application's root
     * @return this builder
     * @see #withRoute(Class, RouteParameters)
     */
    public WebPushMessageBuilder withUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Sets the view that opens when the user taps the notification, as for
     * {@link #withUrl(String)}.
     * <p>
     * The URL of the view is resolved when this method is called, from the
     * application scope routes, so a {@code VaadinService} has to be available,
     * as it is in a request or within {@code UI.access}. In a background thread
     * without one, resolve the URL with {@link RouteConfiguration#forRegistry}
     * and pass it to {@link #withUrl(String)}.
     *
     * @param navigationTarget
     *            the view to open, not {@code null}
     * @return this builder
     * @throws com.vaadin.flow.router.NotFoundException
     *             if the view has no route without parameters
     * @see #withRoute(Class, RouteParameters)
     */
    public WebPushMessageBuilder withRoute(
            Class<? extends Component> navigationTarget) {
        return withRoute(navigationTarget, RouteParameters.empty());
    }

    /**
     * Sets the view that opens when the user taps the notification, with the
     * given route parameters, as for {@link #withUrl(String)}.
     * <p>
     * The URL of the view is resolved when this method is called, from the
     * application scope routes, so a {@code VaadinService} has to be available,
     * as it is in a request or within {@code UI.access}. In a background thread
     * without one, resolve the URL with {@link RouteConfiguration#forRegistry}
     * and pass it to {@link #withUrl(String)}.
     *
     * @param navigationTarget
     *            the view to open, not {@code null}
     * @param parameters
     *            the route parameters of the view, not {@code null}
     * @return this builder
     * @throws com.vaadin.flow.router.NotFoundException
     *             if the view has no route that matches the parameters
     * @see #withRoute(Class)
     */
    public WebPushMessageBuilder withRoute(
            Class<? extends Component> navigationTarget,
            RouteParameters parameters) {
        return withUrl(RouteConfiguration.forApplicationScope()
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
