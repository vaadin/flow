/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import com.vaadin.util.ReflectTools;

/**
 * Utility class for mapping Bean values to {@link TemplateModel} values.
 *
 * @author Vaadin Ltd
 */
public class TemplateModelBeanUtil {

    private TemplateModelBeanUtil() {
        // NOOP
    }

    static void importBeanIntoModel(TemplateModelProxyHandler invocationHandler,
            Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("Bean cannot be null");
        }
        Stream.of(bean.getClass().getMethods()).filter(ReflectTools::isGetter)
                .filter(TemplateModelBeanUtil::isNotGetClass)
                .forEach(method -> mapBeanToModel(method, invocationHandler,
                        bean));
    }

    private static void mapBeanToModel(Method beanGetter,
            TemplateModelProxyHandler invocationHandler, Object bean) {
        try {
            Object value = beanGetter.invoke(bean, (Object[]) null);

            invocationHandler.setModelValue(
                    ReflectTools.getPropertyName(beanGetter),
                    beanGetter.getGenericReturnType(), value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Given method was not accesible "
                    + bean.getClass().getName() + "::" + beanGetter.getName(),
                    e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "Exception thrown while reading bean value from "
                            + bean.getClass().getName() + "::"
                            + beanGetter.getName()
                            + ", getters should not throw exceptions.",
                    e);
        }
    }

    private static boolean isNotGetClass(Method method) {
        return !"getClass".equals(method.getName());
    }

}
