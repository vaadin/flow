/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.PasteEvent;
import com.vaadin.flow.component.clipboard.PasteOptions;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Registers three paste listeners so the IT can prove each variant:
 * <ul>
 * <li>A listener on the focusable {@code #target} div (the view's child) writes
 * to {@code #status}.</li>
 * <li>A view-scoped listener with default {@link PasteOptions} (editable
 * targets skipped) writes to {@code #status-global}.</li>
 * <li>A view-scoped listener with {@link PasteOptions#includingInputFields()}
 * writes to {@code #status-global-include}.</li>
 * </ul>
 * Each "global" status line carries the resolved target element's id so the IT
 * can distinguish a paste on the div from a paste on the {@code #input} field.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerPasteView", layout = ViewTestLayout.class)
public class TriggerPasteView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div target = new Div();
        target.setId("target");
        // paste only fires on focused elements; tabindex makes the div
        // focusable.
        target.getElement().setAttribute("tabindex", "0");
        target.setText("Focus me and paste");

        Div status = new Div();
        status.setId("status");

        Input input = new Input();
        input.setId("input");

        Div statusGlobal = new Div();
        statusGlobal.setId("status-global");

        Div statusGlobalInclude = new Div();
        statusGlobalInclude.setId("status-global-include");

        add(target, status, input, statusGlobal, statusGlobalInclude);

        Clipboard.onPaste(target, event -> status.setText(
                "text=" + event.getText() + ";html=" + event.getHtml()));

        Clipboard.onPaste(this, PasteOptions.defaults(),
                event -> statusGlobal.setText(formatGlobal(event)));

        Clipboard.onPaste(this, PasteOptions.includingInputFields(),
                event -> statusGlobalInclude.setText(formatGlobal(event)));
    }

    private static String formatGlobal(PasteEvent event) {
        Element target = event.getTargetElement();
        String id = target != null ? target.getAttribute("id") : null;
        return "target=" + (id != null ? id : "null") + ";text="
                + event.getText();
    }
}
