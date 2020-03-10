package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinRequest;

@Push
public class MakeComponentVisibleWithPushUI extends UI {

    private Div rootLayout;
    private Input input;
    private SearchThread searchThread;

    @Override
    protected void init(VaadinRequest request) {
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        Push push = getClass().getAnnotation(Push.class);

        getPushConfiguration().setPushMode(push.value());
        getPushConfiguration().setTransport(push.transport());

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
