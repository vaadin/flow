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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.internal.BundleUtils;

/**
 * A reference to the values that a class imports from JavaScript modules, to be
 * passed to {@link Element#executeJs(String, Object...)} as a parameter.
 * <p>
 * A JavaScript expression sent from the server is evaluated in the global scope
 * and can therefore not use {@code import} to reach values exported by a
 * JavaScript module. This type bridges that gap: the imports are declared in
 * Java with {@link JsModule#imports()} or {@link JsModule#importAll()}, and the
 * expression receives them as an ordinary parameter.
 * <p>
 * Declare the imports on a class - a class whose only purpose is to declare
 * them is usually clearest:
 *
 * <pre>
 * &#64;JsModule(value = "lit-html", imports = { "render", "html" })
 * public final class LitImports {
 * }
 * </pre>
 *
 * and pass a reference to that class when running the expression:
 *
 * <pre>
 * element.executeJs("$0.render($0.html`&lt;div&gt;${$1}&lt;/div&gt;`, this)",
 *         JsImports.of(LitImports.class), "Hello");
 * </pre>
 *
 * The parameter arrives as an object with one property per imported name, or as
 * the module's namespace object when {@link JsModule#importAll()} is used. If a
 * more JavaScript-like spelling is preferred, destructure it first:
 *
 * <pre>
 * element.executeJs(
 *         "const { render, html } = $0; render(html`&lt;div&gt;${$1}&lt;/div&gt;`, this)",
 *         JsImports.of(LitImports.class), "Hello");
 * </pre>
 *
 * Because each reference is its own parameter, two modules that export the same
 * name do not collide - declare them on separate classes and pass both, using
 * {@code $0.foo} and {@code $1.foo}.
 * <p>
 * The declared modules are not part of the eager bundle. The chunk holding them
 * is requested before the expression that needs it is run, so the values are
 * always available by the time the expression is evaluated.
 *
 * @author Vaadin Ltd
 * @see JsModule#imports()
 * @see Element#executeJs(String, Object...)
 */
public final class JsImports implements Serializable {

    private final String declaringClassName;

    private JsImports(String declaringClassName) {
        this.declaringClassName = declaringClassName;
    }

    /**
     * Creates a reference to the values imported by the given class.
     *
     * @param declaringClass
     *            the class declaring the imports with
     *            {@link JsModule#imports()} or {@link JsModule#importAll()},
     *            not {@code null}
     * @return a reference to the imported values
     * @throws IllegalArgumentException
     *             if the class does not declare any JS module imports
     */
    public static JsImports of(Class<?> declaringClass) {
        Objects.requireNonNull(declaringClass, "declaringClass");
        boolean declaresImports = false;
        for (JsModule jsModule : declaringClass
                .getAnnotationsByType(JsModule.class)) {
            if (jsModule.importAll() || jsModule.imports().length > 0) {
                declaresImports = true;
                break;
            }
        }
        if (!declaresImports) {
            throw new IllegalArgumentException(declaringClass.getName()
                    + " does not declare any JavaScript module imports. Add a @JsModule annotation that sets 'imports' or 'importAll' to the class, e.g. @JsModule(value = \"lit-html\", imports = {\"render\", \"html\"}).");
        }
        return new JsImports(declaringClass.getName());
    }

    /**
     * Gets the name of the class declaring the imports.
     *
     * @return the declaring class name
     */
    public String getDeclaringClassName() {
        return declaringClassName;
    }

    /**
     * Gets the id of the bundle chunk that publishes these imports. This is the
     * key the imports are registered under on the client, and the chunk that
     * has to be loaded before they can be used.
     * <p>
     * For internal use only.
     *
     * @return the chunk id
     */
    public String getChunkId() {
        return BundleUtils.getChunkId(declaringClassName);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JsImports other
                && declaringClassName.equals(other.declaringClassName);
    }

    @Override
    public int hashCode() {
        return declaringClassName.hashCode();
    }

    @Override
    public String toString() {
        return "JsImports[" + declaringClassName + "]";
    }
}
