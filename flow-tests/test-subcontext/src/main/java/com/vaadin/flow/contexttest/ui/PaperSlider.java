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
package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.router.Route;

@Tag("paper-slider")
@HtmlImport("context://frontend-resource/frontend/bower_components/paper-slider/paper-slider.html")
@Route("slider")
public class PaperSlider extends Component {
    @DomEvent("value-change")
    public static class ValueChangeEvent extends ComponentEvent<PaperSlider> {
        public ValueChangeEvent(PaperSlider source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public PaperSlider() {
        getElement().synchronizeProperty("value", "value-change");
    }

}
