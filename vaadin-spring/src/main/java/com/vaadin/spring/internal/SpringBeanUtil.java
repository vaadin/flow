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
package com.vaadin.spring.internal;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;

public class SpringBeanUtil {

    private SpringBeanUtil() {
        // Util methods only
    }

    /**
     * Creates a managed bean of the given type (not a subtype) if such a bean
     * has been defined.
     *
     * @param <T>
     *            the type of bean
     * @param cls
     *            the type of bean to create
     * @return a bean of the given type or <code>null</code> if not bean of the
     *         given type has been defined
     */
    public static <T> T createManagedBeanIfAvailable(
            ApplicationContext applicationContext, Class<T> cls) {
        // Find out whether the target class is a Spring bean or not
        String[] beanNames = BeanFactoryUtils
                .beanNamesForTypeIncludingAncestors(applicationContext, cls);
        for (String beanName : beanNames) {
            if (applicationContext.getAutowireCapableBeanFactory()
                    .getType(beanName) == cls) {
                return applicationContext.getBean(cls);
            }
        }

        return null;
    }

}
