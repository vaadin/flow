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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.dom.ElementConstants;
import com.vaadin.flow.html.Div;
import com.vaadin.ui.ComboBox;

/**
 * View for {@link ComboBox} demo.
 */
@ComponentDemo(name = "ComboBox", href = "vaadin-combo-box")
public class ComboBoxView extends DemoView {
    /**
     * Example object.
     */
    public static class Song {
        private String name;
        private String artist;
        private String album;

        /**
         * Default constructor.
         */
        public Song() {
        }

        /**
         * Construct a song with the given name, artist and album.
         * 
         * @param name
         *            name of the song
         * @param artist
         *            name of the artist
         * @param album
         *            name of the album
         */
        public Song(String name, String artist, String album) {
            this.name = name;
            this.artist = artist;
            this.album = album;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }
    }

    /**
     * Another example object.
     */
    public static class Fruit {
        private String name;
        private String color;

        /**
         * Default constructor.
         */
        public Fruit() {
        }

        /**
         * Construct a fruit with the given name and color.
         * 
         * @param name
         *            name of the fruit
         * @param color
         *            color of the fruit
         */
        public Fruit(String name, String color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    private static final String WIDTH_STRING = "250px";

    @Override
    void initView() {
        createStringComboBox();
        createObjectComboBox();
        createComboBoxWithObjectStringSimpleValue();
        createComboBoxWithCustomFilter();
    }

    private void createStringComboBox() {
        Div message = createMessageDiv("string-selection-message");

        // begin-source-example
        // source-example-heading: String selection
        ComboBox<String> comboBox = new ComboBox<>("Browsers");
        comboBox.setItems("Google Chrome", "Mozilla Firefox", "Opera",
                "Apple Safari", "Microsoft Edge");

        comboBox.addValueChangeListener(event -> {
            if (event.getSource().isEmpty()) {
                message.setText("No browser selected");
            } else {
                message.setText("Selected browser: " + event.getValue());
            }
        });
        // end-source-example

        comboBox.getStyle().set(ElementConstants.STYLE_WIDTH, WIDTH_STRING);
        comboBox.setId("string-selection-box");
        addCard("String selection", comboBox, message);
    }

    private void createObjectComboBox() {
        Div message = createMessageDiv("object-selection-message");

        // begin-source-example
        // source-example-heading: Object selection
        ComboBox<Song> comboBox = new ComboBox<>();
        comboBox.setLabel("Music selection");
        comboBox.setItemLabelPath("name");

        List<Song> listOfSongs = createListOfSongs();

        comboBox.setItems(listOfSongs);
        comboBox.addSelectedItemChangeListener(event -> {
            Song song = comboBox.getSelectedItem();
            if (song != null) {
                message.setText("Selected song: " + song.getName()
                        + "\nFrom album: " + song.getAlbum() + "\nBy artist: "
                        + song.getArtist());
            } else {
                message.setText("No song is selected");
            }
        });
        // end-source-example

        comboBox.getStyle().set(ElementConstants.STYLE_WIDTH, WIDTH_STRING);
        comboBox.setId("object-selection-box");
        addCard("Object selection", comboBox, message);
    }

    private void createComboBoxWithObjectStringSimpleValue() {
        Div message = createMessageDiv("value-selection-message");

        // begin-source-example
        // source-example-heading: Value selection from objects
        ComboBox<Song> comboBox = new ComboBox<>("Artists");
        comboBox.setItemLabelPath("artist");
        comboBox.setItemValuePath("artist");

        List<Song> listOfSongs = createListOfSongs();

        comboBox.setItems(listOfSongs);

        comboBox.addValueChangeListener(event -> {
            if (event.getSource().isEmpty()) {
                message.setText("No artist selected");
            } else if (event.getOldValue().isEmpty()) {
                message.setText("Selected artist: " + event.getValue());
            } else {
                message.setText("Selected artist: " + event.getValue()
                        + "\nThe old selection was: " + event.getOldValue());
            }
        });
        // end-source-example

        comboBox.getStyle().set(ElementConstants.STYLE_WIDTH, WIDTH_STRING);
        comboBox.setId("value-selection-box");
        addCard("Value selection from objects", comboBox, message);
    }

    private void createComboBoxWithCustomFilter() {
        Div message = createMessageDiv("custom-filter-message");

        // begin-source-example
        // source-example-heading: Custom filtering
        ComboBox<Fruit> comboBox = new ComboBox<>(
                "Filter fruits by color (e.g. red, green, yellow...)");
        comboBox.setItemLabelPath("name");
        comboBox.setItemTemplate(
                "Fruit: [[item.name]]<br>Color: <b>[[item.color]]</b>");

        // when using custom filter, you don't need to call setItems
        List<Fruit> listOfFruits = createListOfFruits();

        comboBox.addFilterChangeListener(event -> {
            String filter = comboBox.getFilter();
            if (filter.isEmpty()) {
                comboBox.setFilteredItems(listOfFruits);
            } else {
                message.setText("Filter used: " + filter);
                List<Fruit> filtered = listOfFruits.stream()
                        .filter(fruit -> fruit.getColor().toLowerCase()
                                .startsWith(filter.toLowerCase()))
                        .collect(Collectors.toList());
                comboBox.setFilteredItems(filtered);
            }
        });

        comboBox.addSelectedItemChangeListener(event -> {
            if (event.getSource().isEmpty()) {
                message.setText("No fruit selected");
            } else {
                message.setText("Selected fruit: "
                        + event.getSource().getSelectedItem().getName());
            }
        });
        // end-source-example

        comboBox.getStyle().set(ElementConstants.STYLE_WIDTH, WIDTH_STRING);
        comboBox.setId("custom-filter-box");
        addCard("Custom filtering", comboBox, message);
    }

    private List<Song> createListOfSongs() {
        List<Song> listOfSongs = new ArrayList<>();
        listOfSongs.add(new Song("A V Club Disagrees", "Haircuts for Men",
                "Physical Fitness"));
        listOfSongs.add(new Song("Sculpted", "Haywyre", "Two Fold Pt.1"));
        listOfSongs.add(
                new Song("Voices of a Distant Star", "Killigrew", "Animus II"));
        return listOfSongs;
    }

    private List<Fruit> createListOfFruits() {
        List<Fruit> listOfFruits = new ArrayList<>();
        listOfFruits.add(new Fruit("Banana", "Yellow"));
        listOfFruits.add(new Fruit("Apple", "Red"));
        listOfFruits.add(new Fruit("Strawberry", "Red"));
        listOfFruits.add(new Fruit("Grape", "Purple"));
        listOfFruits.add(new Fruit("Lemon", "Green"));
        listOfFruits.add(new Fruit("Watermelon", "Green"));
        listOfFruits.add(new Fruit("Orange", "Orange"));
        return listOfFruits;
    }

    private Div createMessageDiv(String id) {
        Div message = new Div();
        message.setId(id);
        message.getStyle().set("whiteSpace", "pre");
        return message;
    }
}
