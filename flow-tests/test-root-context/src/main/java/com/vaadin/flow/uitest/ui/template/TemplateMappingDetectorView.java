/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateMappingDetectorView", layout = ViewTestLayout.class)
public class TemplateMappingDetectorView extends AbstractDivView {

    @Tag("div")
    public static class TemplateMappingDetector extends Component
            implements HasText {

        public TemplateMappingDetector() {
            setText("Template mapped: " + isTemplateMapped());
        }

    }

    public static class TemplateMappingDetectorComposite
            extends Composite<TemplateMappingDetector> {

        @Override
        protected TemplateMappingDetector initContent() {
            TemplateMappingDetector detector = super.initContent();
            detector.setText("Composite template mapped: " + isTemplateMapped()
                    + " " + detector.getText());
            return detector;
        }

    }

    @Tag("template-mapping-detector")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/TemplateMappingDetector.html")
    @JsModule("TemplateMappingDetector.js")
    public static class TemplateMappingDetectorContainer
            extends PolymerTemplate<TemplateModel> {
        @Id
        TemplateMappingDetector detector1;

        // Disabled due to https://github.com/vaadin/flow/issues/3104

        // @Id
        // TemplateMappingDetectorComposite detector2;

        @Id
        Div container;

        public TemplateMappingDetectorContainer() {
            TemplateMappingDetector detector3 = new TemplateMappingDetector();
            detector3.setId("detector2");
            TemplateMappingDetectorComposite detector4 = new TemplateMappingDetectorComposite();
            detector4.setId("detector3");
            Div detector5 = new Div();
            detector5.setId("detector4");
            detector5.setText("The template itself: " + isTemplateMapped());
            container.add(detector3, detector4, detector5);
        }
    }

    @Tag("template-mapping-detector-parent")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/TemplateMappingDetectorParent.html")
    @JsModule("TemplateMappingDetectorParent.js")
    public static class TemplateMappingDetectorContainerParent
            extends PolymerTemplate<TemplateModel> {
        @Id
        TemplateMappingDetectorContainer detector;
    }

    public static class TemplateMappingDetectorContainerComposite
            extends Composite<TemplateMappingDetectorContainer> {
    }

    @Override
    protected void onShow() {
        TemplateMappingDetectorContainer container = new TemplateMappingDetectorContainer();
        TemplateMappingDetectorContainerParent containerParent = new TemplateMappingDetectorContainerParent();
        TemplateMappingDetectorContainerComposite composite = new TemplateMappingDetectorContainerComposite();
        composite.setId("composite");
        add(container, new Hr(), containerParent, new Hr(), composite);
    }

}
