/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.data.converter;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.flow.data.binder.ErrorMessageProvider;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Default implementation of {@link ConverterFactory}, handling all standard
 * converters defined in {@code com.vaadin.flow.data.converters} package.
 *
 * @author Vaadin Ltd
 * @since
 */
public enum DefaultConverterFactory implements ConverterFactory {

    INSTANCE;

    @SuppressWarnings({ "rawtypes", "ImmutableEnumChecker" })
    private final Map<Key, SerializableSupplier<? extends Converter>> converterMap = new HashMap<>();

    DefaultConverterFactory() {
        registerConverter(DateToLongConverter.class, DateToLongConverter::new);
        registerConverter(DateToSqlDateConverter.class,
                DateToSqlDateConverter::new);
        registerConverter(LocalDateTimeToInstantConverter.class,
                () -> new LocalDateTimeToInstantConverter(
                        ZoneId.systemDefault()));
        registerConverter(LocalDateTimeToDateConverter.class,
                () -> new LocalDateTimeToDateConverter(ZoneId.systemDefault()));
        registerConverter(LocalDateToDateConverter.class,
                LocalDateToDateConverter::new);
        registerConverterWithMessageProvider(StringToBigDecimalConverter.class,
                StringToBigDecimalConverter::new);
        registerConverterWithMessageProvider(StringToBigIntegerConverter.class,
                StringToBigIntegerConverter::new);
        registerConverterWithMessageProvider(StringToBooleanConverter.class,
                StringToBooleanConverter::new);
        registerConverter(StringToDateConverter.class,
                StringToDateConverter::new);
        registerConverterWithMessageProvider(StringToDoubleConverter.class,
                StringToDoubleConverter::new);
        registerConverterWithMessageProvider(StringToFloatConverter.class,
                StringToFloatConverter::new);
        registerConverterWithMessageProvider(StringToIntegerConverter.class,
                StringToIntegerConverter::new);
        registerConverterWithMessageProvider(StringToLongConverter.class,
                StringToLongConverter::new);
        registerConverterWithMessageProvider(StringToUuidConverter.class,
                StringToUuidConverter::new);
        registerConverter(BigDecimalToDoubleConverter.class,
                BigDecimalToDoubleConverter::new);
        registerConverter(BigDecimalToIntegerConverter.class,
                BigDecimalToIntegerConverter::new);
        registerConverter(DoubleToBigDecimalConverter.class,
                DoubleToBigDecimalConverter::new);
        registerConverter(IntegerToBigDecimalConverter.class,
                IntegerToBigDecimalConverter::new);
        registerConverter(IntegerToDoubleConverter.class,
                IntegerToDoubleConverter::new);
        registerConverter(IntegerToLongConverter.class,
                IntegerToLongConverter::new);
        registerConverter(BigDecimalToFloatConverter.class,
                BigDecimalToFloatConverter::new);
        registerConverter(BigDecimalToLongConverter.class,
                BigDecimalToLongConverter::new);
        registerConverter(LongToBigDecimalConverter.class,
                LongToBigDecimalConverter::new);
        registerConverter(FloatToBigDecimalConverter.class,
                FloatToBigDecimalConverter::new);
        registerConverter(FloatToDoubleConverter.class,
                FloatToDoubleConverter::new);
    }

    private <C extends Converter<?, ?>> void registerConverter(
            Class<C> converterType, SerializableSupplier<C> factory) {
        List<Class<?>> types = ReflectTools
                .getGenericInterfaceTypes(converterType, Converter.class);
        assert !types.isEmpty() && types.stream().allMatch(Objects::nonNull);
        Key key = new Key(types.get(0), types.get(1));
        converterMap.put(key, factory);
    }

    private <C extends Converter<?, ?>> void registerConverterWithMessageProvider(
            Class<C> converterType, Function<ErrorMessageProvider, C> factory) {
        registerConverter(converterType, () -> factory.apply(context -> ""));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P, M> Optional<Converter<P, M>> newInstance(
            Class<P> presentationType, Class<M> modelType) {
        if (presentationType == null) {
            throw new IllegalArgumentException(
                    "The presentation type cannot be null");
        }
        if (modelType == null) {
            throw new IllegalArgumentException(
                    "The model type must cannot be null");
        }
        return Optional
                .ofNullable(
                        converterMap.get(new Key(presentationType, modelType)))
                .map(Supplier::get);
    }

    private static final class Key implements Serializable {
        private final Class<?> presentationType;
        private final Class<?> modelType;

        private Key(Class<?> presentationType, Class<?> modelType) {
            assert presentationType != null && modelType != null;
            this.presentationType = ReflectTools
                    .convertPrimitiveType(presentationType);
            this.modelType = ReflectTools.convertPrimitiveType(modelType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Key key = (Key) o;
            return presentationType.equals(key.presentationType)
                    && modelType.equals(key.modelType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(presentationType, modelType);
        }
    }

}
