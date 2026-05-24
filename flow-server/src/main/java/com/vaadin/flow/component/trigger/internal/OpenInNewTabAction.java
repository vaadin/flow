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
package com.vaadin.flow.component.trigger.internal;

import java.util.Objects;
import java.util.regex.Pattern;

import com.vaadin.flow.dom.JsFunction;

/**
 * Opens a URL in a new browser tab or window via {@code window.open(url,
 * "_blank", features)} when the bound trigger fires.
 * <p>
 * Most browsers block {@code window.open} calls that don't happen inside a
 * transient user activation. Bind this action to a {@link Trigger} that fires
 * during such a gesture — typically a {@link ClickTrigger} — so the call
 * inherits the gesture and the popup is allowed.
 * <p>
 * Two axes of configuration:
 * <ul>
 * <li><b>URL</b> — either a known {@link String} or an {@link Action.Input} of
 * {@code String} resolved on the client at fire time (e.g. the current value of
 * an input field, or a {@code blob:} URL the client just minted).</li>
 * <li><b>Features</b> — the third argument to {@code window.open}, controlling
 * popup characteristics such as opener access (the standard {@code "noopener"}
 * / {@code "noreferrer"} tokens) and window sizing
 * ({@code "width=600,height=400"}). When not supplied, defaults to
 * {@value #DEFAULT_FEATURES}, which severs {@code window.opener} on the new
 * page and strips the {@code Referer} header — the safe default for opening
 * untrusted URLs. Callers that supply their own features are responsible for
 * including those tokens themselves if they want the same protection.</li>
 * </ul>
 * <p>
 * The target is always {@code "_blank"} — opening into the current tab is not
 * the point of this action, and reusing a named window has different popup
 * blocker semantics that this primitive does not try to model. If you need to
 * navigate the current tab, use {@code Page.setLocation}; if you need named
 * windows, call {@code window.open} via {@code executeJs} directly.
 * <p>
 * {@code javascript:} URLs are rejected: static-{@link String} URLs throw
 * {@link IllegalArgumentException} from the constructor, and {@link Input} URLs
 * resolved on the client are silently dropped at fire time (the generated JS
 * short-circuits {@code window.open}). This prevents an attacker-controlled
 * value flowing through a {@link PropertyInput} from executing arbitrary script
 * in the opener's origin. The check mirrors the URL parser's leading-whitespace
 * handling (C0 controls and ASCII space are stripped before the scheme) so a
 * leading tab/newline can't smuggle one through.
 * <p>
 * No outcome callback: the browser does not reliably report whether the popup
 * was blocked (the spec lets {@code window.open} return {@code null}, but not
 * every blocker path does so), and never reports when the new tab is closed.
 *
 * <pre>{@code
 * // Open a known URL in a new tab.
 * new ClickTrigger(button)
 *         .triggers(new OpenInNewTabAction("https://vaadin.com/docs"));
 *
 * // Open whatever URL the user typed into a field.
 * Action.Input<String> urlInput = new PropertyInput<>(urlField, "value",
 *         String.class);
 * new ClickTrigger(button).triggers(new OpenInNewTabAction(urlInput));
 *
 * // Open a sized popup window with custom features. Note that supplying
 * // features replaces the noopener/noreferrer defaults — include them
 * // explicitly if you want the same protection.
 * new ClickTrigger(button).triggers(new OpenInNewTabAction("/help",
 *         "noopener,noreferrer,width=600,height=400"));
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class OpenInNewTabAction extends Action {

    /**
     * Default features applied when the caller does not supply their own:
     * severs {@code window.opener} on the new page and strips the
     * {@code Referer} header.
     */
    static final String DEFAULT_FEATURES = "noopener,noreferrer";

    /**
     * Matches a {@code javascript:} scheme prefix, ignoring leading C0 controls
     * and ASCII space (the characters the URL parser strips before reading the
     * scheme). Case-insensitive so {@code JavaScript:}, {@code JAVASCRIPT:}, …
     * are all caught.
     */
    private static final Pattern JAVASCRIPT_SCHEME = Pattern
            .compile("^[\\x00-\\x20]*javascript:", Pattern.CASE_INSENSITIVE);

    private final Action.Input<String> urlInput;
    private final Action.Input<String> featuresInput;

    /**
     * Opens {@code url} in a new tab with the default features
     * ({@value #DEFAULT_FEATURES}).
     *
     * @param url
     *            the URL to open, not {@code null} and not a
     *            {@code javascript:} URL
     * @throws IllegalArgumentException
     *             if {@code url} starts with {@code javascript:} (after
     *             stripping leading whitespace/control characters)
     */
    public OpenInNewTabAction(String url) {
        this(urlLiteral(url), new LiteralInput<>(DEFAULT_FEATURES));
    }

    /**
     * Opens {@code url} in a new tab with the given {@code features}. The
     * features string is passed verbatim as the third argument to
     * {@code window.open}; supplying it replaces — does not extend — the
     * default features, so include {@code "noopener,noreferrer"} explicitly if
     * you want the same protection.
     *
     * @param url
     *            the URL to open, not {@code null} and not a
     *            {@code javascript:} URL
     * @param features
     *            the features string for {@code window.open}, not {@code null}
     * @throws IllegalArgumentException
     *             if {@code url} starts with {@code javascript:} (after
     *             stripping leading whitespace/control characters)
     */
    public OpenInNewTabAction(String url, String features) {
        this(urlLiteral(url), literal(features, "features"));
    }

    /**
     * Opens a URL resolved on the client at fire time, with the default
     * features ({@value #DEFAULT_FEATURES}). {@code javascript:} URLs produced
     * by the input are blocked on the client.
     *
     * @param url
     *            input supplying the URL when the trigger fires, not
     *            {@code null}
     */
    public OpenInNewTabAction(Action.Input<String> url) {
        this(Objects.requireNonNull(url, "url must not be null"),
                new LiteralInput<>(DEFAULT_FEATURES));
    }

    /**
     * Opens a URL resolved on the client at fire time, with features also
     * resolved on the client. {@code javascript:} URLs produced by the input
     * are blocked on the client.
     *
     * @param url
     *            input supplying the URL when the trigger fires, not
     *            {@code null}
     * @param features
     *            input supplying the features string when the trigger fires,
     *            not {@code null}
     */
    public OpenInNewTabAction(Action.Input<String> url,
            Action.Input<String> features) {
        this.urlInput = Objects.requireNonNull(url, "url must not be null");
        this.featuresInput = Objects.requireNonNull(features,
                "features must not be null");
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // Defence in depth for Input-based URLs whose value is only known on
        // the client. Static String URLs are rejected by the constructor, so
        // for those this check is redundant — but keeping it unconditional
        // keeps the rendered JS identical regardless of how the URL was
        // supplied, and protects LiteralInput<>("javascript:…") too. The
        // regex mirrors the URL parser's leading-whitespace stripping so a
        // tab/newline before "javascript:" can't slip past.
        // $0 = URL input's JsFunction; $1 = features input's JsFunction —
        // both invoked with the firing event so handler-scoped inputs work.
        return JsFunction.of(
                "((u) => /^[\\x00-\\x20]*javascript:/i.test(String(u))"
                        + " || window.open(u, \"_blank\", $1(event)))"
                        + "($0(event))",
                urlInput.toJs(trigger), featuresInput.toJs(trigger))
                .withArguments("event");
    }

    private static LiteralInput<String> urlLiteral(String url) {
        Objects.requireNonNull(url, "url must not be null");
        if (JAVASCRIPT_SCHEME.matcher(url).find()) {
            throw new IllegalArgumentException(
                    "javascript: URLs are not allowed");
        }
        return new LiteralInput<>(url);
    }

    private static LiteralInput<String> literal(String value, String name) {
        return new LiteralInput<>(
                Objects.requireNonNull(value, name + " must not be null"));
    }
}
