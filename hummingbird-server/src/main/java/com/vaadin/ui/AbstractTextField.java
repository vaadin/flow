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

import com.vaadin.annotations.JavaScript;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.BlurNotifier;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.FocusNotifier;
import com.vaadin.hummingbird.kernel.Element;

public abstract class AbstractTextField extends AbstractField<String>
        implements BlurNotifier, FocusNotifier {

    @JavaScript("abstracttextfield.js")
    public interface JS extends PublishedJavascript {
        public void setSelectionRange(Element e, int pos, int length);

        public void focus(Element element);
    }

    protected AbstractTextField() {
        super();
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
        // FIXME
        // getElement().focus();
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

    @Override
    public void addFocusListener(FocusListener listener) {
        addListener(FocusEvent.EVENT_ID, FocusEvent.class, listener,
                FocusListener.focusMethod);
    }

    @Override
    public void removeFocusListener(FocusListener listener) {
        removeListener(FocusEvent.EVENT_ID, FocusEvent.class, listener);
    }

    @Override
    public void addBlurListener(BlurListener listener) {
        addListener(BlurEvent.EVENT_ID, BlurEvent.class, listener,
                BlurListener.blurMethod);
    }

    @Override
    public void removeBlurListener(BlurListener listener) {
        removeListener(BlurEvent.EVENT_ID, BlurEvent.class, listener);
    }

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {
        super.addValueChangeListener(listener);

        if (!hasElementEventListener("change")) {
            getElement().addEventData("change", "element.value");
            addElementEventListener("change", e -> {
                setValue(e.getString("element.value"));
            });
        }

    }

}
