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
package com.vaadin.flow.component.polymertemplate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Defines the id of an element to map to inside a {@link PolymerTemplate}.
 * <p>
 * Use this annotation with an identifier of the element which you want to refer
 * to as a value for a field inside your {@link PolymerTemplate} class.
 * <p>
 * Here is a Java sample:
 *
 * <pre>
 * <code>
 * &#64;Tag("details")
 * public class Details extends PolymerTemplate&lt;EditorModel&gt;{
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
 * &lt;dom-module id="details"&gt;
 *   &lt;template&gt;
 *     &lt;div id='name'&gt;
 *      &lt;label&gt;Text&lt;/label&gt;
 *     &lt;/div&gt;
 *     &lt;input type="text" id='email'&gt;&lt;/div"&gt;
 *   &lt;/template&gt;
 *   ....
 * </code>
 * </pre>
 * <p>
 * It's important to understand that the element's hierarchical structure for
 * the element injected via <code>@Id</code> is not populated and not available
 * on the server side (it's not known). It means that <code>nestedDiv</code>
 * field value which is a <code>Div</code> component doesn't have any child on
 * the server side. Also attribute values declared on the client side are not
 * available on the server side.
 * <p>
 * You still may use {@link Component}'s or {@link Element}'s mutation methods
 * for the injected element from the server side though. E.g. you may add a
 * child or set attribute/property value. Such children will be available in the
 * element's hierarchy in the same way as for a regular element.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
