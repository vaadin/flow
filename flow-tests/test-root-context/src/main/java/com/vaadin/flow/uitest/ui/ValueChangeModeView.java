package com.vaadin.flow.uitest.ui;

import java.util.Arrays;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.data.value.HasValueChangeMode;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ValueChangeModeView", layout = ViewTestLayout.class)
public class ValueChangeModeView extends AbstractDebounceSynchronizeView {

    @Override
    protected void onShow() {
        Input input = new Input();
        input.setId("input");
        input.setValueChangeTimeout(CHANGE_TIMEOUT);
        add(input);
        input.addValueChangeListener(
                event -> addChangeMessage(event.getValue()));
        addButtons(input);
        addChangeMessagesDiv();
    }

    private void addButtons(HasValueChangeMode component) {
        Arrays.stream(ValueChangeMode.values()).forEach(mode -> {
            NativeButton button = createButton(mode.name(), mode.name(),
                    event -> component.setValueChangeMode(mode));
            add(button);
        });
    }

}
