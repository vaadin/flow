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
package com.vaadin.flow.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resolves the page title of a navigation target dynamically through a
 * {@link PageTitleGenerator}, without creating an instance of the navigation
 * target.
 * <p>
 * This is the instance-free counterpart of {@link HasDynamicTitle}: the title
 * is resolved purely from the navigation target class and its route parameters,
 * which makes it usable for routes that are not (and should not be)
 * instantiated, such as the entries of a breadcrumb trail or a menu.
 *
 * <pre>
 * &#64;Route("products/:productId")
 * &#64;DynamicPageTitle(ProductTitleGenerator.class)
 * public class ProductView extends Div {
 *     // ...
 * }
 * </pre>
 *
 * A {@link PageTitle} value may be declared alongside this annotation; it is
 * then handed to the generator through {@link PageTitleContext#value()} (for
 * example as an i18n message key) rather than used as the title directly.
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DynamicPageTitle {

    /**
     * Gets the {@link PageTitleGenerator} type used to resolve the title.
     *
     * @return the generator type, not {@code null}
     */
    Class<? extends PageTitleGenerator> value();
}
