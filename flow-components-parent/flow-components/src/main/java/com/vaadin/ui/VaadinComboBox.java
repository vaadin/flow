package com.vaadin.ui;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import com.vaadin.generated.vaadin.combo.box.GeneratedVaadinComboBox;
import com.vaadin.util.JsonSerializer;

import elemental.json.JsonObject;

public class VaadinComboBox<T extends Serializable>
        extends GeneratedVaadinComboBox<VaadinComboBox<T>> {

    private Class<T> itemType;

    public VaadinComboBox() {
        getElement().synchronizeProperty("selectedItem",
                "selected-item-changed");
    }

    public VaadinComboBox(String label) {
        this();
        setLabel(label);
    }

    public VaadinComboBox(Class<T> itemType) {
        this();
        setItemType(itemType);
    }

    public VaadinComboBox(Class<T> itemType, String label) {
        this();
        setItemType(itemType);
        setLabel(label);
    }

    public VaadinComboBox(String label, Collection<T> items) {
        this();
        setLabel(label);
        setItems(items);
    }

    @SafeVarargs
    public VaadinComboBox(String label, T... items) {
        this();
        setLabel(label);
        setItems(items);
    }

    public VaadinComboBox<T> setItems(T... items) {
        return setItems(Arrays.asList(items));
    }

    public VaadinComboBox<T> setItems(Collection<T> items) {
        if (items != null && !items.isEmpty()) {
            setItemType((Class<T>) items.iterator().next().getClass());
        }
        setItems(JsonSerializer.toJson(items));
        return get();
    }

    public VaadinComboBox<T> setItemType(Class<T> itemType) {
        Objects.requireNonNull(itemType, "itemType can not be null");
        this.itemType = itemType;
        return get();
    }

    public Class<T> getItemType() {
        return itemType;
    }

    public T getSelectedObject() {
        Objects.requireNonNull(itemType,
                "itemType is null. Set the type by using setItemType(Class<T>) method or get the selected object with the getSelectedObject(Class<T>) method");
        return getSelectedObject((Class<T>) itemType);
    }

    public T getSelectedObject(Class<T> itemType) {
        JsonObject selectedItem = super.getSelectedItem();
        return JsonSerializer.toObject(itemType, selectedItem);
    }

}
