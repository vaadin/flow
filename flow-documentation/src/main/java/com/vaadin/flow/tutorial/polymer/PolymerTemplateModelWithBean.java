package com.vaadin.flow.tutorial.polymer;

import com.vaadin.ui.polymertemplate.EventHandler;
import com.vaadin.flow.model.Exclude;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-model-bean.asciidoc")
public class PolymerTemplateModelWithBean {
    public class Person {
        private String firstName, lastName;
        private int age;

        private Long id;

        public Person() {
            // Needed for TemplateModel
        }

        public Person(String firstName, String lastName, int age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public int getAge() {
            return age;
        }

        public Long getId() {
            return id;
        }
    }

    public interface FormModel extends TemplateModel {
        @Exclude("id")
        void setPerson(Person person);

        Person getPerson();
    }

    public class Form extends PolymerTemplate<FormModel> {
        public Form() {
            Person person = new Person("John", "Doe", 82);
            getModel().setPerson(person);
        }

        @EventHandler
        public void setNameToJeff() {
            getModel().getPerson().setFirstName("Jeff");
        }
    }

}
