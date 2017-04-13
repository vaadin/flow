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

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.Uses;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.uitest.ui.webcomponent.PaperSlider;
import com.vaadin.flow.uitest.ui.webcomponent.ProgressBubble;
import com.vaadin.ui.AngularTemplate;

@Uses(PaperSlider.class)
@Uses(ProgressBubble.class)
public class WebComponentsInTemplate extends AngularTemplate {

    public interface Model extends TemplateModel {
        public int getValue();

        public void setValue(int value);

        public int getMax();

        public void setMax(int max);
    }

    @Override
    protected Model getModel() {
        return (Model) super.getModel();
    }

    @EventHandler
    public void setValue(int value) {
        getModel().setValue(value);
    }

    public WebComponentsInTemplate() {
        getModel().setValue(35);
        getModel().setMax(100);

    }
}
