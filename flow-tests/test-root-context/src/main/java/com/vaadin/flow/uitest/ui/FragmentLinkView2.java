package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.FragmentLinkView2", layout = ViewTestLayout.class)
public class FragmentLinkView2 extends FragmentLinkView {

    public FragmentLinkView2() {
        getElement().insertChild(0, new Element("div").setText("VIEW 2")
                .setAttribute("id", "view2"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // do not call super onAttach since it adds a hashchangelistener
    }
}
