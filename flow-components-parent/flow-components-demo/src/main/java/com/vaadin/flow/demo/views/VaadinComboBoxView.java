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
package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.combo.box.VaadinComboBox;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.html.Label;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * View for {@link VaadinComboBox} demo.
 */
@ComponentDemo(name = "Vaadin ComboBox", href = "vaadin-combo-box")
public class VaadinComboBoxView extends DemoView {

    private Label song;
    private Label artist;
    private Label album;

    @Override
    void initView() {
        VaadinComboBox comboBox = new VaadinComboBox();
        comboBox.setLabel("Music selection");
        comboBox.setItemLabelPath("Song");
        comboBox.setItemValuePath("Song");

        JsonArray items = Json.createArray();
        items.set(0, createItem("A V Club Disagrees", "Haircuts for Men",
                "Physical Fitness"));
        items.set(1, createItem("Sculpted", "Haywyre", "Two Fold Pt.1"));
        items.set(2, createItem("Voices of a Distant Star", "Killigrew",
                "Animus II"));
        comboBox.setItems(items);

        comboBox.getElement().synchronizeProperty("selectedItem",
                "selected-item-changed");
        comboBox.addChangeListener(
                event -> setSelection(comboBox.getSelectedItem()));

        add(comboBox);

        song = new Label("Song:");
        artist = new Label("Artist:");
        album = new Label("Album:");

        add(song, artist, album);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.add(ElementFactory.createHeading3("Simple sample:"));
        container.addCode("VaadinComboBox comboBox = new VaadinComboBox();\n"
                + "comboBox.setLabel(\"Music selection\");\n"
                + "JsonArray items = Json.createArray();\n"
                + "items.set(0, \"A V Club Disagrees\");\n"
                + "items.set(1, \"Sculpted\");\n"
                + "items.set(2, \"Voices of a Distant Star\");\n"
                + "comboBox.setItems(items);\n"
                + "layoutContainer.add(comboBox);");

        container.add(ElementFactory
                .createHeading3("Moderate sample using JsonItem:"));
        container.addCode("VaadinComboBox comboBox = new VaadinComboBox();\n"
                + "comboBox.setLabel(\"Music selection\");\n"
                + "comboBox.setItemLabelPath(\"Song\");\n"
                + "comboBox.setItemValuePath(\"Song\");\n\n"
                + "JsonArray items = Json.createArray();\n"
                + "items.set(0, createItem(\"A V Club Disagrees\", \"Haircuts for Men\", \"Physical Fitness\"));comboBox.setItems(items);\n"
                + "\n"
                + "comboBox.getElement().synchronizeProperty(\"selectedItem\", \"selected-item-changed\");\n"
                + "comboBox.addChangeListener( event -> setSelection(comboBox.getSelectedItem()));\n"
                + "\n" + "layoutContainer.add(comboBox);\n");
    }

    private JsonObject createItem(String song, String artist, String album) {
        JsonObject item = Json.createObject();

        item.put("Song", song);
        item.put("Artist", artist);
        item.put("Album", album);

        return item;
    }

    public void setSelection(JsonObject selection) {
        if (selection == null) {
            updateLabels("", "", "");
        } else {
            updateLabels(selection.getString("Song"),
                    selection.getString("Artist"),
                    selection.getString("Album"));
        }
    }

    private void updateLabels(String song, String artist, String album) {
        this.song.setText("Song:   " + song);
        this.artist.setText("Artist: " + artist);
        this.album.setText("Album:  " + album);
    }
}
