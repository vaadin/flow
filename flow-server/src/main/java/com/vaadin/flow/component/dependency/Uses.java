/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.dependency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Tag;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks that the annotated class implicitly uses another class, so that what
 * the used class needs on the client is available whenever the annotated class
 * is used.
 * <p>
 * Marking class A with <code>@Uses(B.class)</code> makes the production build
 * treat <code>B</code> as used whenever <code>A</code> is, even if no code of
 * <code>A</code> refers to <code>B</code> directly, for example because
 * <code>B</code> is only reached through reflection. All {@link StyleSheet},
 * {@link JavaScript} and other dependencies of <code>B</code> are also loaded
 * when a component <code>A</code> is used, the same way as those of an
 * interface that <code>A</code> implements. The used class does not have to be
 * a component: it can be an interface, for example a
 * {@link com.vaadin.flow.js.JsDefinition} that <code>A</code> runs JavaScript
 * of. A used class that is neither a component nor an interface is only treated
 * as used by the build, which bundles its {@link JsModule}, {@link CssImport}
 * and bundled {@link JavaScript} values. What is loaded at runtime, such as a
 * {@link StyleSheet}, is not loaded for such a class. The classes that it names
 * in its own <code>@Uses</code> are handled as usual.
 * <p>
 * For {@code PolymerTemplate} implementations, used components will also be
 * instantiated if an element with the corresponding {@link Tag @Tag} value is
 * defined in the template. Note that Polymer template support is deprecated -
 * we recommend you to use {@code LitTemplate} instead. Read more details from
 * <a href= "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 * Vaadin blog.</a>
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(Uses.Container.class)
public @interface Uses {

    /**
     * Marks the class to depend on.
     *
     * @return the class to depend on
     */
    Class<?> value();

    /**
     * Internal annotation to enable use of multiple {@link Uses} annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link Uses} annotations.
         *
         * @return an array of the uses annotations
         */
        Uses[] value();
    }
}
