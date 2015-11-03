package hummingbird.template;

import java.util.List;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.annotations.TemplateHTML;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

public class ListInStateNode extends UI {

    @TemplateHTML("ItemsLoop.html")
    public static class ListInStateNodeTemplate extends Template {
        int counter = 0;
        int earlierCounter = -1;

        private List<Object> list;

        public List<Object> getList() {
            return list;
        }

        public ListInStateNodeTemplate() {
            list = getElement().getNode().getMultiValued("items");
        }

        public interface MyModel extends Template.Model {
        }

        @Override
        protected MyModel getModel() {
            return (MyModel) super.getModel();
        }

        @TemplateEventHandler
        public void extend() {
            for (int i = 0; i < 2; i++) {
                StateNode sn = StateNode.create();
                sn.put("value", "Value " + counter++);
                list.add(sn);
            }
        }

        public void prepend() {
            for (int i = 0; i < 2; i++) {
                StateNode sn = StateNode.create();
                sn.put("value", "Value " + earlierCounter--);
                list.add(0, sn);
            }

        }

    }

    @Override
    protected void init(VaadinRequest request) {

        ListInStateNodeTemplate template = new ListInStateNodeTemplate();
        template.extend();

        addComponent(template);
        addComponent(new Button("Show more...", e -> {
            template.extend();
        }));
        addComponent(new Button("Show earlier...", e -> {
            template.prepend();
        }));
    }

}
