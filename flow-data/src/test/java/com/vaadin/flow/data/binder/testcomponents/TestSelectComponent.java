package com.vaadin.flow.data.binder.testcomponents;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.selection.MultiSelectionListener;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
import elemental.json.JsonArray;

@Tag("test-select-field")
public class TestSelectComponent<T>
        extends AbstractSinglePropertyField<TestSelectComponent<T>, Set<T>>
        implements MultiSelect<TestSelectComponent<T>, T>, HasValidation {

    private final KeyMapper<T> keyMapper = new KeyMapper<>(i -> i);

    public TestSelectComponent() {
        super("value", Collections.emptySet(), JsonArray.class,
                TestSelectComponent::presentationToModel,
                TestSelectComponent::modelToPresentation);
    }

    @Override
    public void updateSelection(Set<T> addedItems, Set<T> removedItems) {
        Set<T> value = new HashSet<>(getValue());
        value.addAll(addedItems);
        value.removeAll(removedItems);
        setValue(value);
    }

    @Override
    public Set<T> getSelectedItems() {
        return getValue();
    }

    @Override
    public Registration addSelectionListener(
            MultiSelectionListener<TestSelectComponent<T>, T> listener) {
        return addValueChangeListener(event -> listener
                .selectionChange(new MultiSelectionEvent<>(this, this,
                        event.getOldValue(), event.isFromClient())));
    }

    private String errorMessage = "";
    private boolean invalid;

    @Override
    public void setErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            errorMessage = "";
        }
        this.errorMessage = errorMessage;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    @Override
    public boolean isInvalid() {
        return invalid;
    }

    private static <T> Set<T> presentationToModel(TestSelectComponent<T> group,
            JsonArray presentation) {
        Set<T> set = new HashSet<>();
        for (int i = 0; i < presentation.length(); i++) {
            set.add(group.keyMapper.get(presentation.getString(i)));
        }
        return set;
    }

    private static <T> JsonArray modelToPresentation(
            TestSelectComponent<T> group, Set<T> model) {
        JsonArray array = Json.createArray();
        if (model.isEmpty()) {
            return array;
        }

        model.stream().map(group.keyMapper::key)
                .forEach(key -> array.set(array.length(), key));
        return array;
    }

}
