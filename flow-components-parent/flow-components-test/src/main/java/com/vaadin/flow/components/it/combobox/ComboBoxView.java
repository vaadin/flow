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
package com.vaadin.flow.components.it.combobox;

import java.util.stream.Stream;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.combobox.ComboBox;
import com.vaadin.ui.html.Label;

/**
 * Test view for {@link ComboBox}.
 */
public class ComboBoxView extends TestView {

    /**
     * Data class for a Grid.
     *
     */
    public enum Title {
        MR, MRS;
    }

    private Label selectedTitle = new Label();

    /**
     * Creates a new instance.
     */
    public ComboBoxView() {
        createWithUpdateProvider();
        createWithValueChangeListener();
        createWithPresetValue();
    }

    private void createWithUpdateProvider() {
        ComboBox<String> comboBox = new ComboBox<>();

        comboBox.setItems("foo", "bar");

        comboBox.setId("combo");

        Button setProvider = new Button("Update data provider",
                event -> comboBox.setDataProvider(
                        DataProvider.ofItems("baz", "foobar")));
        setProvider.setId("update-provider");

        Button setItemCaptionGenerator = new Button("Update caption generator",
                event -> comboBox.setItemLabelGenerator(
                        item -> String.valueOf(item.length())));
        setItemCaptionGenerator.setId("update-caption-gen");

        add(comboBox, setProvider, setItemCaptionGenerator);
    }

    private void createWithValueChangeListener() {
        ComboBox<Title> titles = new ComboBox<>();

        titles.setItems(Stream.of(Title.values()));

        titles.setId("titles");
        selectedTitle.setId("selected-titles");
        titles.addValueChangeListener(event -> handleSelection(titles));
        add(titles, selectedTitle);
    }

    private void createWithPresetValue() {
        ComboBox<Title> titles = new ComboBox<>();

        titles.setItems(Stream.of(Title.values()));
        titles.setValue(Title.MRS);

        titles.setId("titles-with-preset-value");
        add(titles);
    }

    private void handleSelection(ComboBox<Title> titles) {
        Title title = titles.getValue();
        selectedTitle.setText(title.name());
    }
}
