package hummingbird.templatewithcomponent;

import com.vaadin.annotations.Bower;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Template;

// Template uses <vaadin-grid>
@Bower("vaadin-grid")
public class GridTemplate extends Template {

    public GridTemplate() {

    }

    @Override
    public void attach() {
        super.attach();

        Element buttonElement = findElement(getElement(), "button");
        Button button = new Button();
        AbstractComponent.mapComponent(button, buttonElement);
        System.out.println(
                "Button template id: " + buttonElement.getTemplate().getId());
        button.addStyleName("foo");

        button.addClickListener(e -> {
            Notification.show("Hello!");
        });

    }

    private Element findElement(Element element, String tag) {
        if (element.getTag().equals(tag)) {
            return element;
        }

        for (int i = 0; i < element.getChildCount(); i++) {
            Element found = findElement(element.getChild(i), tag);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
}
