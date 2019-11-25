/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Tests a scalability bug #5806 with adding many buttons to a view.
 */
@Push(transport = Transport.LONG_POLLING)
@Tag("template-scalability-view")
@HtmlImport("template-scalability-view.html")
@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateScalabilityView", layout = ViewTestLayout.class)
@PageTitle("Template scalability")
public class TemplateScalabilityView extends PolymerTemplate<TemplateModel> {

    @Id("content")
    private Div div;

    private Thread worker;
    private volatile boolean stopped;
    private UI ui;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        ui = getUI().orElseThrow(RuntimeException::new);
        stopped = false;
        worker = new Thread(this::doWork);
        worker.start();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        stopped = true;

        try {
            worker.join(10000);
            worker = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        super.onDetach(detachEvent);
    }

    private void doWork() {
        while (!stopped) {
            try {
                Thread.sleep(2000);

                ui.access(() -> {
                    div.removeAll();

                    for (int i = 0; i < 50; ++i) {
                        TemplateScalabilityPanel p = new TemplateScalabilityPanel(
                                "Panel " + i);
                        div.add(p);
                    }
                    Div complete = new Div();
                    complete.setId("completed");
                    div.add(complete);
                });

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

}
