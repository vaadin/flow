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
package com.vaadin.flow.dom;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Helpers to create {@link Element} instances.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ElementFactory {

    /**
     * Creates an {@code &lt;a>} element.
     *
     * @return an {@code &lt;a>} element.
     */
    static Element createAnchor() {
        return new Element(Tag.A);
    }

    /**
     * Creates an {@code &lt;a>} with the given {@code href} attribute.
     *
     * @param href
     *            the href attribute for the link
     * @return an {@code &lt;a>} element.
     */
    static Element createAnchor(String href) {
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
    static Element createAnchor(String href, String textContent) {
        return createAnchor(href).setText(textContent);
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
    static Element createRouterLink(String href, String textContent) {
        return createAnchor(href, textContent)
                .setAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "");
    }

    /**
     * Creates a {@code &lt;br>} element.
     *
     * @return a {@code &lt;br>} element.
     */
    static Element createBr() {
        return new Element(Tag.BR);
    }

    /**
     * Creates a {@code &lt;button>} element.
     *
     * @return a {@code &lt;button>} element.
     */
    static Element createButton() {
        return new Element(Tag.BUTTON);
    }

    /**
     * Creates a {@code &lt;button>} with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;button>} element.
     */
    static Element createButton(String textContent) {
        return createButton().setText(textContent);
    }

    /**
     * Creates a {@code &lt;div>} element.
     *
     * @return a {@code &lt;div>} element.
     */
    static Element createDiv() {
        return new Element(Tag.DIV);
    }

    /**
     * Creates a {@code &lt;div>} with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;div>} element.
     */
    static Element createDiv(String textContent) {
        return createDiv().setText(textContent);
    }

    /**
     * Creates an {@code &lt;h1>} element.
     *
     * @return an {@code &lt;h1>} element.
     */
    static Element createHeading1() {
        return new Element(Tag.H1);
    }

    /**
     * Creates an {@code &lt;h2>} element.
     *
     * @return an {@code &lt;h2>} element.
     */
    static Element createHeading2() {
        return new Element(Tag.H2);
    }

    /**
     * Creates an {@code &lt;h3>} element.
     *
     * @return an {@code &lt;h3>} element.
     */
    static Element createHeading3() {
        return new Element(Tag.H3);
    }

    /**
     * Creates an {@code &lt;h4>} element.
     *
     * @return an {@code &lt;h4>} element.
     */
    static Element createHeading4() {
        return new Element(Tag.H4);
    }

    /**
     * Creates an {@code &lt;h5>} element.
     *
     * @return an {@code &lt;h5>} element.
     */
    static Element createHeading5() {
        return new Element(Tag.H5);
    }

    /**
     * Creates an {@code &lt;h6>} element.
     *
     * @return an {@code &lt;h6>} element.
     */
    static Element createHeading6() {
        return new Element(Tag.H6);
    }

    /**
     * Creates a {@code &lt;h1>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h1>} element.
     */
    static Element createHeading1(String textContent) {
        return createHeading1().setText(textContent);
    }

    /**
     * Creates a {@code &lt;h2>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h2>} element.
     */
    static Element createHeading2(String textContent) {
        return createHeading2().setText(textContent);
    }

    /**
     * Creates a {@code &lt;h3>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h3>} element.
     */
    static Element createHeading3(String textContent) {
        return createHeading3().setText(textContent);
    }

    /**
     * Creates a {@code &lt;h4>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h4>} element.
     */
    static Element createHeading4(String textContent) {
        return createHeading4().setText(textContent);
    }

    /**
     * Creates a {@code &lt;h5>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h5>} element.
     */
    static Element createHeading5(String textContent) {
        return createHeading5().setText(textContent);
    }

    /**
     * Creates a {@code &lt;h6>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;h6>} element.
     */
    static Element createHeading6(String textContent) {
        return createHeading6().setText(textContent);
    }

    /**
     * Creates an {@code &lt;hr>} element.
     *
     * @return an {@code &lt;hr>} element.
     */
    static Element createHr() {
        return new Element(Tag.HR);
    }

    /**
     * Creates an {@code &lt;input>} element.
     *
     * @return an {@code &lt;input>} element.
     */
    static Element createInput() {
        return new Element(Tag.INPUT);
    }

    /**
     * Creates an {@code &lt;input>} element with the given type.
     *
     * @param type
     *            the type attribute for the element
     * @return an {@code &lt;input>} element
     */
    static Element createInput(String type) {
        return new Element(Tag.INPUT).setAttribute("type", type);
    }

    /**
     * Creates an {@code &lt;label>} element.
     *
     * @return an {@code &lt;label>} element.
     */
    static Element createLabel() {
        return new Element(Tag.LABEL);
    }

    /**
     * Creates an {@code &lt;label>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;label>} element.
     */
    static Element createLabel(String textContent) {
        return createLabel().setText(textContent);
    }

    /**
     * Creates an {@code &lt;li>} element.
     *
     * @return an {@code &lt;li>} element.
     */
    static Element createListItem() {
        return new Element(Tag.LI);
    }

    /**
     * Creates an {@code &lt;li>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;li>} element.
     */
    static Element createListItem(String textContent) {
        return createListItem().setText(textContent);
    }

    /**
     * Creates an {@code &lt;option>} element.
     *
     * @return an {@code &lt;option>} element.
     */
    static Element createOption() {
        return new Element(Tag.OPTION);
    }

    /**
     * Creates an {@code &lt;option>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;option>} element.
     */
    static Element createOption(String textContent) {
        return createOption().setText(textContent);
    }

    /**
     * Creates a {@code &lt;p>} element.
     *
     * @return a {@code &lt;p>} element.
     */
    static Element createParagraph() {
        return new Element(Tag.P);
    }

    /**
     * Creates a {@code &lt;p>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;p>} element.
     */
    static Element createParagraph(String textContent) {
        return new Element(Tag.P).setText(textContent);
    }

    /**
     * Creates a {@code &lt;pre>} element.
     *
     * @return a {@code &lt;pre>} element.
     */
    static Element createPreformatted() {
        return new Element(Tag.PRE);
    }

    /**
     * Creates a {@code &lt;pre>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;pre>} element.
     */
    static Element createPreformatted(String textContent) {
        return createPreformatted().setText(textContent);
    }

    /**
     * Creates a {@code &lt;select>} element.
     *
     * @return a {@code &lt;select>} element.
     */
    static Element createSelect() {
        return new Element(Tag.SELECT);
    }

    /**
     * Creates a {@code &lt;span>} element.
     *
     * @return a {@code &lt;span>} element.
     */
    static Element createSpan() {
        return new Element(Tag.SPAN);
    }

    /**
     * Creates a {@code &lt;span>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;span>} element.
     */
    static Element createSpan(String textContent) {
        return createSpan().setText(textContent);
    }

    /**
     * Creates a {@code &lt;textarea>} element.
     *
     * @return a {@code &lt;textarea>} element.
     */
    static Element createTextarea() {
        return new Element(Tag.TEXTAREA);
    }

    /**
     * Creates a {@code &lt;ul>} element.
     *
     * @return a {@code &lt;ul>} element.
     */
    static Element createUnorderedList() {
        return new Element(Tag.UL);
    }

    /**
     * Creates a {@code &lt;strong>} element.
     *
     * @return a {@code &lt;strong>} element.
     */
    static Element createStrong() {
        return new Element(Tag.STRONG);
    }

    /**
     * Creates a {@code &lt;strong>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return a {@code &lt;strong>} element
     */
    static Element createStrong(String textContent) {
        return createStrong().setText(textContent);
    }

    /**
     * Creates an {@code &lt;em>} element.
     *
     * @return an {@code &lt;em>} element.
     */
    static Element createEmphasis() {
        return new Element(Tag.EM);
    }

    /**
     * Creates an {@code &lt;em>} element with the given text content.
     *
     * @param textContent
     *            the text content of the element
     * @return an {@code &lt;em>} element.
     */
    static Element createEmphasis(String textContent) {
        return createEmphasis().setText(textContent);
    }

}
