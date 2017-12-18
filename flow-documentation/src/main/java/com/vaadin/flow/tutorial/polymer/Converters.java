/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.polymer;

import java.io.Serializable;
import java.util.Date;

import com.vaadin.flow.templatemodel.Convert;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-model-converters.asciidoc")
public class Converters {

    public interface MyModel extends TemplateModel {
        @Convert(value = LongToStringConverter.class, path = "id")
        void setPerson(Person person);

        Person getPerson();

        Date getBirthDate();

        @Convert(DateToDateBeanConverter.class)
        void setBirthDate(Date birthDate);
    }

    @Entity
    public class Person implements Serializable {
        @Id
        @GeneratedValue
        private Long id;

        @Temporal(TemporalType.DATE)
        private Date birthDate;

        public Long getId() {
            return id;
        }

    }
}
