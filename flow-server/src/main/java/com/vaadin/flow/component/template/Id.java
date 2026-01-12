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
package com.vaadin.flow.component.template;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Defines the id of a component or an element to map to inside a lit template.
 * <p>
 * Use this annotation with an identifier of the element which you want to refer
 * to as a value for a field inside your {@code LitTemplate} class.
 * <p>
 * Here is a Java sample:
 *
 * <pre>
 * <code>
 * &#64;Tag("details")
 * public class Details extends LitTemplate {
 *
 *      &#64;Id("name")
 *      private Div nestedDiv;
 *
 *      &#64;Id("email")
 *      private Element nestedElement;
 * }
 * </code>
 * </pre>
 *
 * This code may be used with the following template:
 *
 * <pre>
 * <code>
 *    render(){
 *     return html`
 *     &lt;div id='name'&gt;
 *      &lt;label&gt;Text&lt;/label&gt;
 *     &lt;/div&gt;
 *     &lt;input type="text" id='email'&gt;&lt;/div"&gt;
 *     `;
 *   ....
 * </code>
 * </pre>
 * <p>
 * It's important to understand that the element's hierarchical structure for
 * the element injected via <code>@Id</code> is not populated and not available
 * on the server side (it's not known). It means that <code>nestedDiv</code>
 * field value which is a <code>Div</code> component doesn't have any child on
 * the server side. Attribute values declared on the client side are reflected
 * to the server side as property values or attribute values.
 * <p>
 * You still may use {@link Component}'s or {@link Element}'s mutation methods
 * for the injected element from the server side though. E.g. you may add a
 * child or set attribute/property value. Such children will be available in the
 * element's hierarchy in the same way as for a regular element.
 * <p>
 * An attribute/property value set using server side API methods overrides the
 * values used in the template. If the attribute/property value is not
 * explicitly set on the server side then the template value is used. In this
 * example:
 *
 * <pre>
 * <code>
 * &#64;Tag("my-layout")
 * public class Layout extends LitTemplate {
 *
 *      &#64;Id("container")
 *      private MyComponent container;
 * }
 *
 * &#64;Tag("my-component")
 * public class MyComponent extends LitTemplate {
 *
 *      public MyComponent(){
 *          getElement().setProperty("name","Joe");
 *      }
 * }
 *
 * </code>
 * </pre>
 *
 * the {@code container} field has {@code "name"} property value {@code "Joe"}
 * (even though it has another value declared in the template) and it has
 * {@code "version"} value {@code "1.0"} with the following template:
 *
 * <pre>
 * <code>
 *    render(){
 *     return html`
 *      &lt;my-component id='container' name='Doe' version='1.0' &gt;&lt;/my-component&gt;
 *     `;
 * </code>
 * </pre>
 *
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Id {
    /**
     * The id of the element to map to. When empty, the name of the field is
     * used instead.
     *
     * @return the id of the element to map to
     */
    String value() default "";
}
