package com.vaadin.ui;

import java.util.Collection;

import com.vaadin.generated.vaadin.combo.box.GeneratedVaadinComboBox;
import com.vaadin.util.JsonSerializer;

import elemental.json.JsonObject;

public class VaadinComboBox extends GeneratedVaadinComboBox<VaadinComboBox> {

    public VaadinComboBox() {
        getElement().synchronizeProperty("selectedItem",
                "selected-item-changed");
    }

    public VaadinComboBox(String label) {
        this();
        setLabel(label);
    }

    public VaadinComboBox(String label, Collection<?> items) {
        this();
        setLabel(label);
        setItems(items);
    }

    public VaadinComboBox setItems(Collection<?> items) {
        setItems(JsonSerializer.toJson(items));
        return this;
    }

    public <T> T getSelectedValue(Class<T> clazz) {
        JsonObject selectedItem = getSelectedItem();
        return JsonSerializer.toObject(clazz, selectedItem);
    }

}
