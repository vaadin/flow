package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.event.AttachEvent;

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
