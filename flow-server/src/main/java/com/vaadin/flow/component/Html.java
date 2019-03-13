/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;

import com.vaadin.flow.dom.Element;

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
            setOuterHtml(Jsoup.parse(stream, UTF_8.name(), ""));
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

        setOuterHtml(Jsoup.parse(outerHtml));
    }

    private void setOuterHtml(Document doc) {
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

        Component.setElement(this, new Element(root.tagName()));
        attrs.forEach(this::setAttribute);

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

}
