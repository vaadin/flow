package com.vaadin.flow.npmtest.ui.littemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.npmtest.ui.littemplate.SimpleLitTemplate.SimpleModel;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("simple-lit-template")
@JsModule("lit/simple-lit-template.js")
@NpmPackage(value = "lit-element", version = "2.1.0")
public class SimpleLitTemplate extends LitTemplate<SimpleModel> {

    private static final String[] firstNames = new String[] { "Nash", "Cade",
            "Willa", "Joel", "Mechelle" };
    private static final String[] lastNames = new String[] { "Davenport",
            "Suarez", "Morrison" };

    @Id
    public NativeButton hello;
    @Id
    public NativeButton addString;
    @Id
    public NativeButton addPerson;
    private int personIndex = 1;

    public interface SimpleModel extends TemplateModel {
        public void setText(String text);

        public List<String> getStrings();

        public void setStrings(List<String> strings);

        public List<LitPerson> getPersons();

        public void setPersons(List<LitPerson> persons);
    }

    public SimpleLitTemplate() {
        super();
        getModel().setText("Hello");
        List<String> strings = new ArrayList<>();
        Collections.addAll(strings,
                new String[] { "String 1", "String 2", "String 3" });
        getModel().setStrings(strings);
        getModel().setPersons(new ArrayList<>());
        getModel().getPersons()
                .add(new LitPerson(personIndex++, "Wyoming", "Wiggins"));
        getModel().getPersons()
                .add(new LitPerson(personIndex++, "Ori", "Griffith"));
        getModel().getPersons()
                .add(new LitPerson(personIndex++, "Maisie", "Hurst"));
        hello.addClickListener(e -> {
            getModel().setText(
                    "Hello from the server at " + System.currentTimeMillis());
        });
        addString.addClickListener(e -> {
            List<String> newStrings = new ArrayList<>(getModel().getStrings());
            newStrings.add("String created at " + System.currentTimeMillis());
            getModel().setStrings(newStrings);
        });
        addPerson.addClickListener(e -> {
            getModel().getPersons()
                    .add(new LitPerson(personIndex++,
                            firstNames[personIndex % firstNames.length],
                            lastNames[personIndex % lastNames.length]));
        });
    }

    @ClientCallable
    public void deleteString(String string) {
        List<String> newList = getModel().getStrings().stream()
                .filter(str -> !str.equals(string))
                .collect(Collectors.toList());
        getModel().setStrings(newList);
    }

    @ClientCallable
    public void deletePerson(int personId) {
        getModel().getPersons().removeIf(p -> p.getId() == personId);
    }
}
