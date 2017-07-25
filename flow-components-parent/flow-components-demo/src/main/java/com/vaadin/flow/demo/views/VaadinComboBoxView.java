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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.html.Label;
import com.vaadin.ui.VaadinComboBox;

/**
 * View for {@link VaadinComboBox} demo.
 */
@ComponentDemo(name = "Vaadin ComboBox", href = "vaadin-combo-box")
public class VaadinComboBoxView extends DemoView {

    private Label song;
    private Label artist;
    private Label album;

    public static class Song implements Serializable {
        private String name;
        private String artist;
        private String album;

        public Song() {
        }

        public Song(String name, String artist, String album) {
            this.name = name;
            this.artist = artist;
            this.album = album;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name
         *            the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the artist
         */
        public String getArtist() {
            return artist;
        }

        /**
         * @param artist
         *            the artist to set
         */
        public void setArtist(String artist) {
            this.artist = artist;
        }

        /**
         * @return the album
         */
        public String getAlbum() {
            return album;
        }

        /**
         * @param album
         *            the album to set
         */
        public void setAlbum(String album) {
            this.album = album;
        }
    }

    @Override
    void initView() {
        VaadinComboBox<Song> comboBox = new VaadinComboBox<>();
        comboBox.setLabel("Music selection");
        comboBox.setItemLabelPath("name");
        comboBox.setItemValuePath("");

        List<Song> listOfSongs = new ArrayList<>();
        listOfSongs.add(new Song("A V Club Disagrees", "Haircuts for Men",
                "Physical Fitness"));
        listOfSongs.add(new Song("Sculpted", "Haywyre", "Two Fold Pt.1"));
        listOfSongs.add(
                new Song("Voices of a Distant Star", "Killigrew", "Animus II"));

        comboBox.setItems(listOfSongs);
        comboBox.addChangeListener(
                event -> setSelection(comboBox.getSelectedObject(Song.class)));

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

    public void setSelection(Song selection) {
        if (selection == null) {
            updateLabels("", "", "");
        } else {
            updateLabels(selection.getName(), selection.getArtist(),
                    selection.getAlbum());
        }
    }

    private void updateLabels(String song, String artist, String album) {
        this.song.setText("Song:   " + song);
        this.artist.setText("Artist: " + artist);
        this.album.setText("Album:  " + album);
    }
}
