package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.WildcardParameter;

public class RootContextLayout extends Div implements RouterLayout {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        PushUtil.setupPush();
    }

    @Route(value = "", layout = RootContextLayout.class)
    public static class RootSubLayout extends DependencyLayout {
        public RootSubLayout() {
            getElement().appendChild(ElementFactory.createDiv("Root Layout")
                    .setAttribute("id", "root"));
        }

    }

    @Route(value = "sub-context", layout = RootContextLayout.class)
    public static class SubContextLayout extends DependencyLayout
            implements HasUrlParameter<String> {

        public SubContextLayout() {
            getElement().appendChild(ElementFactory
                    .createDiv("Sub Context Layout").setAttribute("id", "sub"));
        }

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            // ignored
        }
    }

}
