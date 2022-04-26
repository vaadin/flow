/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.data.validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

import org.slf4j.LoggerFactory;

public class JEEValidationHelper {

    public static final boolean JAVAX_BEAN_VALIDATION_AVAILABLE = isAvailable(
            "javax", "BeanValidator");
    public static final boolean JAKARTA_BEAN_VALIDATION_AVAILABLE = isAvailable(
            "jakarta", "JakartaBeanValidator");

    public static boolean checkBeanValidationAvailable() {
        return JAVAX_BEAN_VALIDATION_AVAILABLE
                || JAKARTA_BEAN_VALIDATION_AVAILABLE;
    }

    private JEEValidationHelper() {
    }

    private static boolean isAvailable(String packagePrefix,
            String beanValidatorName) {
        try {
            Class<?> clazz = Class
                    .forName(packagePrefix + ".validation.Validation");
            Method method = clazz.getMethod("buildDefaultValidatorFactory");
            method.invoke(null);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException
                | InvocationTargetException e) {
            LoggerFactory.getLogger("com.vaadin.validator.BeanValidator").info(
                    "A JSR-303 bean validation implementation not found on the classpath or could not be initialized. {} cannot be used.",
                    beanValidatorName, e);
            return false;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Unable to invoke " + packagePrefix
                    + ".validation.Validation.buildDefaultValidatorFactory()",
                    e);
        }
    }

    public static BiFunction<Class<?>, String, AbstractBeanValidator<?>> beanValidatorFactory() {
        // TODO: should fail if we implementations for both javax and jakarta?
        if (JAVAX_BEAN_VALIDATION_AVAILABLE) {
            return BeanValidator::new;
        } else if (JEEValidationHelper.JAKARTA_BEAN_VALIDATION_AVAILABLE) {
            return JakartaBeanValidator::new;
        }
        throw new IllegalStateException("Cannot create a BeanValidator "
                + ": a JSR-303 Bean Validation implementation not found on the classpath");
    }

    public static boolean isNotNullConstraint(Annotation annotation) {
        return checkAnnotation(annotation, "NotNull");
    }

    public static boolean isNotEmptyConstraint(Annotation annotation) {
        return checkAnnotation(annotation, "NotEmpty");
    }

    public static boolean isSizeConstraint(Annotation annotation) {
        return checkAnnotation(annotation, "Size");
    }

    private static boolean checkAnnotation(Annotation annotation,
            String annotationSimpleName) {
        String annotationName = annotation.annotationType().getName();
        return annotationName
                .endsWith(".validation.constraints." + annotationSimpleName)
                && ((JAVAX_BEAN_VALIDATION_AVAILABLE
                        && annotationName.startsWith("javax."))
                        || (JAKARTA_BEAN_VALIDATION_AVAILABLE
                                && annotationName.startsWith("jakarta.")));
    }

}
