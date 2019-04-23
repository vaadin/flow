package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.History.HistoryStateChangeHandler;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.FragmentLinkView", layout = ViewTestLayout.class)
public class FragmentLinkView extends AbstractDivView {

    public FragmentLinkView() {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element scrollLocator = ElementFactory.createDiv()
                .setAttribute("id", "scrollLocator").setText("Scroll locator");
        scrollLocator.getStyle().set("position", "fixed").set("top", "0")
                .set("right", "0");

        Element placeholder = ElementFactory.createDiv("Hash Change Events")
                .setAttribute("id", "placeholder");

        bodyElement.appendChild(scrollLocator, placeholder, new Element("p"));

        Element scrollToLink = ElementFactory.createRouterLink(
                "/view/com.vaadin.flow.uitest.ui.FragmentLinkView#Scroll_Target",
                "Scroller link");
        Element scrollToLink2 = ElementFactory.createRouterLink(
                "/view/com.vaadin.flow.uitest.ui.FragmentLinkView#Scroll_Target2",
                "Scroller link 2");
        Element scrollToLinkAnotherView = ElementFactory.createRouterLink(
                "/view/com.vaadin.flow.uitest.ui.FragmentLinkView2#Scroll_Target",
                "Scroller link with different view");
        Element linkThatIsOverridden = ElementFactory.createRouterLink(
                "./override#Scroll_Target", "Link that server overrides");

        Element scrollTarget = ElementFactory.createHeading1("Scroll Target")
                .setAttribute("id", "Scroll_Target");
        Element scrollTarget2 = ElementFactory.createHeading2("Scroll Target 2")
                .setAttribute("id", "Scroll_Target2");

        bodyElement.appendChild(scrollToLink, new Element("p"), scrollToLink2,
                new Element("p"), scrollToLinkAnotherView, new Element("p"),
                linkThatIsOverridden, new Element("p"), createSpacer(),
                scrollTarget, createSpacer(), scrollTarget2, createSpacer());

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Page page = attachEvent.getUI().getPage();
        page.executeJs("var i = 0;"
                + "window.addEventListener('hashchange', function(event) {"
                + "var x = document.createElement('span');"
                + "x.textContent = ' ' + i;" + "i++;"
                + "x.class = 'hashchange';"
                + "document.getElementById('placeholder').appendChild(x);},"
                + " false);");

        HistoryStateChangeHandler current = page.getHistory()
                .getHistoryStateChangeHandler();
        page.getHistory().setHistoryStateChangeHandler(event -> {
            if (event.getLocation().getPath().equals("override")) {
                event.getSource().replaceState(null,
                        "overridden#Scroll_Target2");
            } else {
                current.onHistoryStateChange(event);
            }
        });
    }

    private Element createSpacer() {
        Element spacer = ElementFactory.createDiv().setText("spacer");
        spacer.getStyle().set("height", "1000px");
        return spacer;
    }

}
