/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;

import com.vaadin.flow.dom.Element;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A component that displays a given SVG image.
 * <p>
 * Note, because of implementation details, we wrap the SVG in a div element.
 *
 * @author Vaadin Ltd
 * @since 24.0
 */
public class Svg extends Component {

    /**
     * Creates an instance based on the given SVG input. The string must have
     * exactly one root element.
     *
     * @param stream
     *            the SVG to display
     */
    public Svg(InputStream stream) {
        super(null);
        if (stream == null) {
            throw new IllegalArgumentException("SVG stream cannot be null");
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
     * Creates an instance based on the given SVG string. The the string must
     * have exactly one root element.
     *
     * @param svg
     *            the SVG to display
     */
    public Svg(String svg) {
        super(null);
        if (svg == null || svg.isEmpty()) {
            throw new IllegalArgumentException("HTML cannot be null or empty");
        }

        setOuterHtml(svg, false);
    }

    private void setOuterHtml(String outerHtml, boolean update) {
        Document doc = Jsoup.parseBodyFragment(outerHtml);
        int nrChildren = doc.body().children().size();
        if (nrChildren != 1) {
            String message = "SVG must contain exactly one top level element (ignoring text nodes). Found "
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
        if (root.nodeName().equals("svg")) {
            // SVG can't be handled like normal elements on the
            // client side due to different namespace, wrap in div
            Component.setElement(this, new Element("div"));
            getElement().setProperty("innerHTML", outerHtml);
        } else {
            throw new IllegalArgumentException(
                    "SVG must contain single svg node as a root");
        }

    }

}
