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
package com.vaadin.ui.dialog;

import com.vaadin.flow.dom.Element;

/**
 * Server-side component for the {@code <vaadin-dialog>} element.
 * 
 * @author Vaadin Ltd
 */
public class Dialog extends GeneratedVaadinDialog<Dialog> {

    /**
     * Creates an empty dialog.
     */
    public Dialog() {
        this("");
    }

    /**
     * Creates a dialog with the given String rendered as it's HTML content.
     * 
     * @param content
     *            the content of the Dialog as HTML markup
     */
    public Dialog(String content) {
        Element templateElement = new Element("template");
        getElement().appendChild(templateElement);

        templateElement.setProperty("innerHTML", content);
    }

    /**
     * Opens the dialog.
     */
    public void open() {
        setOpened(true);
    }

    /**
     * Closes the dialog.
     */
    public void close() {
        setOpened(false);
    }

}
