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
package com.vaadin.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.annotations.Id;
import com.vaadin.data.HasItems;
import com.vaadin.flow.dom.Element;
import com.vaadin.generated.vaadin.combo.box.GeneratedVaadinComboBox;
import com.vaadin.util.JsonSerializer;

import elemental.json.JsonArray;
import elemental.json.JsonNull;
import elemental.json.JsonValue;

/**
 * Server-side component for the {@code vaadin-combo-box} webcomponent. It
 * contains the same features of the webcomponent, such as item filtering,
 * object selection and item templating.
 * <p>
 * Please note that for the object deserialization (from json to a Java object),
 * this component must know the {@link Class} of the deserialized object. This
 * is done automatically when the {@link #setItems(Collection)} or
 * {@link #setFilteredItems(Collection)} methods are called with a non-empty
 * collection, but if an empty combo box is created, then the
 * {@link #setItemType(Class)} should be called to set the Class of the items
 * (before any deserialization is done, like on {@link #getSelectedItem()} or
 * {@link #getItems()}). This is needed when the items are set from the
 * client-side and the component is created by hooking up to a model using the
 * {@link Id} annotation.
 *
 * @param <T>
 *            the type of the items to be inserted in the combo box
 */
public class ComboBox<T> extends GeneratedVaadinComboBox<ComboBox<T>>
        implements HasSize, HasItems<T>, HasValidation {

    private static final String SELECTED_ITEM_PROPERTY_NAME = "selectedItem";
    private static final String TEMPLATE_TAG_NAME = "template";

    private Class<T> itemType;

    /**
     * Default constructor. Creates an empty combo box.
     *
     * @see #setItemType(Class)
     */
    public ComboBox() {
        getElement().synchronizeProperty(SELECTED_ITEM_PROPERTY_NAME,
                "selected-item-changed");
        getElement().synchronizeProperty(SELECTED_ITEM_PROPERTY_NAME, "change");
        getElement().synchronizeProperty("value", "change");
    }

    /**
     * Creates an empty combo box with the defined label.
     *
     * @param label
     *            the label describing the combo box
     * @see #setItemType(Class)
     */
    public ComboBox(String label) {
        this();
        setLabel(label);
    }

    /**
     * Creates an empty combo box with the defined {@link Class} for the items.
     *
     * @param itemType
     *            the Class of the items. This class definition is used when
     *            deserializing json to Java objects
     */
    public ComboBox(Class<T> itemType) {
        this();
        setItemType(itemType);
    }

    /**
     * Creates an empty combo box with the defined {@link Class} for the items
     * and a label.
     *
     * @param itemType
     *            the Class of the items. This class definition is used when
     *            deserializing json to Java objects
     * @param label
     *            the label describing the combo box
     */
    public ComboBox(Class<T> itemType, String label) {
        this();
        setItemType(itemType);
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
     * @return this instance for method chaining
     */
    public ComboBox<T> setItemTemplate(String template) {
        getElement().getChildren()
                .filter(child -> TEMPLATE_TAG_NAME.equals(child.getTag()))
                .findFirst().ifPresent(element -> element.removeFromParent());

        Element templateElement = new Element(TEMPLATE_TAG_NAME);
        getElement().appendChild(templateElement);
        templateElement.setProperty("innerHTML", template);
        return get();
    }

    /**
     * Gets the current item template associated with this combo box.
     *
     * @return the current item template, or <code>null</code> if it wasn't set
     */
    public String getItemTemplate() {
        Optional<Element> optionalTemplate = getElement().getChildren()
                .filter(child -> TEMPLATE_TAG_NAME.equals(child.getTag()))
                .findFirst();

        if (optionalTemplate.isPresent()) {
            return optionalTemplate.get().getProperty("innerHTML");
        }
        return null;
    }

    /**
     * Gets the current items set on the combo box. To deserialize from json to
     * the type of the object expected by the combo box, the item type must be
     * set. See {@link #setItemType(Class)} for instructions.
     *
     * @return the current items in the combo box, or empty list if there are
     *         none
     */
    public List<T> getItems() {
        return JsonSerializer.toObjects(itemType,
                checkWhetherItemTypeIsSetIfNeeded(protectedGetItems()));
    }

    /**
     * Sets the selectable items in this combo box. Objects are serialized to
     * json using the {@link JsonSerializer#toJson(Collection)}.
     *
     * @param items
     *            the selectable items in this combo box
     */
    @Override
    public void setItems(Collection<T> items) {
        tryToSetItemTypeIfNeeded(items);
        setItems(JsonSerializer.toJson(items));
    }

    /**
     * Gets the {@link Class} representation of the items in the combo box. The
     * item type can be set manually or be automatically detected when the
     * methods {@link #setSelectedItem(Object)}, {@link #setItems(Collection)}
     * or {@link #setFilteredItems(Collection)} are called with a non-empty
     * collection of items.
     * <p>
     * The item type is needed when deserializing objects from json, which occur
     * when calling {@link #getSelectedItem()}, {@link #getItems()} and
     * {@link #getFilteredItems()} methods.
     *
     * @return the type of the items inside the combo box
     */
    public Class<T> getItemType() {
        return itemType;
    }

    /**
     * Sets the {@link Class} representation of the items in the combo box. The
     * item type can be automatically detected when the methods
     * {@link #setSelectedItem(Object)}, {@link #setItems(Collection)} or
     * {@link #setFilteredItems(Collection)} are called with a non-empty
     * collection of items.
     * <p>
     * The item type is needed when deserializing objects from json, which occur
     * when calling {@link #getSelectedItem()}, {@link #getItems()} and
     * {@link #getFilteredItems()} methods.
     *
     * @param itemType
     *            the type of the items inside the combo box
     * @return this instance for method chaining
     */
    public ComboBox<T> setItemType(Class<T> itemType) {
        Objects.requireNonNull(itemType, "itemType can not be null");
        this.itemType = itemType;
        return get();
    }

    /**
     * Gets the selected object of this combo box.
     *
     * @return the selected object, or <code>null</code> if none was selected
     */
    public T getSelectedItem() {
        return JsonSerializer.toObject(itemType,
                checkWhetherItemTypeIsSetIfNeeded(protectedGetSelectedItem()));
    }

    /**
     * Manually sets the selected object of this combo box (without user
     * interaction). This can be used to set a default value of the combo box.
     *
     * @param item
     *            the selected object, or <code>null</code> to clear the
     *            selection
     * @return this instance for method chaining
     */
    public ComboBox<T> setSelectedItem(T item) {
        tryToSetItemTypeIfNeeded(item);
        JsonValue json = JsonSerializer.toJson(item);
        getElement().setPropertyJson(SELECTED_ITEM_PROPERTY_NAME, json);
        return get();
    }

    /**
     * Gets the list of items which were filtered by the user input. Filter
     * events can be received by using
     * {@link #addFilterChangeListener(com.vaadin.flow.event.ComponentEventListener)}.
     *
     * @return the list of filtered items, or empty list if none were filtered
     */
    public List<T> getFilteredItems() {
        return JsonSerializer.toObjects(itemType,
                checkWhetherItemTypeIsSetIfNeeded(protectedGetFilteredItems()));
    }

    /**
     * Convenience method for the {@link #setFilteredItems(Collection)}. It sets
     * the list of visible items in reaction of the input of the user. Filter
     * events can be received by using
     * {@link #addFilterChangeListener(com.vaadin.flow.event.ComponentEventListener)}.
     *
     * @param filteredItems
     *            the items to show in response of a filter input
     * @return this instance for method chaining
     */
    public ComboBox<T> setFilteredItems(T... filteredItems) {
        return setFilteredItems(Arrays.asList(filteredItems));
    }

    /**
     * It sets the list of visible items in reaction of the input of the user.
     * Filter events can be received by using
     * {@link #addFilterChangeListener(com.vaadin.flow.event.ComponentEventListener)}.
     *
     * @param filteredItems
     *            the items to show in response of a filter input
     * @return this instance for method chaining
     */
    public ComboBox<T> setFilteredItems(Collection<T> filteredItems) {
        tryToSetItemTypeIfNeeded(filteredItems);
        setFilteredItems(JsonSerializer.toJson(filteredItems));
        return get();
    }

    /*
     * Method that checks if the item type is set if needed. The item type is
     * not needed to deserialize null objects and empty arrays.
     */
    private <I extends JsonValue> I checkWhetherItemTypeIsSetIfNeeded(I value) {
        if (itemType != null || value == null || value instanceof JsonNull) {
            return value;
        }
        if (value instanceof JsonArray && ((JsonArray) value).length() == 0) {
            return value;
        }
        throw new IllegalStateException(
                "Error: itemType is null. Set the type by using setItemType(Class<T>) method or by setting items using the "
                        + "setItems(Collection<T>) or setFilteredItems(Collection<T>) methods");
    }

    /*
     * Method that does the best effort to get the actual Class of the elements
     * inside the combo box, since it's not possible to get it directly due to
     * limitations on Java generics.
     */
    private void tryToSetItemTypeIfNeeded(Collection<T> items) {
        if (itemType == null && items != null && !items.isEmpty()) {
            setItemType((Class<T>) items.iterator().next().getClass());
        }
    }

    /*
     * Same as the overloaded method, but for a single item.
     */
    private void tryToSetItemTypeIfNeeded(T item) {
        if (itemType == null && item != null) {
            setItemType((Class<T>) item.getClass());
        }
    }

    @Override
    public String getEmptyValue() {
        return "";
    }
}
