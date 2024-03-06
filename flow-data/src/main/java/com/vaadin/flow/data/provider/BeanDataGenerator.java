/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import com.vaadin.flow.internal.JsonSerializer;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A {@link DataGenerator} that sends all the fields of the objects in the model
 * to the client, using the field names as property names.
 * <p>
 * This class is useful for the cases when the properties in the template of the
 * columns have the same name as the fields of the model object in the server
 * side.
 * <p>
 * Note: this generator sends the entire bean to the client, even if the
 * template in the client doesn't use all the properties.
 * <p>
 * For objects without inner properties, like Strings, a property called
 * {@code value} is created in the model, so it can be accessed via
 * {@code [[item.value]]} in the template.
 *
 * @author Vaadin Ltd.
 * @since 1.2
 *
 * @param <T>
 *            the type of the bean to be serialized to the client
 */
public class BeanDataGenerator<T> implements DataGenerator<T> {

    @Override
    public void generateData(T item, JsonObject data) {
        JsonValue value = JsonSerializer.toJson(item);
        if (value instanceof JsonObject) {
            JsonObject object = (JsonObject) value;
            for (String key : object.keys()) {
                data.put(key, (JsonValue) object.get(key));
            }
        } else {
            data.put("value", value);
        }
    }

}
