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

/**
 * Utility class which provides helpers to create {@link Element} instances.
 *
 * @author Vaadin
 * @since
 */
public class ElementFactory {
    public static Element createAnchor() {
        return new Element("a");
    }

    public static Element createAnchor(String href) {
        return createAnchor().setAttribute("href", href);
    }

    public static Element createAnchor(String href, String textContent) {
        return createAnchor(href).setTextContent(textContent);
    }

    public static Element createRouterLink(String href, String textContent) {
        return createAnchor(href, textContent).setAttribute("routerlink", "");
    }

    public static Element createBr() {
        return new Element("br");
    }

    public static Element createButton() {
        return new Element("button");
    }

    public static Element createButton(String textContent) {
        return createButton().setTextContent(textContent);
    }

    public static Element createDiv() {
        return ElementFactory.createDiv();
    }

    public static Element createDiv(String textContent) {
        return createDiv().setTextContent(textContent);
    }

    public static Element createH1() {
        return new Element("h1");
    }

    public static Element createH2() {
        return new Element("h2");
    }

    public static Element createH3() {
        return new Element("h3");
    }

    public static Element createH4() {
        return new Element("h4");
    }

    public static Element createH1(String textContent) {
        return createH1().setTextContent(textContent);
    }

    public static Element createH2(String textContent) {
        return createH2().setTextContent(textContent);
    }

    public static Element createH3(String textContent) {
        return createH3().setTextContent(textContent);
    }

    public static Element createH4(String textContent) {
        return createH4().setTextContent(textContent);
    }

    public static Element createHr() {
        return new Element("hr");
    }

    public static Element createInput() {
        return new Element("input");
    }

    public static Element createOption() {
        return new Element("option");
    }

    public static Element createOption(String textContent) {
        return createOption().setTextContent(textContent);
    }

    public static Element createPre() {
        return new Element("pre");
    }

    public static Element createSelect() {
        return new Element("select");
    }

    public static Element createSpan() {
        return new Element("span");
    }

    public static Element createSpan(String textContent) {
        return createSpan().setTextContent(textContent);
    }

    public static Element createTextarea() {
        return new Element("textarea");
    }

    public static Element createBold() {
        return new Element("b");

    }

    public static Element createBold(String viewName) {
        return createBold();
    }

}
