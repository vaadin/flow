/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.jsoup.helper.DataUtil;

import com.vaadin.flow.dom.Element;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A component that displays a given SVG image.
 * <p>
 * Note that it is the developer's responsibility to sanitize and remove any
 * dangerous parts of the SVG before sending it to the user through this
 * component. Passing raw input data to the user will possibly lead to
 * cross-site scripting attacks.
 * <p>
 * Note, because of implementation details, we currently wrap the SVG in a div
 * element. This might change in the future.
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
        this();
        setSvg(stream);
    }

    /**
     * Creates an instance based on the given SVG string. The string must have
     * exactly one root element.
     *
     * @param svg
     *            the SVG to display
     */
    public Svg(String svg) {
        this();
        setSvg(svg);
    }

    /**
     * Creates an empty Svg.
     */
    public Svg() {
        super(new Element("div"));
    }

    /**
     * Sets the graphics shown in this component.
     *
     * @param svg
     *            the SVG string
     */
    public void setSvg(String svg) {
        if (svg == null || svg.isEmpty()) {
            setInnerHtml("");
        } else {
            validateAndSet(svg);
        }
    }

    /**
     * Sets the graphics shown in this component.
     *
     * @param stream
     *            the input stream where SVG is read from
     */
    public void setSvg(InputStream stream) {
        validateAndSet(readSvgStreamAsString(stream));
    }

    private String readSvgStreamAsString(InputStream stream) {
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
            return UTF_8.decode(DataUtil.readToByteBuffer(stream, 0))
                    .toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read SVG from stream", e);
        }
    }

    private void validateAndSet(String svgInput) {
        if (!svgInput.startsWith("<svg")) {
            // remove possible xml header & doc types
            int startIndex = svgInput.indexOf("<svg");
            if (startIndex == -1) {
                throw new IllegalArgumentException(
                        "The content don't appear to be SVG");
            }
            svgInput = svgInput.substring(startIndex);
        }
        setInnerHtml(svgInput);
    }

    private void setInnerHtml(String html) {
        getElement().setProperty("innerHTML", html);
    }

}
