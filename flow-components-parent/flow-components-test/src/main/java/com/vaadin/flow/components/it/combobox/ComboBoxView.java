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

import com.vaadin.data.provider.DataProvider;
import com.vaadin.flow.components.it.TestView;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.combobox.ComboBox;

/**
 * Test view for {@link ComboBox}.
 */
public class ComboBoxView extends TestView {

    /**
     * Creates a new instance.
     */
    public ComboBoxView() {
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
}
