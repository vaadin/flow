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

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.DemoView;
import com.vaadin.flow.demo.MainLayout;
import com.vaadin.flow.dom.ElementConstants;
import com.vaadin.router.Route;
import com.vaadin.ui.combobox.ComboBox;

/**
 * View for {@link ComboBox} demo.
 */
@Route(value = "vaadin-combo-box", layout = MainLayout.class)
@HtmlImport("bower_components/vaadin-valo-theme/vaadin-combo-box.html")
@ComponentDemo(name = "ComboBox")
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

    private static final String WIDTH_STRING = "250px";

    @Override
    protected void initView() {
        createStringComboBox();
        createObjectComboBox();
        createComboBoxWithObjectStringSimpleValue();
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
        comboBox.setItemLabelGenerator(Song::getName);

        List<Song> listOfSongs = createListOfSongs();

        comboBox.setItems(listOfSongs);
        comboBox.addValueChangeListener(event -> {
            Song song = comboBox.getValue();
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
        comboBox.setItemLabelGenerator(Song::getArtist);

        List<Song> listOfSongs = createListOfSongs();

        comboBox.setItems(listOfSongs);

        comboBox.addValueChangeListener(event -> {
            if (event.getSource().isEmpty()) {
                message.setText("No artist selected");
            } else if (event.getOldValue() == null) {
                message.setText(
                        "Selected artist: " + event.getValue().getArtist());
            } else {
                message.setText(
                        "Selected artist: " + event.getValue().getArtist()
                                + "\nThe old selection was: "
                                + event.getOldValue().getArtist());
            }
        });
        // end-source-example

        comboBox.getStyle().set(ElementConstants.STYLE_WIDTH, WIDTH_STRING);
        comboBox.setId("value-selection-box");
        addCard("Value selection from objects", comboBox, message);
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

    private Div createMessageDiv(String id) {
        Div message = new Div();
        message.setId(id);
        message.getStyle().set("whiteSpace", "pre");
        return message;
    }
}
