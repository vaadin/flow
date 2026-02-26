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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.dom.SignalBinding;
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
 * This component does not expand the HTML fragment into a server side DOM tree
 * so you cannot traverse or modify the HTML on the server. The root element can
 * be accessed through {@link #getElement()} and the inner HTML through
 * {@link #getInnerHtml()}.
 * <p>
 * The HTML fragment cannot be changed after creation. You should create a new
 * instance to encapsulate another fragment.
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
        if (outerHtml == null || outerHtml.isEmpty()) {
            throw new IllegalArgumentException("HTML cannot be null or empty");
        }

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
     * Sets the content based on the given HTML fragment. The fragment must have
     * exactly one root element, which matches the existing one.
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
        // Disallow manual setting while a binding exists
        getElement().getNode()
                .getFeatureIfInitialized(SignalBindingFeature.class)
                .ifPresent(feature -> {
                    if (feature.hasBinding(SignalBindingFeature.HTML_CONTENT)) {
                        throw new BindingActiveException(
                                "setHtmlContent is not allowed while a binding for HTML content exists.");
                    }
                });
        setOuterHtml(html, true);
    }

    private void setOuterHtml(String outerHtml, boolean update) {
        Document doc = Jsoup.parseBodyFragment(outerHtml);
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
     * component's HTML content (outer HTML) and keeps the content synchronized
     * with the signal value while the component is attached. When the component
     * is detached, signal value changes have no effect.
     * <p>
     * While a Signal is bound to the HTML content, any attempt to set the HTML
     * content manually via {@link #setHtmlContent(String)} throws
     * {@link com.vaadin.flow.signals.BindingActiveException}. The same happens
     * when trying to bind a new Signal while one is already bound.
     * <p>
     * The first value of the signal must have exactly one root element. When
     * updating the content, the root tag name must remain the same as the
     * component's current root tag.
     *
     * @param htmlSignal
     *            the signal to bind, not <code>null</code>
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     */
    public void bindHtmlContent(Signal<String> htmlSignal) {
        Objects.requireNonNull(htmlSignal, "Signal cannot be null");
        SignalBindingFeature feature = getElement().getNode()
                .getFeature(SignalBindingFeature.class);

        if (feature.hasBinding(SignalBindingFeature.HTML_CONTENT)) {
            throw new BindingActiveException();
        }

        SignalBinding<?> binding = ElementEffect.bind(getElement(), htmlSignal,
                (element, value) -> setOuterHtml(value, true));
        feature.setBinding(SignalBindingFeature.HTML_CONTENT,
                binding.getEffectRegistration(), htmlSignal);
    }
}
