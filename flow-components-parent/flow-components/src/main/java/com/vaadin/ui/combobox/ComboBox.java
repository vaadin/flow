/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.combobox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.data.HasDataProvider;
import com.vaadin.data.HasItems;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.KeyMapper;
import com.vaadin.data.provider.Query;
import com.vaadin.flow.dom.Element;
import com.vaadin.shared.Registration;
import com.vaadin.ui.common.HasSize;
import com.vaadin.ui.common.HasValidation;
import com.vaadin.ui.common.HasValue;
import com.vaadin.ui.common.ItemLabelGenerator;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Server-side component for the {@code vaadin-combo-box} webcomponent. It
 * contains the same features of the webcomponent, such as item filtering,
 * object selection and item templating.
 *
 * @param <T>
 *            the type of the items to be inserted in the combo box
 */

public class ComboBox<T> extends GeneratedVaadinComboBox<ComboBox<T>>
        implements HasSize, HasItems<T>, HasValidation,
        HasValue<ComboBox<T>, T>, HasDataProvider<T> {
    private static final String ITEM_LABEL_PROPERTY = "label";
    private static final String KEY_PROPERTY = "key";
    private static final String SELECTED_ITEM_PROPERTY_NAME = "selectedItem";
    private static final String TEMPLATE_TAG_NAME = "template";

    private T oldValue;
    private ItemLabelGenerator<T> itemLabelGenerator = String::valueOf;

    private DataProvider<T, ?> dataProvider = DataProvider.ofItems();

    private final KeyMapper<T> keyMapper = new KeyMapper<>();

    /**
     * Default constructor. Creates an empty combo box.
     *
     */
    public ComboBox() {
        getElement().synchronizeProperty(SELECTED_ITEM_PROPERTY_NAME,
                "selected-item-changed");
        getElement().synchronizeProperty(SELECTED_ITEM_PROPERTY_NAME, "change");

        getElement().addEventListener("selected-item-changed", event -> {
            fireEvent(new ValueChangeEvent<>(this, this, oldValue, true));
            oldValue = getValue();
        });

        setItemLabelPath(ITEM_LABEL_PROPERTY);
        setItemValuePath(ITEM_LABEL_PROPERTY);
    }

    /**
     * Creates an empty combo box with the defined label.
     *
     * @param label
     *            the label describing the combo box
     */
    public ComboBox(String label) {
        this();
        setLabel(label);
    }

    /**
     * Creates a combo box with the defined label and populated with the items
     * in the collection.
     *
     * @param label
     *            the label describing the combo box
     * @param items
     *            the items to be shown in the list of the combo box
     * @see #setItems(Collection)
     */
    public ComboBox(String label, Collection<T> items) {
        this();
        setLabel(label);
        setItems(items);
    }

    /**
     * Creates a combo box with the defined label and populated with the items
     * in the array.
     *
     * @param label
     *            the label describing the combo box
     * @param items
     *            the items to be shown in the list of the combo box
     * @see #setItems(Object...)
     */
    @SafeVarargs
    public ComboBox(String label, T... items) {
        this();
        setLabel(label);
        setItems(items);
    }

    /**
     * Sets the template of the items inside the list of the combo box. The
     * template defines how the item is rendered, and it is based on the
     * properties of the item.
     * <p>
     * For example, if you have an object with the properties "name" and "id",
     * you can create a template like this: <blockquote>
     *
     * <pre>
     * comboBox.setItemTemplate("Name: [[item.name]]<br>Id: [[item.id]]");
     * </pre>
     *
     * </blockquote>
     * <p>
     * You can access any property of the object using the notation
     * <code>[[item.property]]</code>.
     * <p>
     * Note: the webcomponent currently doesn't support changing the item
     * template after the component is first populated. This method should be
     * called before setting any items.
     *
     * @param template
     *            the template to be used to render the items inside the list of
     *            this combo box
     */
    private void setItemTemplate(String template) {
        getElement().getChildren()
                .filter(child -> TEMPLATE_TAG_NAME.equals(child.getTag()))
                .findFirst().ifPresent(element -> element.removeFromParent());

        Element templateElement = new Element(TEMPLATE_TAG_NAME);
        getElement().appendChild(templateElement);
        templateElement.setProperty("innerHTML", template);
    }

    /**
     * Gets the current item template associated with this combo box.
     *
     * @return the current item template, or <code>null</code> if it wasn't set
     */
    private String getItemTemplate() {
        Optional<Element> optionalTemplate = getElement().getChildren()
                .filter(child -> TEMPLATE_TAG_NAME.equals(child.getTag()))
                .findFirst();

        if (optionalTemplate.isPresent()) {
            return optionalTemplate.get().getProperty("innerHTML");
        }
        return null;
    }

    @Override
    public void setDataProvider(DataProvider<T, ?> dataProvider) {
        Objects.requireNonNull(dataProvider);
        this.dataProvider = dataProvider;
        refresh();
    }

    public DataProvider<T, ?> getDataProvider() {
        return dataProvider;
    }

    /**
     * Gets the list of items which were filtered by the user input.
     *
     * @return the list of filtered items, or empty list if none were filtered
     */
    public List<T> getFilteredItems() {
        JsonArray items = protectedGetFilteredItems();
        List<T> result = new ArrayList<>(items.length());
        for (int i = 0; i < items.length(); i++) {
            result.add(getData(items.get(i)));
        }
        return result;
    }

    /**
     * Convenience method for the {@link #setFilteredItems(Collection)}. It sets
     * the list of visible items in reaction of the input of the user.
     *
     * @param filteredItems
     *            the items to show in response of a filter input
     */
    public void setFilteredItems(T... filteredItems) {
        setFilteredItems(Arrays.asList(filteredItems));
    }

    /**
     * It sets the list of visible items in reaction of the input of the user.
     *
     * @param filteredItems
     *            the items to show in response of a filter input
     */
    public void setFilteredItems(Collection<T> filteredItems) {
        setFilteredItems(generateJson(filteredItems.stream()));
    }

    /**
     * Sets the item label generator that is used to produce the strings shown
     * in the combo box for each item. By default,
     * {@link String#valueOf(Object)} is used.
     *
     * @param itemLabelGenerator
     *            the item label provider to use, not null
     */
    public void setItemLabelGenerator(
            ItemLabelGenerator<T> itemLabelGenerator) {
        Objects.requireNonNull(itemLabelGenerator,
                "Item label generators must not be null");
        this.itemLabelGenerator = itemLabelGenerator;
        refresh();
    }

    /**
     * Gets the item label generator that is used to produce the strings shown
     * in the combo box for each item.
     *
     * @return the item label generator used, not null
     */
    public ItemLabelGenerator<T> getItemLabelGenerator() {
        return itemLabelGenerator;
    }

    @Override
    public T getEmptyValue() {
        return null;
    }

    @Override
    public void setValue(T value) {
        getElement().setPropertyJson(SELECTED_ITEM_PROPERTY_NAME,
                generateJson(value));
    }

    @Override
    public T getValue() {
        Serializable property = getElement()
                .getPropertyRaw(SELECTED_ITEM_PROPERTY_NAME);
        if (property instanceof JsonObject) {
            JsonObject selected = (JsonObject) property;
            assert selected.hasKey(KEY_PROPERTY);
            return keyMapper.get(selected.getString(KEY_PROPERTY));
        }
        return getEmptyValue();
    }

    @Override
    public Registration addValueChangeListener(
            ValueChangeListener<ComboBox<T>, T> listener) {

        return addListener(ValueChangeEvent.class,
                (ValueChangeListener) listener);
    }

    private JsonArray generateJson(Stream<T> data) {
        JsonArray array = Json.createArray();
        data.map(this::generateJson)
                .forEachOrdered(json -> array.set(array.length(), json));
        return array;
    }

    private JsonValue generateJson(T item) {
        JsonObject json = Json.createObject();
        json.put(KEY_PROPERTY, keyMapper.key(item));

        json.put(ITEM_LABEL_PROPERTY, itemLabelGenerator.apply(item));

        return json;
    }

    private T getData(JsonObject item) {
        if (item == null) {
            return null;
        }
        assert item.hasKey(KEY_PROPERTY);
        JsonValue key = item.get(KEY_PROPERTY);
        return keyMapper.get(key.asString());
    }

    private void refresh() {
        keyMapper.removeAll();
        JsonArray array = generateJson(getDataProvider().fetch(new Query<>()));
        setItems(array);
    }

    private void setItemLabelPath(String path) {
        getElement().setProperty("itemLabelPath", path == null ? "" : path);
    }

    private void setItemValuePath(String path) {
        getElement().setProperty("itemValuePath", path == null ? "" : path);
    }
}
