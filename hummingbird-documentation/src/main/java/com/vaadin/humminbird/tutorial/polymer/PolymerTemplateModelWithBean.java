package com.vaadin.humminbird.tutorial.polymer;

import com.vaadin.annotations.EventHandler;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;

@CodeFor("tutorial-template-model-bean.asciidoc")
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

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public interface FormModel extends TemplateModel {
        void setPerson(Person person);
        Person getPerson();
    }

    public class Form extends PolymerTemplate<FormModel> {
        public Form() {
            Person person = new Person("John", "Doe", 82);
            getModel().setPerson(person);
        }

        @Override
        protected FormModel getModel() {
            return (FormModel) super.getModel();
        }

        @EventHandler
        public void setNameToJeff() {
            getModel().getPerson().setFirstName("Jeff");
        }
    }

}
