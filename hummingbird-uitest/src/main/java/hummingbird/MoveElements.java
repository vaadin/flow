package hummingbird;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class MoveElements extends UI {
    private final Element text = new Element("#text").setAttribute("content",
            "Child");

    @Override
    protected void init(VaadinRequest request) {
        Element blueParent = createElement("blue");
        Element redParent = createElement("red");

        getElement().appendChild(createButton("Move to blue", blueParent));
        getElement().appendChild(createButton("Move to red", redParent));
        getElement().appendChild(createButton("Move out", getElement()));

        getElement().appendChild(blueParent);
        getElement().appendChild(redParent);
        getElement().appendChild(text);
    }

    private Element createButton(String text, Element moveTo) {
        Element button = new Element("button");
        button.setTextContent(text);
        button.addEventListener("click", e -> moveTo.appendChild(this.text));
        return button;
    }

    private Element createElement(String color) {
        Element element = new Element("div");
        element.setStyle("height", "100px");
        element.setStyle("width", "400px");
        element.setStyle("background", color);
        return element;
    }
}
