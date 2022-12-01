/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * the server side. Attribute values declared on the client side are reflected
 * to the server side as property values or attribute values.
 * <p>
 * You still may use {@link Component}'s or {@link Element}'s mutation methods
 * for the injected element from the server side though. E.g. you may add a
 * child or set attribute/property value. Such children will be available in the
 * element's hierarchy in the same way as for a regular element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Use com.vaadin.flow.component.template.Id instead. Polymer
 *             template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Deprecated
public @interface Id {
    /**
     * The id of the element to map to. When empty, the name of the field is
     * used instead.
     *
     * @return the id of the element to map to
     */
    String value() default "";
}
