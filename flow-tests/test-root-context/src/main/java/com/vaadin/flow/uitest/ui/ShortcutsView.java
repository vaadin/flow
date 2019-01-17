package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ShortcutsView",
        layout = ViewTestLayout.class)
public class ShortcutsView extends Div {

    private Paragraph invisibleP = new Paragraph("invisible");
    private boolean attached = false;

    public ShortcutsView() {
        Paragraph expected = new Paragraph();
        expected.setId("expected");
        expected.setText("testing...");

        NativeButton button = new NativeButton();
        button.setId("button");
        button.addClickListener(e -> expected.setText("button"));
        button.addClickShortcut(Key.KEY_B, KeyModifier.ALT);

        Input input = new Input();
        input.setId("input");
        input.addFocusShortcut(Key.KEY_F, KeyModifier.ALT);

        UI.getCurrent().addShortcut(() -> {
            invisibleP.setVisible(!invisibleP.isVisible());
            expected.setText("toggled!");
        }, Key.KEY_I, KeyModifier.ALT);

        Shortcuts.registerShortcut(UI.getCurrent(), invisibleP, () -> expected
                .setText("invisibleP"), Key.KEY_V).withAlt();

        add(expected, button, input, invisibleP);

        Div subview = new Div();
        subview.setId("subview");

        Input focusTarget = new Input();
        focusTarget.setId("focusTarget");

        subview.add(focusTarget);

        // only works, when focusTarget is focused
        Shortcuts.addShortcut(subview, () -> expected.setText("subview"),
                Key.KEY_S, KeyModifier.ALT);

        add(subview);

        Paragraph attachable = new Paragraph("attachable");
        attachable.setId("attachable");

        Shortcuts.registerShortcut(UI.getCurrent(), attachable, () -> expected
                .setText("attachable"), Key.KEY_A).withAlt();

        UI.getCurrent().addShortcut(() -> {
            attached = !attached;
            if (attached) {
                add(attachable);
            }
            else {
                remove(attachable);
            }
            expected.setText("toggled!");
        }, Key.KEY_Y, KeyModifier.ALT);
    }
}

