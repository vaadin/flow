package hummingbird;

import java.util.Collections;

import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.ComputedProperty;
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
        node.setComputedProperties(Collections.singletonMap("foo",
                new ComputedProperty("foo", null) {
                    @Override
                    public Object compute(StateNode context) {
                        return String.valueOf(context.get("count", 0));
                    }
                }));

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
