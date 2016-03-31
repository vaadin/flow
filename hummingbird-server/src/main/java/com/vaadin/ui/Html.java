/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;

import com.vaadin.hummingbird.dom.Element;

/**
 * A component which encapsulates a given HTML fragment with a single root
 * element.
 * <p>
 * Note that it is the developer's responsibility to sanitize and remove any
 * dangerous parts of the HTML before sending it to the user through this
 * component.
 * <p>
 * This component does not expand the HTML fragment into a server side DOM tree
 * so you cannot traverse or modify the HTML on the server. The root element can
 * be accessed through {@link #getElement()} and the inner HTML through #get
 * <p>
 * The HTML fragment cannot be changed after creation. You should create a new
 * instance to encapsulate another fragment.
 *
 * @author Vaadin
 * @since
 */
public class Html implements Component {

    private static final String INNER_HTML = "innerHTML";
    private Element element;

    /**
     * Creates an instance based on the HTML fragment read from the stream. The
     * fragment must have exactly one root element.
     * <p>
     * A best effort is done to parse broken HTML but no guarantees are given
     * for how invalid HTML is handled.
     *
     * @param stream
     *            the input stream which provides the HTML in UTF-8
     * @throws UncheckedIOException
     *             if reading the stream fails
     */
    public Html(InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("HTML stream cannot be null");
        }
        try {
            setOuterHtml(Jsoup.parse(stream, "UTF-8", ""));
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
     *
     * @param outerHtml
     *            the HTML to wrap
     */
    public Html(String outerHtml) {
        if (outerHtml == null || outerHtml.isEmpty()) {
            throw new IllegalArgumentException("HTML cannot be null or empty");
        }

        setOuterHtml(Jsoup.parse(outerHtml));
    }

    private void setOuterHtml(Document doc) {
        int nrChildren = doc.body().children().size();
        if (nrChildren != 1) {
            throw new IllegalArgumentException(
                    "HTML must contain exactly one root element. Found "
                            + nrChildren);
        }

        org.jsoup.nodes.Element root = doc.body().child(0);
        Attributes attrs = root.attributes();

        element = new Element(root.tagName());
        attrs.forEach(a -> {
            String name = a.getKey();
            String value = a.getValue();
            if (name.equals("style")) {
                getLogger().warning("Style values '" + value
                        + "' ignored for root element.");
            } else {
                element.setAttribute(name, value);
            }
        });

        setInnerHtml(root.html());

    }

    /**
     * Sets the inner HTML, i.e. everything inside the root element.
     *
     * @param innerHtml
     *            the inner HTML
     */
    private void setInnerHtml(String innerHtml) {
        getElement().setProperty(INNER_HTML, innerHtml);
    }

    /**
     * Gets the inner HTML, i.e. everything inside the root element.
     *
     * @return the inner HTML
     */
    public String getInnerHtml() {
        return getElement().getProperty(INNER_HTML);
    }

    @Override
    public Element getElement() {
        return element;
    }

    private static final Logger getLogger() {
        return Logger.getLogger(Html.class.getName());
    }

}
