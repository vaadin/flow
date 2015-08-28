package hummingbird;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class RemoveRemovedElement extends UI {
    @Override
    protected void init(VaadinRequest request) {
        Element label = new Element("#text").setAttribute("content", "Label");
        Element removeButton = new Element("button")
                .setTextContent("Remove label");
        removeButton.addEventListener("click", e -> label.removeFromParent());

        getElement().appendChild(label);
        getElement().appendChild(removeButton);

        getRoot().getRootNode().enqueueRpc(removeButton.getNode(),
                "$0.addEventListener('click', function() { $1.remove(); })",
                removeButton, label);
    }
}
