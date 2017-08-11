package com.vaadin.flow.template.model;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.util.ReflectTools;

/**
 * @author Vaadin Ltd.
 */
class PropertyMapBuilder {
    private final Map<String, ModelType> properties = new HashMap<>();
    private final PropertyFilter propertyFilter;
    private final ModelConverterProvider converterProvider;

    PropertyMapBuilder(Class<?> javaType, PropertyFilter propertyFilter,
            ModelConverterProvider converterProvider) {
        assert javaType != null;
        assert propertyFilter != null;

        this.propertyFilter = propertyFilter;
        this.converterProvider = converterProvider;
        ReflectTools.getSetterMethods(javaType).forEach(this::addProperty);
        ReflectTools.getGetterMethods(javaType).forEach(this::addProperty);
    }

    Map<String, ModelType> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    private void addProperty(Method method) {
        String propertyName = ReflectTools.getPropertyName(method);
        if (properties.containsKey(propertyName)) {
            return;
        }

        if (!propertyFilter.test(propertyName)) {
            return;
        }

        PropertyFilter innerFilter = new PropertyFilter(propertyFilter,
                propertyName,
                TemplateModelUtil.getFilterFromIncludeExclude(method));

        ModelConverterProvider newConverterProvider = new ModelConverterProvider(
                converterProvider, TemplateModelUtil.getModelConverters(method),
                innerFilter);

        ModelType propertyType;
        if (newConverterProvider.apply(innerFilter).isPresent()) {
            propertyType = BeanModelType.getConvertedModelType(
                    ReflectTools.getPropertyType(method), innerFilter,
                    propertyName, method.getDeclaringClass(),
                    newConverterProvider);
        } else {
            propertyType = BeanModelType.getModelType(
                    ReflectTools.getPropertyType(method), innerFilter,
                    propertyName, method.getDeclaringClass(),
                    newConverterProvider);
        }

        properties.put(propertyName, propertyType);
    }
}
