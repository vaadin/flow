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
package com.vaadin.flow.component.webshare;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;
import com.vaadin.flow.component.trigger.internal.ShareAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Fluent surface returned from {@link WebShare#onClick}, used to declare what a
 * click should hand to the browser's native share sheet. Call {@link #share}
 * with the title, text, and/or URL to share. The share runs in the browser at
 * click time, while the user gesture required by the Web Share API is still
 * valid.
 * <p>
 * Actions come in two flavours: fire-and-forget (one argument) and observed
 * (with {@code onShared}/{@code onError} callbacks). {@code onShared} fires
 * after the user picks a share target; {@code onError} fires when the user
 * dismisses the sheet (the browser reports {@code AbortError}) or when the call
 * is rejected for other reasons (no gesture, permissions policy block,
 * unsupported browser). Both consumers are required in the observed form — pass
 * {@code () -> {}} or {@code err -> {}} to opt out of one.
 *
 * <pre>{@code
 * Button share = new Button("Share");
 * WebShare.onClick(share).share(
 *         ShareContent.create().title("Hello").url("https://vaadin.com"));
 *
 * WebShare.onClick(share).share(
 *         ShareContent.create().url("https://vaadin.com"),
 *         () -> Notification.show("Shared!"),
 *         err -> Notification.show("Cancelled: " + err.name()));
 * }</pre>
 */
public final class WebShareBinding implements Serializable {

    private final Trigger trigger;

    WebShareBinding(Trigger trigger) {
        this.trigger = Objects.requireNonNull(trigger);
    }

    /**
     * Invokes the browser's native share sheet with the given content when the
     * underlying trigger fires.
     *
     * @param content
     *            the content, not {@code null}; must have at least one slot set
     * @throws IllegalArgumentException
     *             if {@code content} has no slots set
     */
    public void share(ShareContent content) {
        Objects.requireNonNull(content, "content must not be null");
        bind(new ShareAction(content.getTitleInput(), content.getTextInput(),
                content.getUrlInput()));
    }

    /**
     * Like {@link #share(ShareContent)} but reports the outcome back to the
     * server.
     *
     * @param content
     *            the content, not {@code null}; must have at least one slot set
     * @param onShared
     *            UI-thread callback invoked after the user picked a share
     *            target, not {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error (including
     *            {@code AbortError} when the user dismissed the sheet), not
     *            {@code null}
     * @throws IllegalArgumentException
     *             if {@code content} has no slots set
     */
    public void share(ShareContent content, SerializableRunnable onShared,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(content, "content must not be null");
        bind(new ShareAction(content.getTitleInput(), content.getTextInput(),
                content.getUrlInput(), onShared, onError));
    }

    private void bind(ShareAction action) {
        trigger.triggers(action);
    }
}
