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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the tag to use for the root element for a component created using the
 * default {@link Component} constructor.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Tag {
    /**
     * Tag for an <code>&lt;a&gt;</code>.
     */
    String A = "a";
    /**
     * Tag for an <code>&lt;article&gt;</code>.
     */
    String ARTICLE = "article";
    /**
     * Tag for an <code>&lt;aside&gt;</code>.
     */
    String ASIDE = "aside";
    /**
     * Tag for an <code>&lt;br&gt;</code>.
     */
    String BR = "br";
    /**
     * Tag for an <code>&lt;button&gt;</code>.
     */
    String BUTTON = "button";
    /**
     * Tag for an <code>&lt;dd&gt;</code>.
     */
    String DD = "dd";
    /**
     * Tag for an <code>&lt;div&gt;</code>.
     */
    String DIV = "div";
    /**
     * Tag for an <code>&lt;dl&gt;</code>.
     */
    String DL = "dl";
    /**
     * Tag for an <code>&lt;dt&gt;</code>.
     */
    String DT = "dt";
    /**
     * Tag for an <code>&lt;em&gt;</code>.
     */
    String EM = "em";
    /**
     * Tag for an <code>&lt;footer&gt;</code>.
     */
    String FOOTER = "footer";
    /**
     * Tag for an <code>&lt;h1&gt;</code>.
     */
    String H1 = "h1";
    /**
     * Tag for an <code>&lt;h2&gt;</code>.
     */
    String H2 = "h2";
    /**
     * Tag for an <code>&lt;h3&gt;</code>.
     */
    String H3 = "h3";
    /**
     * Tag for an <code>&lt;h4&gt;</code>.
     */
    String H4 = "h4";
    /**
     * Tag for an <code>&lt;h5&gt;</code>.
     */
    String H5 = "h5";
    /**
     * Tag for an <code>&lt;g6&gt;</code>.
     */
    String H6 = "h6";
    /**
     * Tag for an <code>&lt;header&gt;</code>.
     */
    String HEADER = "header";
    /**
     * Tag for an <code>&lt;hr&gt;</code>.
     */
    String HR = "hr";
    /**
     * Tag for an <code>&lt;iframe&gt;</code>.
     */
    String IFRAME = "iframe";
    /**
     * Tag for an <code>&lt;img&gt;</code>.
     */
    String IMG = "img";
    /**
     * Tag for an <code>&lt;input&gt;</code>.
     */
    String INPUT = "input";
    /**
     * Tag for an <code>&lt;label&gt;</code>.
     */
    String LABEL = "label";
    /**
     * Tag for an <code>&lt;li&gt;</code>.
     */
    String LI = "li";
    /**
     * Tag for an <code>&lt;main&gt;</code>.
     */
    String MAIN = "main";
    /**
     * Tag for an <code>&lt;nav&gt;</code>.
     */
    String NAV = "nav";
    /**
     * Tag for an <code>&lt;ol&gt;</code>.
     */
    String OL = "ol";
    /**
     * Tag for an <code>&lt;option&gt;</code>.
     */
    String OPTION = "option";
    /**
     * Tag for an <code>&lt;p&gt;</code>.
     */
    String P = "p";
    /**
     * Tag for an <code>&lt;pre&gt;</code>.
     */
    String PRE = "pre";
    /**
     * Tag for an <code>&lt;section&gt;</code>.
     */
    String SECTION = "section";
    /**
     * Tag for an <code>&lt;select&gt;</code>.
     */
    String SELECT = "select";
    /**
     * Tag for an <code>&lt;span&gt;</code>.
     */
    String SPAN = "span";
    /**
     * Tag for an <code>&lt;strong&gt;</code>.
     */
    String STRONG = "strong";
    /**
     * Tag for an <code>&lt;textarea&gt;</code>.
     */
    String TEXTAREA = "textarea";
    /**
     * Tag for an <code>&lt;ul&gt;</code>.
     */
    String UL = "ul";

    /**
     * Gets the tag name.
     *
     * @return the tag name
     */
    String value();

}
