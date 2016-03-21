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
package com.vaadin.hummingbird.dom;

import com.vaadin.shared.ApplicationConstants;

/**
 * Utility class which provides helpers to create {@link Element} instances.
 *
 * @author Vaadin
 * @since
 */
public class ElementFactory {

    /**
     * Creates an {@code &lt;a>} element.
     *
     * @return an {@code &lt;a>} element.
     */
    public static Element createAnchor() {
        return new Element("a");
    }

    /**
     * Creates an {@code &lt;a>} element configured using the given parameters.
     *
     * @param href
     *            the href parameter for the link
     * @return an {@code &lt;a>} element.
     */
    public static Element createAnchor(String href) {
        return createAnchor().setAttribute("href", href);
    }

    /**
     * Creates an {@code &lt;a>} element configured using the given parameters.
     *
     * @param href
     *            the href parameter for the element
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;a>} element configured using the given parameters
     */
    public static Element createAnchor(String href, String textContent) {
        return createAnchor(href).setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;a>} element configured using the given parameters
     * and set up to be used as a router link (handles view navigation without a
     * page reload).
     *
     * @param href
     *            the href parameter for the element
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;a>} element configured as a router link and using
     *         the given parameters
     */
    public static Element createRouterLink(String href, String textContent) {
        return createAnchor(href, textContent)
                .setAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "");
    }

    /**
     * Creates a {@code &lt;br>} element.
     *
     * @return a {@code &lt;br>} element.
     */
    public static Element createBr() {
        return new Element("br");
    }

    /**
     * Creates a {@code &lt;button>} element.
     *
     * @return a {@code &lt;button>} element.
     */
    public static Element createButton() {
        return new Element("button");
    }

    /**
     * Creates a {@code &lt;button>} element configured using the given
     * parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;button>} element configured using the given
     *         parameters.
     */
    public static Element createButton(String textContent) {
        return createButton().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;div>} element.
     *
     * @return a {@code &lt;div>} element.
     */
    public static Element createDiv() {
        return new Element("div");
    }

    /**
     * Creates a {@code &lt;div>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;div>} element configured using the given parameters.
     */
    public static Element createDiv(String textContent) {
        return createDiv().setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;h1>} element.
     *
     * @return an {@code &lt;h1>} element.
     */
    public static Element createH1() {
        return new Element("h1");
    }

    /**
     * Creates an {@code &lt;h2>} element.
     *
     * @return an {@code &lt;h2>} element.
     */
    public static Element createH2() {
        return new Element("h2");
    }

    /**
     * Creates an {@code &lt;h3>} element.
     *
     * @return an {@code &lt;h3>} element.
     */
    public static Element createH3() {
        return new Element("h3");
    }

    /**
     * Creates an {@code &lt;h4>} element.
     *
     * @return an {@code &lt;h4>} element.
     */
    public static Element createH4() {
        return new Element("h4");
    }

    /**
     * Creates a {@code &lt;h1>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h1>} element configured using the given parameters.
     */
    public static Element createH1(String textContent) {
        return createH1().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h2>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h2>} element configured using the given parameters.
     */
    public static Element createH2(String textContent) {
        return createH2().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h3>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h3>} element configured using the given parameters.
     */
    public static Element createH3(String textContent) {
        return createH3().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h4>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h4>} element configured using the given parameters.
     */
    public static Element createH4(String textContent) {
        return createH4().setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;hr>} element.
     *
     * @return an {@code &lt;hr>} element.
     */
    public static Element createHr() {
        return new Element("hr");
    }

    /**
     * Creates an {@code &lt;input>} element.
     *
     * @return an {@code &lt;input>} element.
     */
    public static Element createInput() {
        return new Element("input");
    }

    /**
     * Creates an {@code &lt;option>} element.
     *
     * @return an {@code &lt;option>} element.
     */
    public static Element createOption() {
        return new Element("option");
    }

    /**
     * Creates an {@code &lt;option>} element configured using the given
     * parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;option>} element configured using the given
     *         parameters.
     */
    public static Element createOption(String textContent) {
        return createOption().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;pre>} element.
     *
     * @return a {@code &lt;pre>} element.
     */
    public static Element createPre() {
        return new Element("pre");
    }

    /**
     * Creates a {@code &lt;select>} element.
     *
     * @return a {@code &lt;select>} element.
     */
    public static Element createSelect() {
        return new Element("select");
    }

    /**
     * Creates a {@code &lt;span>} element.
     *
     * @return a {@code &lt;span>} element.
     */
    public static Element createSpan() {
        return new Element("span");
    }

    /**
     * Creates a {@code &lt;span>} element configured using the given
     * parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;span>} element configured using the given
     *         parameters.
     */
    public static Element createSpan(String textContent) {
        return createSpan().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;textarea>} element.
     *
     * @return a {@code &lt;textarea>} element.
     */
    public static Element createTextarea() {
        return new Element("textarea");
    }

    /**
     * Creates a {@code &lt;b>} element.
     *
     * @return a {@code &lt;b>} element.
     */
    public static Element createBold() {
        return new Element("b");
    }

    /**
     * Creates a {@code &lt;b>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;b>} element configured using the given parameters..
     */
    public static Element createBold(String textContent) {
        return createBold().setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;i>} element.
     *
     * @return an {@code &lt;i>} element.
     */
    public static Element createItalic() {
        return new Element("i");
    }

    /**
     * Creates an {@code &lt;i>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;i>} element configured using the given parameters..
     */
    public static Element createItalic(String textContent) {
        return createItalic().setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;u>} element.
     *
     * @return an {@code &lt;u>} element.
     */
    public static Element createUnderline() {
        return new Element("u");
    }

    /**
     * Creates a {@code &lt;u>} element configured using the given parameters.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;u>} element configured using the given parameters..
     */
    public static Element createUnderline(String textContent) {
        return createUnderline().setTextContent(textContent);
    }

}
