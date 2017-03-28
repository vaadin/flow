/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be put on {@link com.vaadin.ui.UI}-subclasses that are to be
 * automatically detected and configured by Spring. Use it like this:
 *
 * <pre>
 * &#064;SpringUI
 * public class MyRootUI extends UI {
 *     // ...
 * }
 * </pre>
 *
 * Or like this, if you want to map your UI to another URL (for example if you
 * are having multiple UI subclasses in your application):
 *
 * <pre>
 * &#064;SpringUI(path = &quot;/myPath&quot;)
 * public class MyUI extends UI {
 *     // ...
 * }
 * </pre>
 *
 * The annotated UI will automatically be placed in the {@link UIScope}, so
 * there is no need to add that annotation explicitly.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
@Target({ java.lang.annotation.ElementType.TYPE })
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@SpringComponent
@UIScope
public @interface SpringUI {

    /**
     * The path to which the UI will be bound. For example, a value of
     * {@code "/myUI"} would be mapped to
     * {@code "/myContextPath/myVaadinServletPath/myUI"}. An empty string
     * (default) will map the UI to the root of the servlet. Within a web
     * application, there must not be multiple UI sub classes with the same
     * path.
     */
    String path() default "";

}
