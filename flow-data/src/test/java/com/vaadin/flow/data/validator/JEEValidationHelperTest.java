package com.vaadin.flow.data.validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.tests.data.bean.BeanToValidate;
import com.vaadin.flow.tests.data.bean.NotEmptyValue;

public class JEEValidationHelperTest {

    @Test
    public void isNotNullConstraint_worksWithBothValidationApi() {
        for (Annotation annotation : getField(
                com.vaadin.flow.tests.data.bean.jakarta.BeanToValidate.class,
                "firstname").getAnnotations()) {
            boolean isNotNull = JEEValidationHelper.JAKARTA_BEAN_VALIDATION_AVAILABLE
                    && annotation.annotationType().getName()
                            .endsWith(".validation.constraints.NotNull");
            Assert.assertEquals("jakarta NotNull annotation: " + annotation,
                    isNotNull,
                    JEEValidationHelper.isNotNullConstraint(annotation));
        }
        for (Annotation annotation : getField(BeanToValidate.class, "firstname")
                .getAnnotations()) {
            boolean isNotNull = JEEValidationHelper.JAVAX_BEAN_VALIDATION_AVAILABLE
                    && annotation.annotationType().getName()
                            .endsWith(".validation.constraints.NotNull");
            Assert.assertEquals("javax NotNull annotation " + annotation,
                    isNotNull,
                    JEEValidationHelper.isNotNullConstraint(annotation));
        }
    }

    @Test
    public void isNotEmptyConstraint_worksWithBothValidationApi() {
        for (Annotation annotation : getField(
                com.vaadin.flow.tests.data.bean.jakarta.NotEmptyValue.class,
                "value").getAnnotations()) {
            boolean isNotEmpty = JEEValidationHelper.JAKARTA_BEAN_VALIDATION_AVAILABLE
                    && annotation.annotationType().getName()
                            .endsWith(".validation.constraints.NotEmpty");
            Assert.assertEquals("jakarta NotEmpty annotation: " + annotation,
                    isNotEmpty,
                    JEEValidationHelper.isNotEmptyConstraint(annotation));
        }
        for (Annotation annotation : getField(NotEmptyValue.class, "value")
                .getAnnotations()) {
            boolean isNotEmpty = JEEValidationHelper.JAVAX_BEAN_VALIDATION_AVAILABLE
                    && annotation.annotationType().getName()
                            .endsWith(".validation.constraints.NotEmpty");
            Assert.assertEquals("javax NotEmpty annotation " + annotation,
                    isNotEmpty,
                    JEEValidationHelper.isNotEmptyConstraint(annotation));
        }
    }

    @Test
    public void isSizeConstraint_worksWithBothValidationApi() {
        for (Annotation annotation : getField(
                com.vaadin.flow.tests.data.bean.jakarta.BeanToValidate.class,
                "age").getAnnotations()) {
            boolean isSize = JEEValidationHelper.JAKARTA_BEAN_VALIDATION_AVAILABLE
                    && annotation.annotationType().getName()
                            .endsWith(".validation.constraints.Size");
            Assert.assertEquals("jakarta Size annotation: " + annotation,
                    isSize, JEEValidationHelper.isSizeConstraint(annotation));
        }
        for (Annotation annotation : getField(BeanToValidate.class, "age")
                .getAnnotations()) {
            boolean isSize = JEEValidationHelper.JAVAX_BEAN_VALIDATION_AVAILABLE
                    && annotation.annotationType().getName()
                            .endsWith(".validation.constraints.Size");
            Assert.assertEquals("javax Size annotation " + annotation, isSize,
                    JEEValidationHelper.isSizeConstraint(annotation));
        }
    }

    protected Field getField(Class<?> annotatedBean, String fieldName) {
        try {
            return annotatedBean.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field " + fieldName
                    + " not found in type " + annotatedBean);
        }
    }

}
