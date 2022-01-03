package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "com.vaadin.flow.uitest.ui.InertComponentView")
public class InertComponentView extends Div {

    static final String LINK = "link-to-another-view";
    static final String NEW_BOX = "new-box";
    static final String BOX = "box";
    static final String NEW_MODAL_BOX = "new-modal-box";
    static final String REMOVE = "remove";
    private static int boxCounter;

    public InertComponentView() {
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        boxCounter = 0;

        super.onAttach(attachEvent);

        // collapse outlet so that the added boxes are rendered inside visible
        // browser window
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "document.getElementById('outlet').style.height = 'auto';"));

        add(new Box(false));
    }

    private static class Box extends Div {
        public Box(boolean modal) {
            boxCounter++;

            withId(BOX, this);

            add(new Text(boxCounter + " " + (modal ? "modal" : "not modal")
                    + " Box"));
            add(withId(REMOVE, new NativeButton("Remove",
                    event -> getElement().removeFromParent())));
            add(withId(NEW_BOX, new NativeButton("New box",
                    event -> getUI().ifPresent(ui -> ui.add(new Box(false))))));
            add(withId(NEW_MODAL_BOX,
                    new NativeButton("New Modal Box", event -> getUI()
                            .ifPresent(ui -> ui.addModal(new Box(true))))));
            add(withId(LINK, new RouterLink("Link to another view",
                    ModalDialogView.class)));

            getStyle().set("border", "1px solid pink");
            if (modal) {
                getStyle().set("background-color", "silver");
            }
        }

        private Component withId(String id, Component component) {
            component.setId(id + "-" + boxCounter);
            return component;
        }
    }
}
