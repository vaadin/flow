package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.ui.History.HistoryStateChangeHandler;

public class FragmentLinkView extends AbstractDivView {

    public FragmentLinkView() {
        Element bodyElement = getElement();
        bodyElement.getStyle().set("margin", "1em");

        Element scrollLocator = ElementFactory.createDiv()
                .setAttribute("id", "scrollLocator")
                .setTextContent("Scroll locator");
        scrollLocator.getStyle().set("position", "fixed").set("top", "0")
                .set("right", "0");

        Element placeholder = ElementFactory.createDiv("Hash Change Events")
                .setAttribute("id", "placeholder");

        bodyElement.appendChild(scrollLocator, placeholder, new Element("p"));

        Element scrollToLink = ElementFactory.createRouterLink(
                "/view/com.vaadin.hummingbird.uitest.ui.FragmentLinkView#Scroll_Target",
                "Scroller link");
        Element scrollToLink2 = ElementFactory.createRouterLink(
                "/view/com.vaadin.hummingbird.uitest.ui.FragmentLinkView#Scroll_Target2",
                "Scroller link 2");
        Element scrollToLinkAnotherView = ElementFactory.createRouterLink(
                "/view/com.vaadin.hummingbird.uitest.ui.FragmentLinkView2#Scroll_Target",
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
    protected void onAttach() {
        getUI().get().getPage()
                .executeJavaScript("var i = 0;"
                        + "window.addEventListener('hashchange', function(event) {"
                        + "var x = document.createElement('span');"
                        + "x.textContent = ' ' + i;" + "i++;"
                        + "x.class = 'hashchange';"
                        + "document.getElementById('placeholder').appendChild(x);},"
                        + " false);");

        HistoryStateChangeHandler current = getUI().get().getPage().getHistory()
                .getHistoryStateChangeHandler();
        getUI().get().getPage().getHistory()
                .setHistoryStateChangeHandler(event -> {
                    if (event.getLocation().equals("override")) {
                        event.getSource().replaceState(null,
                                "overridden#Scroll_Target2");
                    } else {
                        current.onHistoryStateChange(event);
                    }
                });
    }

    private Element createSpacer() {
        Element spacer = ElementFactory.createDiv().setTextContent("spacer");
        spacer.getStyle().set("height", "1000px");
        return spacer;
    }

}
