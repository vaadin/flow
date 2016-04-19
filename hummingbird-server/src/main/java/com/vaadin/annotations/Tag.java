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
package com.vaadin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.ui.Component;

/**
 * Defines the tag to use for the root element for a component created using the
 * default {@link Component} constructor.
 *
 * @since
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface Tag {
    /**
     * Tag for an <code>&lt;a&gt;</code>
     */
    public static final String A = "a";
    /**
     * Tag for an <code>&lt;br&gt;</code>
     */
    public static final String BR = "br";
    /**
     * Tag for an <code>&lt;button&gt;</code>
     */
    public static final String BUTTON = "button";
    /**
     * Tag for an <code>&lt;div&gt;</code>
     */
    public static final String DIV = "div";
    /**
     * Tag for an <code>&lt;em&gt;</code>
     */
    public static final String EM = "em";
    /**
     * Tag for an <code>&lt;h1&gt;</code>
     */
    public static final String H1 = "h1";
    /**
     * Tag for an <code>&lt;h2&gt;</code>
     */
    public static final String H2 = "h2";
    /**
     * Tag for an <code>&lt;h3&gt;</code>
     */
    public static final String H3 = "h3";
    /**
     * Tag for an <code>&lt;h4&gt;</code>
     */
    public static final String H4 = "h4";
    /**
     * Tag for an <code>&lt;h5&gt;</code>
     */
    public static final String H5 = "h5";
    /**
     * Tag for an <code>&lt;g6&gt;</code>
     */
    public static final String H6 = "h6";
    /**
     * Tag for an <code>&lt;hr&gt;</code>
     */
    public static final String HR = "hr";
    /**
     * Tag for an <code>&lt;input&gt;</code>
     */
    public static final String INPUT = "input";
    /**
     * Tag for an <code>&lt;label&gt;</code>
     */
    public static final String LABEL = "label";
    /**
     * Tag for an <code>&lt;li&gt;</code>
     */
    public static final String LI = "li";
    /**
     * Tag for an <code>&lt;option&gt;</code>
     */
    public static final String OPTION = "option";
    /**
     * Tag for an <code>&lt;p&gt;</code>
     */
    public static final String P = "p";
    /**
     * Tag for an <code>&lt;pre&gt;</code>
     */
    public static final String PRE = "pre";
    /**
     * Tag for an <code>&lt;select&gt;</code>
     */
    public static final String SELECT = "select";
    /**
     * Tag for an <code>&lt;span&gt;</code>
     */
    public static final String SPAN = "span";
    /**
     * Tag for an <code>&lt;strong&gt;</code>
     */
    public static final String STRONG = "strong";
    /**
     * Tag for an <code>&lt;textarea&gt;</code>
     */
    public static final String TEXTAREA = "textarea";
    /**
     * Tag for an <code>&lt;ul&gt;</code>
     */
    public static final String UL = "ul";

    /**
     * Gets the tag name.
     *
     * @return the tag name
     */
    String value();

}
