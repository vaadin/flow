/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import com.vaadin.event.FieldEvents.BlurNotifier;
import com.vaadin.event.FieldEvents.FocusNotifier;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.hummingbird.kernel.DomEventListener;

import elemental.json.JsonObject;

public abstract class AbstractTextField extends AbstractField<String>
        implements BlurNotifier, FocusNotifier {

    private final DomEventListener textChangeDomListener = new DomEventListener() {
        @Override
        public void handleEvent(JsonObject e) {
            String text = e.getString("value");
            int cursorPos = (int) e.getNumber("selectionStart");
            fireEvent(new TextChangeEvent(AbstractTextField.this, text,
                    cursorPos));
        }
    };

    protected AbstractTextField() {
        super();

        // Always immediate
        getElement().addEventData("change", "element.value");
        getElement().addEventListener("change", e -> {
            setValue(e.getString("element.value"));
        });
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || getValue().length() == 0;
    }

    /**
     * Returns the maximum number of characters in the field. Value -1 is
     * considered unlimited. Terminal may however have some technical limits.
     *
     * @return the maxLength
     */
    public int getMaxLength() {
        return getElement().getAttribute("maxLength", -1);
    }

    /**
     * Sets the maximum number of characters in the field. Value -1 is
     * considered unlimited. Terminal may however have some technical limits.
     *
     * @param maxLength
     *            the maxLength to set
     */
    public void setMaxLength(int maxLength) {
        if (maxLength == -1) {
            getElement().removeAttribute("maxLength");
        } else {
            getElement().setAttribute("maxLength", maxLength);
        }
    }

    /**
     * Gets the current input prompt.
     *
     * @see #setInputPrompt(String)
     * @return the current input prompt, or null if not enabled
     * @deprecated Use {@link #getPlaceHolder()}
     */
    @Deprecated
    public String getInputPrompt() {
        return getPlaceHolder();
    }

    /**
     * Sets the input prompt - a textual prompt that is displayed when the field
     * would otherwise be empty, to prompt the user for input.
     *
     * @param inputPrompt
     * @deprecated Use {@link #setPlaceHolder(String)}
     */
    @Deprecated
    public void setInputPrompt(String inputPrompt) {
        setPlaceHolder(inputPrompt);
    }

    public String getPlaceHolder() {
        return getElement().getAttribute("placeholder", "");
    }

    public void setPlaceHolder(String placeHolder) {
        getElement().setAttribute("placeholder", placeHolder);

    }

    /**
     * Selects all text in the field.
     *
     * @since 6.4
     */
    public void selectAll() {
        String text = getValue() == null ? "" : getValue().toString();
        setSelectionRange(0, text.length());
    }

    /**
     * Sets the range of text to be selected.
     *
     * As a side effect the field will become focused.
     *
     * @since 6.4
     *
     * @param pos
     *            the position of the first character to be selected
     * @param length
     *            the number of characters to be selected
     */
    public void setSelectionRange(int pos, int length) {
        getElement().setAttribute("selectionStart", pos);
        getElement().setAttribute("selectionEnd", pos + length);
        getElement().focus();
    }

    /**
     * Sets the cursor position in the field. As a side effect the field will
     * become focused.
     *
     * @since 6.4
     *
     * @param pos
     *            the position for the cursor
     */
    public void setCursorPosition(int pos) {
        setSelectionRange(pos, 0);
    }

    public void addTextChangeListener(TextChangeListener listener) {
        if (!hasListeners(TextChangeEvent.class)) {
            getElement().addEventData("input", "value", "selectionStart");
            getElement().addEventListener("input", textChangeDomListener);
        }

        addListener(TextChangeEvent.class, listener);
    }

    public void removeTextChangeListener(TextChangeListener listener) {
        removeListener(TextChangeEvent.class, listener);
        if (!hasListeners(TextChangeEvent.class)) {
            getElement().removeEventListener("input", textChangeDomListener);
        }
    }
}
