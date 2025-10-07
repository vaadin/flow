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
package com.vaadin.flow.data.binder.testcomponents;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.selection.MultiSelectionListener;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

@Tag("test-select-field")
public class TestSelectComponent<T>
        extends AbstractSinglePropertyField<TestSelectComponent<T>, Set<T>>
        implements MultiSelect<TestSelectComponent<T>, T>, HasValidation {

    private final KeyMapper<T> keyMapper = new KeyMapper<>(i -> i);

    public TestSelectComponent() {
        super("value", Collections.emptySet(), ArrayNode.class,
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
            ArrayNode presentation) {
        Set<T> set = new HashSet<>();
        for (int i = 0; i < presentation.size(); i++) {
            set.add(group.keyMapper.get(presentation.get(i).asText()));
        }
        return set;
    }

    private static <T> ArrayNode modelToPresentation(
            TestSelectComponent<T> group, Set<T> model) {
        ArrayNode array = JacksonUtils.createArrayNode();
        if (model.isEmpty()) {
            return array;
        }

        model.stream().map(group.keyMapper::key).forEach(key -> array.add(key));
        return array;
    }

}
