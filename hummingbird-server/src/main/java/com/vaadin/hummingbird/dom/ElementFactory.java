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

    private ElementFactory() {
        // Static helpers only
    }

    /**
     * Creates an {@code &lt;a>} element.
     *
     * @return an {@code &lt;a>} element.
     */
    public static Element createAnchor() {
        return new Element("a");
    }

    /**
     * Creates an {@code &lt;a>} with the given {@code href} attribute.
     *
     * @param href
     *            the href attribute for the link
     * @return an {@code &lt;a>} element.
     */
    public static Element createAnchor(String href) {
        return createAnchor().setAttribute("href", href);
    }

    /**
     * Creates an {@code &lt;a>} element with the given {@code href} attribute
     * and text content.
     *
     * @param href
     *            the href parameter for the element
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;a>} element.
     */
    public static Element createAnchor(String href, String textContent) {
        return createAnchor(href).setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;a>} element with the given {@code href} attribute,
     * text content and the router link attribute. Router links are handled by
     * the framework to perform view navigation without a page reload.
     *
     * @param href
     *            the href parameter for the element
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;a>} element.
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
     * Creates a {@code &lt;button>} with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;button>} element.
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
     * Creates a {@code &lt;div>} with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;div>} element.
     */
    public static Element createDiv(String textContent) {
        return createDiv().setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;h1>} element.
     *
     * @return an {@code &lt;h1>} element.
     */
    public static Element createHeading1() {
        return new Element("h1");
    }

    /**
     * Creates an {@code &lt;h2>} element.
     *
     * @return an {@code &lt;h2>} element.
     */
    public static Element createHeading2() {
        return new Element("h2");
    }

    /**
     * Creates an {@code &lt;h3>} element.
     *
     * @return an {@code &lt;h3>} element.
     */
    public static Element createHeading3() {
        return new Element("h3");
    }

    /**
     * Creates an {@code &lt;h4>} element.
     *
     * @return an {@code &lt;h4>} element.
     */
    public static Element createHeading4() {
        return new Element("h4");
    }

    /**
     * Creates an {@code &lt;h5>} element.
     *
     * @return an {@code &lt;h5>} element.
     */
    public static Element createHeading5() {
        return new Element("h5");
    }

    /**
     * Creates an {@code &lt;h6>} element.
     *
     * @return an {@code &lt;h6>} element.
     */
    public static Element createHeading6() {
        return new Element("h6");
    }

    /**
     * Creates a {@code &lt;h1>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h1>} element.
     */
    public static Element createHeading1(String textContent) {
        return createHeading1().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h2>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h2>} element.
     */
    public static Element createHeading2(String textContent) {
        return createHeading2().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h3>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h3>} element.
     */
    public static Element createHeading3(String textContent) {
        return createHeading3().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h4>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h4>} element.
     */
    public static Element createHeading4(String textContent) {
        return createHeading4().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h5>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h5>} element.
     */
    public static Element createHeading5(String textContent) {
        return createHeading5().setTextContent(textContent);
    }

    /**
     * Creates a {@code &lt;h6>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h6>} element.
     */
    public static Element createHeading6(String textContent) {
        return createHeading6().setTextContent(textContent);
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
     * Creates an {@code &lt;input>} element with the given type.
     *
     * @return an {@code &lt;input>} element
     */
    public static Element createInput(String type) {
        return new Element("input").setAttribute("type", type);
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
     * Creates an {@code &lt;option>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;option>} element.
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
     * Creates a {@code &lt;pre>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;pre>} element.
     */
    public static Element createPre(String textContent) {
        return createPre().setTextContent(textContent);
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
     * Creates a {@code &lt;span>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;span>} element.
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
     * Creates a {@code &lt;strong>} element.
     *
     * @return a {@code &lt;strong>} element.
     */
    public static Element createStrong() {
        return new Element("strong");
    }

    /**
     * Creates a {@code &lt;strong>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;strong>} element
     */
    public static Element createStrong(String textContent) {
        return createStrong().setTextContent(textContent);
    }

    /**
     * Creates an {@code &lt;em>} element.
     *
     * @return an {@code &lt;em>} element.
     */
    public static Element createEmphasis() {
        return new Element("em");
    }

    /**
     * Creates an {@code &lt;em>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;em>} element.
     */
    public static Element createEmphasis(String textContent) {
        return createEmphasis().setTextContent(textContent);
    }

}
