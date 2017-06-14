package com.vaadin.flow.demo;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.demo.views.DemoView;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.html.Anchor;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.View;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Component;

@Tag("main-layout")
@HtmlImport("frontend://src/main-layout.html")
public class MainLayout extends Component implements HasChildView {

    @Id("selector")
    private Element selector;

    private View selectedView;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            appendViewAnchor("paper-button", "Paper Button");
            appendViewAnchor("paper-input", "Paper Input");
        }
    }

    private void appendViewAnchor(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.getElement().setProperty("slot", "selectors");
        anchor.getElement().setProperty("name", text);
        anchor.getElement().setAttribute("router-link", true);
        getElement().appendChild(anchor.getElement());
    }

    @Override
    public void setChildView(View childView) {
        if (selectedView == childView) {
            return;
        }
        if (selectedView != null) {
            selectedView.getElement().removeFromParent();
        }
        selectedView = childView;

        // uses the <slot> at the template
        getElement().appendChild(childView.getElement());
        if (childView instanceof DemoView) {
            getElement().setProperty("page",
                    ((DemoView) childView).getViewName());
        }
    }

}
