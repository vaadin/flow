package com.vaadin.flow.spring.test;

import java.util.concurrent.locks.Lock;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
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
@JsModule("TemplatePushView.js")
@Tag("template-push-view")
public class TemplatePushView extends PolymerTemplate<TemplateModel> {

    @Id
    private Label label;

    @Id
    private NativeButton elementTest;
    @Id
    private NativeButton execJsTest;
    @Id
    private NativeButton callFunctionTest;

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
        ui.getPage().executeJs(
                "$0.setText = function(text) {$0.innerText=text;}", label);
        callFunctionTest.addClickListener(e -> {
            new Thread(new CallFunction(ui)).start();
        });

    }

    private static abstract class Cmd implements Runnable {
        private final UI ui;

        public Cmd(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {
            // We can acquire the lock after the request started this thread is processed
            // Needed to make sure that this is sent as a push message
            Lock lockInstance = ui.getSession().getLockInstance();
            lockInstance.lock();
            lockInstance.unlock();

            ui.access(() -> execute(ui));
        }

        protected abstract void execute(UI ui);

    }

    private class CallFunction extends Cmd {

        public CallFunction(UI ui) {
            super(ui);
        }

        @Override
        protected void execute(UI ui) {
            label.getElement().callJsFunction("setText", "from callFunction");
        }

    }

    private class ExecJS extends Cmd {

        public ExecJS(UI ui) {
            super(ui);
        }

        @Override
        protected void execute(UI ui) {
            ui.getPage().executeJs("$0.innerText='from execJS'", label);
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
