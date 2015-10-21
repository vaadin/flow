package hummingbird;

import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.TemplateBuilder;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

public class SimpleComputedProperty extends AbstractTestUI {

    @Override
    protected void setup(VaadinRequest request) {
        BoundElementTemplate template = TemplateBuilder.withTag("div")
                .addChild(TemplateBuilder.dynamicText("foo")).build();

        StateNode node = StateNode.create();

        node.putComputed("foo", () -> {
            return String.valueOf(node.get("count", 0));
        });

        getLayout().getElement()
                .appendChild(Element.getElement(template, node));

        addComponent(new Button("Increment", new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                Integer newCount = Integer.valueOf(node.get("count", 0) + 1);
                node.put("count", newCount);
            }
        }));
    }

}
