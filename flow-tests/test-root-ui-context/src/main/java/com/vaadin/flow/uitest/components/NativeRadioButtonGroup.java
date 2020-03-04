package com.vaadin.flow.uitest.components;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;

public class NativeRadioButtonGroup<T> extends Composite<Div> {
    private final String group;
    private T selected;

    private List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    public NativeRadioButtonGroup(String caption) {
        getContent().add(new Text(caption));
        this.group = caption.replaceAll(" ", "").toLowerCase();
        getContent().getStyle().set("display", "block");
    }

    public Input addOption(String caption, T value) {
        Input input = new Input();
        input.setId(caption.replaceAll("[ .]", "").toLowerCase());

        input.getElement().setAttribute("name", group);
        input.getElement().setAttribute("type", "radio");

        // Last one to receive a change event is selected
        input.getElement().addEventListener("change", event -> {
            selected = value;
            listeners.forEach(listener -> listener.accept(selected));
        }).setFilter("element.checked");

        // Preselect first option
        if (selected == null) {
            assert value != null;

            selected = value;
            input.getElement().setAttribute("checked", true);
        }

        Label label = new Label(caption);
        label.setFor(input);

        getContent().add(new Div(input, label));

        return input;
    }

    public void addValueChangeListener(Consumer<T> listener) {
        listeners.add(listener);
    }
}