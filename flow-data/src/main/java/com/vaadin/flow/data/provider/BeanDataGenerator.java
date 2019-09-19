/*
 * Copyright 2000-2018 Vaadin Ltd.
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
