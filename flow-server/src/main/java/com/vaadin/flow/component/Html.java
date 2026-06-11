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
package com.vaadin.flow.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.dom.SignalBinding;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A component which encapsulates a given HTML fragment with a single root
 * element.
 * <p>
 * Note that it is the developer's responsibility to sanitize and remove any
 * dangerous parts of the HTML before sending it to the user through this
 * component. Passing raw input data to the user will possibly lead to
 * cross-site scripting attacks.
 * <p>
 * To help with sanitization, a jsoup {@link Safelist} of permitted elements and
 * attributes can be configured through a safelist-accepting constructor. Once
 * configured, all content applied to the component &mdash; the initial value
 * and any value later set through {@link #setHtmlContent(String)} or pushed by
 * a bound signal &mdash; is sanitized with it, so any element or attribute that
 * the safelist doesn't permit is removed. The safelist is fixed at construction
 * and cannot be changed afterwards.
 * <p>
 * jsoup's {@link Safelist} is <b>not</b> {@link java.io.Serializable}, but a
 * Vaadin component must be serializable (sessions can be persisted to disk or
 * replicated across a cluster). Storing a {@code Safelist} directly would break
 * session serialization, so the safelist is supplied through a
 * {@link SerializableSupplier} &mdash; a serializable factory that is invoked
 * to build the safelist &mdash; rather than as a {@code Safelist} instance.
 * <p>
 * For the supplier to be serializable, it must not capture a (non-serializable)
 * {@code Safelist} instance. Use a method reference to a factory, or a lambda
 * that builds a new safelist inline:
 *
 * <pre>{@code
 * // Method reference to one of the standard jsoup factories:
 * new Html(untrustedHtml, Safelist::basic);
 *
 * // Lambda that builds a fresh safelist inline (captures nothing):
 * new Html(untrustedHtml,
 *         () -> Safelist.basic().addTags("figure", "figcaption"));
 *
 * // A reusable supplier kept in a static field:
 * static final SerializableSupplier<Safelist> SAFELIST = () -> Safelist
 *         .relaxed().addAttributes("a", "target");
 * new Html(untrustedHtml, SAFELIST);
 * }</pre>
 *
 * Do <b>not</b> capture an already-built {@code Safelist} in the supplier: that
 * puts the non-serializable instance back into the component's state and fails
 * session serialization:
 *
 * <pre>{@code
 * Safelist safelist = Safelist.basic();
 * // BAD: captures the non-serializable Safelist instance
 * new Html(untrustedHtml, () -> safelist);
 * }</pre>
 *
 * The supplier is invoked when the component is created (and again lazily on
 * the next sanitization after deserialization, since the cached safelist is
 * {@code transient}), not on every sanitization, so it should return an
 * equivalent safelist on each call and must not return {@code null}.
 * <p>
 * The safelist must permit the fragment's single root element; otherwise the
 * root is stripped and the resulting fragment no longer has exactly one root
 * element.
 * <p>
 * This component does not expand the HTML fragment into a server side DOM tree
 * so you cannot traverse or modify the HTML on the server. The root element can
 * be accessed through {@link #getElement()} and the inner HTML through
 * {@link #getInnerHtml()}.
 * <p>
 * The inner content and attributes can be changed after creation through
 * {@link #setHtmlContent(String)} or by binding a signal, but the root tag
 * cannot be changed.
 * <p>
 * Note that this component doesn't support svg element as a root node. See
 * separate {@link Svg} component if you want to display SVG images.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Html extends Component {

    private static final PropertyDescriptor<String, String> innerHtmlDescriptor = PropertyDescriptors
            .propertyWithDefault("innerHTML", "");

    /**
     * Supplies the jsoup safelist used to sanitize all content applied to this
     * component, or {@code null} when no safelist was configured. A supplier is
     * stored (rather than a {@link Safelist}) because {@code Safelist} is not
     * serializable.
     */
    private SerializableSupplier<Safelist> safelistSupplier;

    /**
     * Cached safelist obtained from {@link #safelistSupplier}, so the supplier
     * is not invoked on every sanitization. Transient because {@link Safelist}
     * is not serializable; it is rebuilt from the supplier on demand after
     * deserialization.
     */
    private transient Safelist safelist;

    /**
     * Creates an instance based on the HTML fragment read from the stream. The
     * fragment must have exactly one root element.
     * <p>
     * A best effort is done to parse broken HTML but no guarantees are given
     * for how invalid HTML is handled.
     * <p>
     * Any heading or trailing whitespace is removed while parsing but any
     * whitespace inside the root tag is preserved.
     *
     * @param stream
     *            the input stream which provides the HTML in UTF-8
     * @throws UncheckedIOException
     *             if reading the stream fails
     */
    public Html(InputStream stream) {
        super(null);
        if (stream == null) {
            throw new IllegalArgumentException("HTML stream cannot be null");
        }
        try {
            /*
             * Cannot use any of the methods that accept a stream since they all
             * parse as a document rather than as a body fragment. The logic for
             * reading a stream into a String is the same that is used
             * internally by JSoup if you strip away all the logic to guess an
             * encoding in case one isn't defined.
             */
            setOuterHtml(UTF_8.decode(DataUtil.readToByteBuffer(stream, 0))
                    .toString(), false);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read HTML from stream",
                    e);
        }
    }

    /**
     * Creates an instance based on the HTML fragment read from the stream,
     * sanitized using the safelist obtained from the given supplier. The
     * fragment must have exactly one root element after sanitization.
     * <p>
     * The supplier is also stored on the component, so any later content change
     * via {@link #setHtmlContent(String)} or a bound signal is sanitized with
     * it as well. Any element or attribute that the safelist doesn't permit is
     * removed. The safelist must permit the root element.
     * <p>
     * A best effort is done to parse broken HTML but no guarantees are given
     * for how invalid HTML is handled.
     * <p>
     * Any heading or trailing whitespace is removed while parsing but any
     * whitespace inside the root tag is preserved.
     *
     * @param stream
     *            the input stream which provides the HTML in UTF-8
     * @param safelistSupplier
     *            supplies the safelist of permitted elements and attributes,
     *            not <code>null</code>
     * @throws UncheckedIOException
     *             if reading the stream fails
     * @throws NullPointerException
     *             if the supplier is <code>null</code>
     */
    public Html(InputStream stream,
            SerializableSupplier<Safelist> safelistSupplier) {
        super(null);
        if (stream == null) {
            throw new IllegalArgumentException("HTML stream cannot be null");
        }
        initSafelist(safelistSupplier);
        try {
            String outerHtml = UTF_8
                    .decode(DataUtil.readToByteBuffer(stream, 0)).toString();
            setOuterHtml(outerHtml, false);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read HTML from stream",
                    e);
        }
    }

    /**
     * Creates an instance based on the given HTML fragment. The fragment must
     * have exactly one root element.
     * <p>
     * A best effort is done to parse broken HTML but no guarantees are given
     * for how invalid HTML is handled.
     * <p>
     * Any heading or trailing whitespace is removed while parsing but any
     * whitespace inside the root tag is preserved.
     *
     * @param outerHtml
     *            the HTML to wrap
     */
    public Html(String outerHtml) {
        super(null);
        // null/empty is rejected by setOuterHtml
        setOuterHtml(outerHtml, false);
    }

    /**
     * Creates an instance based on the given HTML fragment, sanitized using the
     * safelist obtained from the given supplier. The fragment must have exactly
     * one root element after sanitization.
     * <p>
     * The supplier is also stored on the component, so any later content change
     * via {@link #setHtmlContent(String)} or a bound signal is sanitized with
     * it as well. Any element or attribute that the safelist doesn't permit is
     * removed. The safelist must permit the root element.
     * <p>
     * A best effort is done to parse broken HTML but no guarantees are given
     * for how invalid HTML is handled.
     * <p>
     * Any heading or trailing whitespace is removed while parsing but any
     * whitespace inside the root tag is preserved.
     *
     * @param outerHtml
     *            the HTML to wrap
     * @param safelistSupplier
     *            supplies the safelist of permitted elements and attributes,
     *            not <code>null</code>
     * @throws NullPointerException
     *             if the supplier is <code>null</code>
     */
    public Html(String outerHtml,
            SerializableSupplier<Safelist> safelistSupplier) {
        super(null);
        initSafelist(safelistSupplier);
        // null/empty html is rejected by setOuterHtml
        setOuterHtml(outerHtml, false);
    }

    /**
     * Creates an instance based on the given HTML fragment signal. The signal's
     * current value must have exactly one root element. Subsequent changes to
     * the signal will update the component's content (root tag cannot be
     * changed after creation).
     *
     * @param htmlSignal
     *            the signal that provides the HTML outer content
     * @throws IllegalArgumentException
     *             if the signal is {@code null} or its current value is null or
     *             empty, or doesn't have exactly one root element
     */
    public Html(Signal<String> htmlSignal) {
        super(null);
        if (htmlSignal == null) {
            throw new IllegalArgumentException("HTML signal cannot be null");
        }
        String outerHtml = htmlSignal.peek();
        if (outerHtml == null || outerHtml.isEmpty()) {
            throw new IllegalArgumentException("HTML cannot be null or empty");
        }
        // Initialize from current signal value (sets the root element and
        // attrs)
        setOuterHtml(outerHtml, false);
        // Bind further updates to inner content and attributes (root tag cannot
        // change)
        bindHtmlContent(htmlSignal);
    }

    /**
     * Creates an instance based on the given HTML fragment signal, with every
     * value sanitized using the safelist obtained from the given supplier. The
     * signal's current value must have exactly one root element after
     * sanitization. Subsequent changes to the signal will update the
     * component's content (root tag cannot be changed after creation).
     * <p>
     * The supplier is stored on the component, so both the initial value and
     * every subsequent value are sanitized with it. Any element or attribute
     * that the safelist doesn't permit is removed. The safelist must permit the
     * root element.
     *
     * @param htmlSignal
     *            the signal that provides the HTML outer content
     * @param safelistSupplier
     *            supplies the safelist of permitted elements and attributes,
     *            not <code>null</code>
     * @throws IllegalArgumentException
     *             if the signal is {@code null} or its current value is null or
     *             empty, or doesn't have exactly one root element
     * @throws NullPointerException
     *             if the supplier is <code>null</code>
     */
    public Html(Signal<String> htmlSignal,
            SerializableSupplier<Safelist> safelistSupplier) {
        super(null);
        if (htmlSignal == null) {
            throw new IllegalArgumentException("HTML signal cannot be null");
        }
        initSafelist(safelistSupplier);
        String outerHtml = htmlSignal.peek();
        if (outerHtml == null || outerHtml.isEmpty()) {
            throw new IllegalArgumentException("HTML cannot be null or empty");
        }
        // Initialize from the current signal value, sanitized through the
        // stored safelist (sets the root element and attrs)
        setOuterHtml(outerHtml, false);
        // Bind further updates; each value is sanitized through the stored
        // safelist as well (root tag cannot change)
        bindHtmlContent(htmlSignal);
    }

    /**
     * Sets the content based on the given HTML fragment. The fragment must have
     * exactly one root element, which matches the existing one.
     * <p>
     * If a safelist supplier was provided at construction, the content is
     * sanitized with it before being used.
     * <p>
     * A best effort is done to parse broken HTML but no guarantees are given
     * for how invalid HTML is handled.
     * <p>
     * Any heading or trailing whitespace is removed while parsing but any
     * whitespace inside the root tag is preserved.
     *
     * @param html
     *            the HTML to wrap
     */
    public void setHtmlContent(String html) {
        ensureNoHtmlContentBinding();
        setOuterHtml(html, true);
    }

    /**
     * Returns the safelist instance for sanitization, lazily (re)building it
     * from the supplier when not cached (e.g. after deserialization).
     *
     * @return the safelist, or {@code null} if no supplier is configured
     */
    private Safelist getSafelistInstance() {
        if (safelist == null && safelistSupplier != null) {
            safelist = resolveSafelist(safelistSupplier);
        }
        return safelist;
    }

    /**
     * Validates and stores the given non-null safelist supplier, eagerly
     * resolving and caching the safelist. Used by the safelist-accepting
     * constructors.
     *
     * @param safelistSupplier
     *            the safelist supplier, not {@code null}
     */
    private void initSafelist(SerializableSupplier<Safelist> safelistSupplier) {
        Objects.requireNonNull(safelistSupplier,
                "Safelist supplier cannot be null");
        this.safelist = resolveSafelist(safelistSupplier);
        this.safelistSupplier = safelistSupplier;
    }

    /**
     * Resolves the safelist from the given supplier, validating that the
     * supplier does not return {@code null}.
     *
     * @param safelistSupplier
     *            the safelist supplier, may be {@code null}
     * @return the safelist, or {@code null} if the supplier is {@code null}
     */
    private static Safelist resolveSafelist(
            SerializableSupplier<Safelist> safelistSupplier) {
        return safelistSupplier == null ? null
                : Objects.requireNonNull(safelistSupplier.get(),
                        "Safelist supplier must not return null");
    }

    /**
     * Throws if a Signal is bound to the HTML content, as manual setting is not
     * allowed while a binding exists.
     */
    private void ensureNoHtmlContentBinding() {
        getElement().getNode()
                .getFeatureIfInitialized(SignalBindingFeature.class)
                .ifPresent(feature -> {
                    if (feature.hasBinding(SignalBindingFeature.HTML_CONTENT)) {
                        throw new BindingActiveException(
                                "setHtmlContent is not allowed while a binding for HTML content exists.");
                    }
                });
    }

    private void setOuterHtml(String outerHtml, boolean update) {
        if (outerHtml == null || outerHtml.isEmpty()) {
            throw new IllegalArgumentException("HTML cannot be null or empty");
        }
        // Sanitize through the configured safelist, if any, before parsing
        String effectiveHtml = outerHtml;
        Safelist currentSafelist = getSafelistInstance();
        if (currentSafelist != null) {
            effectiveHtml = sanitize(outerHtml, currentSafelist);
        }
        Document doc = Jsoup.parseBodyFragment(effectiveHtml);
        int nrChildren = doc.body().children().size();
        if (nrChildren != 1) {
            String message = "HTML must contain exactly one top level element (ignoring text nodes). Found "
                    + nrChildren;
            if (nrChildren > 1) {
                String tagNames = doc.body().children().stream()
                        .map(org.jsoup.nodes.Element::tagName)
                        .collect(Collectors.joining(", "));
                message += " elements with the tag names " + tagNames;
            }
            throw new IllegalArgumentException(message);
        }

        org.jsoup.nodes.Element root = doc.body().child(0);
        Attributes attrs = root.attributes();

        if (!update) {
            Component.setElement(this, new Element(root.tagName()));
        }
        attrs.forEach(this::setAttribute);

        if (update && !root.tagName().equals(getElement().getTag())) {
            throw new IllegalStateException(
                    "Existing root tag '" + getElement().getTag()
                            + "' can't be changed to '" + root.tagName() + "'");
        }

        doc.outputSettings().prettyPrint(false);
        setInnerHtml(root.html());
    }

    /**
     * Removes any element or attribute not permitted by the given safelist,
     * without pretty-printing the result so that whitespace inside the fragment
     * is preserved as-is.
     */
    private static String sanitize(String html, Safelist safelist) {
        Document dirty = Jsoup.parseBodyFragment(html);
        Document clean = new Cleaner(safelist).clean(dirty);
        clean.outputSettings().prettyPrint(false);
        return clean.body().html();
    }

    private void setAttribute(Attribute attribute) {
        String name = attribute.getKey();
        String value = attribute.getValue();
        if (value == null) {
            value = "";
        }
        getElement().setAttribute(name, value);
    }

    /**
     * Sets the inner HTML, i.e. everything inside the root element.
     *
     * @param innerHtml
     *            the inner HTML, not <code>null</code>
     */
    private void setInnerHtml(String innerHtml) {
        set(innerHtmlDescriptor, innerHtml);
    }

    /**
     * Gets the inner HTML, i.e. everything inside the root element.
     *
     * @return the inner HTML, not <code>null</code>
     */
    public String getInnerHtml() {
        return get(innerHtmlDescriptor);
    }

    /**
     * Binds a {@link com.vaadin.flow.signals.Signal}'s value to this
     * component's HTML content (outer HTML). The content is set immediately
     * with the current signal value when the binding is created, and is kept
     * synchronized with any subsequent signal value changes while the component
     * is attached. When the component is detached, signal value changes have no
     * effect.
     * <p>
     * If a safelist supplier was provided at construction, every value &mdash;
     * the initial one and each subsequent change &mdash; is sanitized with it,
     * so any element or attribute that the safelist doesn't permit is removed.
     * <p>
     * While a Signal is bound to the HTML content, any attempt to set the HTML
     * content manually via {@link #setHtmlContent(String)} throws
     * {@link com.vaadin.flow.signals.BindingActiveException}. The same happens
     * when trying to bind a new Signal while one is already bound.
     * <p>
     * The first value of the signal must have exactly one root element. When
     * updating the content, the root tag name must remain the same as the
     * component's current root tag. A {@code null} or empty signal value is
     * rejected with an {@link IllegalArgumentException}.
     *
     * @param htmlSignal
     *            the signal to bind, not <code>null</code>
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     */
    public SignalBinding<String> bindHtmlContent(Signal<String> htmlSignal) {
        Objects.requireNonNull(htmlSignal, "Signal cannot be null");
        SignalBindingFeature feature = getElement().getNode()
                .getFeature(SignalBindingFeature.class);

        if (feature.hasBinding(SignalBindingFeature.HTML_CONTENT)) {
            throw new BindingActiveException();
        }

        SignalBinding<String> binding = ElementEffect.bind(getElement(),
                htmlSignal, (element, value) -> setOuterHtml(value, true));
        feature.setBinding(SignalBindingFeature.HTML_CONTENT, htmlSignal);
        return binding;
    }
}
