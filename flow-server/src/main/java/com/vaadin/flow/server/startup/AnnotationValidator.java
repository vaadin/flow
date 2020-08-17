/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Validation class that is run during servlet container initialization which
 * checks that specific annotations are not configured wrong.
 *
 * @since 1.0
 */
@HandlesTypes({ Viewport.class, BodySize.class, Inline.class })
public class AnnotationValidator extends AbstractAnnotationValidator
        implements ClassLoaderAwareServletContainerInitializer, VaadinContextInitializer {

    /**
     *
     * @param set
     *            the Set of application classes that extend, implement, or have
     *            been annotated with the class types specified by the
     *            {@link HandlesTypes}
     *            annotation, or <tt>null</tt> if there are no matches, or this
     *            <tt>ServletContainerInitializer</tt> has not been annotated
     *            with {@link HandlesTypes}
     *
     * @param ctx
     *            the {@link ServletContext} of the web application that is
     *            being started and in which the classes contained in <tt>set</tt>
     *            were found
     *
     * @deprecated Use {@link #process(Set, VaadinContext)} instead
     *             by wrapping {@link ServletContext} with {@link VaadinServletContext}.
     *
     * @throws ServletException
     */
    @Override
    @Deprecated
    public void process(Set<Class<?>> set, ServletContext ctx) throws ServletException {
        process(set, new VaadinServletContext(ctx));
    }

    @Override
    public void process(Set<Class<?>> classSet, VaadinContext vaadinContext) {
        validateClasses(classSet);
    }

    @Override
    public List<Class<?>> getAnnotations() {
        return Arrays.asList(
                this.getClass().getAnnotation(HandlesTypes.class).value());
    }

}
