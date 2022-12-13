package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@CustomPush
@Route("com.vaadin.flow.uitest.ui.push.MakeComponentVisibleWithPushView")
public class MakeComponentVisibleWithPushView extends Div {

    private Div rootLayout;
    private Input input;
    private SearchThread searchThread;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        CustomPush push = getClass().getAnnotation(CustomPush.class);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(push.value());
        ui.getPushConfiguration().setTransport(push.transport());

        rootLayout = new Div();
        add(rootLayout);

        input = new Input();
        input.setVisible(false);
        input.setValue("foo");
        input.setId("input");
        rootLayout.add(input);

        NativeButton doUpdateButton = new NativeButton("Do Update",
                event -> doUpdate());
        doUpdateButton.setId("update");

        rootLayout.add(doUpdateButton);
    }

    private void doUpdate() {

        cancelSuggestThread();

        input.setVisible(false);

        UI ui = UI.getCurrent();
        searchThread = new SearchThread(ui);
        searchThread.start();
    }

    class SearchThread extends Thread {
        private UI ui;

        public SearchThread(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {

            if (!searchThread.isInterrupted()) {
                ui.access(() -> {
                    input.setValue(input.getValue() + "bar");
                    input.setVisible(true);
                });
            }
        }

    }

    private void cancelSuggestThread() {

        if ((searchThread != null) && !searchThread.isInterrupted()) {
            searchThread.interrupt();
            searchThread = null;
        }
    }

}
