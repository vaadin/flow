package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.templatemodel.TemplateModel;

@Route("template-push")
@Push(transport = Transport.WEBSOCKET)
@HtmlImport("TemplatePushView.html")
@Tag("template-push-view")
public class TemplatePushView extends PolymerTemplate<TemplateModel> {

    @Id
    private Label label;

    @Id
    private NativeButton elementTest;
    @Id
    private NativeButton execJsTest;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();

        elementTest.addClickListener(e -> {
            new Thread(new ElementAPI(ui)).start();
        });

        execJsTest.addClickListener(e -> {
            new Thread(new ExecJS(ui)).start();
        });

    }

    private static abstract class Cmd implements Runnable {
        private final UI ui;

        public Cmd(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {
            sleep();
            ui.access(() -> execute(ui));
        }

        private void sleep() {
            try {
                // Needed to make sure that this is sent as a push message
                Thread.sleep(100);
            } catch (InterruptedException e1) {
            }

        }

        protected abstract void execute(UI ui);

    }

    private class ExecJS extends Cmd {

        public ExecJS(UI ui) {
            super(ui);
        }

        @Override
        protected void execute(UI ui) {
            ui.getPage().executeJavaScript("$0.innerText='from execJS'", label);
        }

    }

    private class ElementAPI extends Cmd {
        private ElementAPI(UI ui) {
            super(ui);
        }

        @Override
        protected void execute(UI ui) {
            label.getElement().setText("from Element API");
        }

    }

}
