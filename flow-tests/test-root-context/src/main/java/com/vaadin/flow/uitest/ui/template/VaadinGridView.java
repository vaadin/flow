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
package com.vaadin.flow.uitest.ui.template;

import java.util.Arrays;
import java.util.List;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.uitest.ui.template.VaadinGridView.GridModel;

@Tag("vaadin-grid-view")
@HtmlImport("/com/vaadin/flow/uitest/ui/template/VaadinGrid.html")
public class VaadinGridView extends PolymerTemplate<GridModel> implements View {

    public interface GridModel extends TemplateModel {

        void setPatients(List<Patient> patients);

        Patient getActivePatient();

        void setSelected(String name);

        void setMessages(List<String> messages);

        String getActiveMessage();
    }

    public static class Patient {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public VaadinGridView() {
        getModel().setPatients(
                Arrays.asList(createPatient("foo"), createPatient("bar")));

        getModel().setMessages(Arrays.asList("baz", "msg"));

        getElement().addPropertyChangeListener("activePatient",
                event -> patientSelected());

        getElement().addPropertyChangeListener("activeMessage",
                event -> messageSelected());
    }

    private Patient createPatient(String name) {
        Patient patient = new Patient();
        patient.setName(name);
        return patient;
    }

    private void patientSelected() {
        Patient patient = getModel().getActivePatient();
        getModel().setSelected(patient.getName());
    }

    private void messageSelected() {
        String msg = getModel().getActiveMessage();
        getModel().setSelected(msg);
    }

}
