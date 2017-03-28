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

import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.UI;

/**
 * Annotation to be placed on {@link com.vaadin.navigator.View}-classes that
 * should be handled by the {@link SpringViewProvider}.
 * <p>
 * This annotation is also a stereotype annotation, so Spring will automatically
 * detect the annotated classes. By default, this annotation also puts the view
 * into the {@link com.vaadin.spring.annotation.ViewScope view scope}. You can
 * override this by using another scope annotation, such as
 * {@link com.vaadin.spring.annotation.UIScope the UI scope}, on your view
 * class. <b>However, the singleton scope will not work!</b>
 * <p>
 * This is an example of a view that is mapped to an empty view name and is
 * available for all UI subclasses in the application:
 *
 * <pre>
 * &#064;SpringView(name = &quot;&quot;)
 * public class MyDefaultView extends CustomComponent implements View {
 *     // ...
 * }
 * </pre>
 *
 * This is an example of a view that is only available to a specified UI
 * subclass:
 *
 * <pre>
 * &#064;SpringView(name = &quot;myView&quot;, ui = MyUI.class)
 * public class MyView extends CustomComponent implements View {
 *     // ...
 * }
 * </pre>
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 */
@Target({ java.lang.annotation.ElementType.TYPE })
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@SpringComponent
@ViewScope
public @interface SpringView {

    /**
     * The name of the view. This is the name that is to be passed to the
     * {@link com.vaadin.navigator.Navigator} when navigating to the view. There
     * can be multiple views with the same name as long as they belong to
     * separate UI subclasses.
     *
     * If the default value {@link #USE_CONVENTIONS} is used, the name of the
     * view is derived from the class name so that e.g. UserDetailView becomes
     * "user-detail". Although auto-generated view names are supported, using
     * explicit naming of views is strongly recommended.
     *
     * @see #ui()
     */
    String name() default USE_CONVENTIONS;

    /**
     * USE_CONVENTIONS is treated as a special case that will cause the
     * automatic View mapping to occur.
     */
    public static final String USE_CONVENTIONS = "USE CONVENTIONS";

    /**
     * By default, the view will be available for all UI subclasses in the
     * application. This attribute can be used to explicitly specify which
     * subclass (or subclasses) that the view belongs to.
     */
    Class<? extends UI>[] ui() default {};
}
