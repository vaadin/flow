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
package com.vaadin.flow.component.html;

import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.signals.Signal;

/**
 * Component representing a <code>&lt;details&gt;</code> element.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.DETAILS)
public class NativeDetails extends HtmlComponent
        implements ClickNotifier<NativeDetails> {

    /**
     * Component representing a <code>&lt;summary&gt;</code> element.
     *
     * @author Vaadin Ltd
     */
    @Tag(Tag.SUMMARY)
    public static class Summary extends HtmlContainer
            implements ClickNotifier<Summary> {

        /**
         * Creates a new empty summary.
         */
        public Summary() {
            super();
        }

    }

    private final Summary summary;
    private Component content;

    /**
     * Creates a new details with an empty summary.
     */
    public NativeDetails() {
        super();
        summary = new Summary();
        getElement().appendChild(summary.getElement());
    }

    /**
     * Creates a new details with the given summary.
     *
     * @param summary
     *            the summary to set.
     */
    public NativeDetails(String summary) {
        this();
        this.summary.setText(summary);
    }

    /**
     * Binds a signal's value to the summary text so that the text is updated
     * when the signal's value is updated.
     * <p>
     * While a binding for the summary text is active, any attempt to set the
     * text manually throws
     * {@link com.vaadin.flow.signals.BindingActiveException}. The same happens
     * when trying to bind a new Signal while one is already bound.
     * <p>
     * Bindings are lifecycle-aware and only active while this component is in
     * the attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param summarySignal
     *            the signal to bind, not <code>null</code>
     * @see Element#bindText(Signal)
     *
     * @since 25.1
     */
    public NativeDetails(Signal<String> summarySignal) {
        this();
        Objects.requireNonNull(summarySignal, "summarySignal must not be null");
        bindSummaryText(summarySignal);
    }

    /**
     * Creates a new details with the given content of the summary.
     *
     * @param summaryContent
     *            the summary content to set.
     */
    public NativeDetails(Component summaryContent) {
        this();
        summary.add(summaryContent);
    }

    /**
     * Creates a new details using the provided summary and content.
     *
     * @param summary
     *            the summary text to set.
     * @param content
     *            the content component to set.
     */
    public NativeDetails(String summary, Component content) {
        this(summary);
        setContent(content);
    }

    /**
     * Creates a new details using the provided summary signal and content.
     *
     * @param summarySignal
     *            the signal to bind, not <code>null</code>
     * @param content
     *            the content component to set.
     * @see #bindSummaryText(Signal)
     *
     * @since 25.1
     */
    public NativeDetails(Signal<String> summarySignal, Component content) {
        this(summarySignal);
        setContent(content);
    }

    /**
     * Creates a new details using the provided summary content and content.
     *
     * @param summaryContent
     *            the summary content to set.
     * @param content
     *            the content component to set.
     */
    public NativeDetails(Component summaryContent, Component content) {
        this(summaryContent);
        setContent(content);
    }

    /**
     * Returns {@link Summary} component associated with this details.
     *
     * @return the summary component
     */
    public Summary getSummary() {
        return summary;
    }

    /**
     * Returns the textual summary of this details.
     *
     * @return the text content of the summary, not <code>null</code>
     * @see Element#getText()
     */
    public String getSummaryText() {
        return summary.getText();
    }

    /**
     * Sets the text of the summary. Removes previously set components of the
     * summary.
     *
     * @see #getSummary()
     * @param summary
     *            the summary text to set.
     */
    public void setSummaryText(String summary) {
        this.summary.setText(summary);
    }

    /**
     * Binds a signal's value to the summary text so that the text is updated
     * when the signal's value is updated.
     * <p>
     * While a binding for the summary text is active, any attempt to set the
     * text manually throws
     * {@link com.vaadin.flow.signals.BindingActiveException}. The same happens
     * when trying to bind a new Signal while one is already bound.
     * <p>
     * Bindings are lifecycle-aware and only active while this component is in
     * the attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param summarySignal
     *            the signal to bind, not <code>null</code>
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setSummaryText(String)
     * @see Element#bindText(Signal)
     *
     * @since 25.1
     */
    public void bindSummaryText(Signal<String> summarySignal) {
        this.summary.getElement().bindText(summarySignal);
    }

    /**
     * Sets the components of the summary. Removes previously set text or
     * components of the summary.
     *
     * @see #getSummary()
     * @param summaryContent
     *            the summary content to set.
     */
    public void setSummary(Component... summaryContent) {
        this.summary.removeAll();
        this.summary.add(summaryContent);
    }

    /**
     * Returns the details content which was set via
     * {@link #setContent(Component)}.
     *
     * @return the content of the details, can be <code>null</code>.
     */
    public Component getContent() {
        return content;
    }

    /**
     * Sets the details content and removes the previously set content.
     *
     * @see #getContent()
     * @param content
     *            the content of the details to set
     */
    public void setContent(Component content) {
        Objects.requireNonNull(content, "Content to set cannot be null");
        if (this.content != null) {
            this.content.getElement().removeFromParent();
        }
        this.content = content;
        getElement().appendChild(content.getElement());
    }

    /**
     * Return whether or not the details is opened and the content is displayed.
     *
     * @return whether details are expanded or collapsed
     */
    @Synchronize(property = "open", value = "toggle", allowInert = true)
    public boolean isOpen() {
        return getElement().getProperty("open", false);
    }

    /**
     * Sets whether or not the details should be opened. {@code true} if the
     * details should be opened and the content should be displayed,
     * {@code false} to collapse it.
     *
     * @param open
     *            the boolean value to set
     */
    public void setOpen(boolean open) {
        getElement().setProperty("open", open);
    }

    /**
     * Binds the open state to the given signal. Signal changes push to the DOM
     * property. If a non-null {@code writeCallback} is provided, client-side
     * property changes are pushed back through the callback, making the binding
     * two-way. If {@code writeCallback} is {@code null}, the binding is
     * read-only.
     * <p>
     * While a signal is bound, any attempt to set the open state manually
     * throws {@link com.vaadin.flow.signals.BindingActiveException}.
     *
     * @param signal
     *            the signal to bind, not {@code null}
     * @param writeCallback
     *            callback invoked when the client-side value changes, or
     *            {@code null} for a read-only binding
     * @since 25.1
     */
    public void bindOpen(Signal<Boolean> signal,
            SerializableConsumer<Boolean> writeCallback) {
        Objects.requireNonNull(signal, "Signal cannot be null");
        getElement().bindProperty("open",
                signal.map(v -> v == null ? Boolean.FALSE : v), writeCallback);
    }

    /**
     * Represents the DOM event "toggle".
     * <p>
     * In addition to the usual events supported by HTML elements, the details
     * element supports the toggle event, which is dispatched to the details
     * element whenever its state changes between open and closed.
     * <p>
     * It is sent after the state is changed, although if the state changes
     * multiple times before the browser can dispatch the event, the events are
     * coalesced so that only one is sent.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/details">https://developer.mozilla.org/en-US/docs/Web/HTML/Element/details</a>
     */
    @DomEvent("toggle")
    public static class ToggleEvent extends ComponentEvent<NativeDetails> {

        private final boolean open;

        /**
         * ToggleEvent base constructor.
         * <p>
         * Note: This event is always triggered on client side. Resulting in
         * {@code fromClient} to be always {@code true}.
         *
         * @param source
         *            the source component
         * @param fromClient
         *            <code>true</code> if the event originated from the client
         *            side, <code>false</code> otherwise
         */
        public ToggleEvent(NativeDetails source, boolean fromClient) {
            super(source, fromClient);
            this.open = source.isOpen();
        }

        /**
         * Return whether or not the details was opened or closed in this event.
         * <p>
         * Delegating to the source component after the toggle event occurred.
         *
         * @return whether details are expanded or collapsed
         */
        public boolean isOpened() {
            return open;
        }
    }

    /**
     * Adds a listener for {@code toggle} events fired by the details, which are
     * dispatched to the details element whenever its state changes between open
     * and closed.
     * <p>
     * Note: This event is always triggered on client side. Resulting in
     * {@code isFromClient()} to always return {@code true}.
     *
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    public Registration addToggleListener(
            ComponentEventListener<ToggleEvent> listener) {
        return ComponentUtil.addListener(this, ToggleEvent.class, listener);
    }
}
