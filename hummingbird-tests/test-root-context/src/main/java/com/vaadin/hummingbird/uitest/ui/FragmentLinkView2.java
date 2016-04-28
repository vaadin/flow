package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;

public class FragmentLinkView2 extends FragmentLinkView {

    public FragmentLinkView2() {
        getElement().insertChild(0,
                new Element("div").setTextContent("VIEW 2"));
    }

    @Override
    protected void onAttach() {
        // do not call super onAttach since it adds a hashchangelistener
    }
}
