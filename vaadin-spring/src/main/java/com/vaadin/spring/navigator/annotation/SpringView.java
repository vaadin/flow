/*
 * Copyright 2015 The original authors
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
package com.vaadin.spring.navigator.annotation;

import com.vaadin.spring.annotation.VaadinComponent;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.UI;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be placed on {@link com.vaadin.navigator.View}-classes that
 * should be handled by the {@link SpringViewProvider}.
 * <p>
 * This annotation is also a stereotype annotation, so Spring will automatically
 * detect the annotated classes. <b>However, the scope must be explicitly
 * specified as the default singleton scope will not work!</b> You can use the
 * {@code prototype} scope or the
 * {@link com.vaadin.spring.annotation.VaadinUIScope ui} scope.
 * <p>
 * This is an example of a view that is mapped to an empty view name and is
 * available for all UI subclasses in the application:
 * 
 * <pre>
 * &#064;SpringView(name = &quot;&quot;)
 * &#064;UIScope
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
 * &#064;UIScope
 * public class MyView extends CustomComponent implements View {
 *     // ...
 * }
 * </pre>
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
@Target({ java.lang.annotation.ElementType.TYPE })
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
@VaadinComponent
public @interface SpringView {

    /**
     * The name of the view. This is the name that is to be passed to the
     * {@link com.vaadin.navigator.Navigator} when navigating to the view. There
     * can be multiple views with the same name as long as they belong to
     * separate UI subclasses.
     *
     * @see #ui()
     */
    String name();

    /**
     * By default, the view will be available for all UI subclasses in the
     * application. This attribute can be used to explicitly specify which
     * subclass (or subclasses) that the view belongs to.
     */
    Class<? extends UI>[] ui() default {};
}
