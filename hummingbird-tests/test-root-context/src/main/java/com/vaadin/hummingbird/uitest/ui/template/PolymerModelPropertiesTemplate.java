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
package com.vaadin.hummingbird.uitest.ui.template;

import java.util.List;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.hummingbird.uitest.ui.template.PolymerModelPropertiesTemplate.ModelProperties;

@Tag("model-properties")
@HtmlImport("/com/vaadin/hummingbird/uitest/ui/template/ModelProperties.html")
public class PolymerModelPropertiesTemplate
        extends PolymerTemplate<ModelProperties> {

    public interface ModelProperties extends TemplateModel {

        void setName(String name);

        void setCity(String city);

        void setVisible(boolean visible);

        void setEnable(Boolean enable);

        void setAge(int age);

        void setHeight(Double height);

        void setList(List<Integer> list);
    }
}
