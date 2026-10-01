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
package com.example.application;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * Binds fields by property name, which introspects the bean class through
 * reflection.
 */
@Route("binder")
@Menu(title = "Binder")
public class BinderView extends Div {

    public static final String NAME_ID = "name";
    public static final String AGE_ID = "age";
    public static final String SAVE_ID = "save";
    public static final String RESULT_ID = "result";

    public BinderView() {
        Input name = new Input();
        name.setId(NAME_ID);
        Input age = new Input();
        age.setId(AGE_ID);
        Span result = new Span();
        result.setId(RESULT_ID);

        Binder<Person> binder = new Binder<>(Person.class);
        binder.forField(name).bind("name");
        binder.forField(age)
                .withConverter(new StringToIntegerConverter("Not a number"))
                .bind("age");

        NativeButton save = new NativeButton("Save", event -> {
            Person person = new Person();
            if (binder.writeBeanIfValid(person)) {
                result.setText(person.getName() + " is " + person.getAge());
            } else {
                result.setText("Invalid");
            }
        });
        save.setId(SAVE_ID);
        add(name, age, save, result);
    }

    public static class Person {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
