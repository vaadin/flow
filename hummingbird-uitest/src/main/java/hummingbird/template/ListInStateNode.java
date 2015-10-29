package hummingbird.template;

import java.util.List;

import com.vaadin.annotations.TemplateHTML;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

public class ListInStateNode extends UI {

    @TemplateHTML("ItemsLoop.html")
    public static class ListInStateNodeTemplate extends Template {

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
    }

    int counter = 0;
    int earlierCounter = -1;

    @Override
    protected void init(VaadinRequest request) {

        ListInStateNodeTemplate listInStateNodeTemplate = new ListInStateNodeTemplate();
        List<Object> list = listInStateNodeTemplate.getList();
        for (int i = 0; i < 2; i++) {
            StateNode sn = StateNode.create();
            sn.put("value", "Value " + counter++);
            list.add(sn);
        }
        addComponent(listInStateNodeTemplate);
        addComponent(new Button("Show more...", e -> {
            for (int i = 0; i < 2; i++) {
                StateNode sn = StateNode.create();
                sn.put("value", "Value " + counter++);
                list.add(sn);
            }
        }));
        addComponent(new Button("Show earlier...", e -> {
            for (int i = 0; i < 2; i++) {
                StateNode sn = StateNode.create();
                sn.put("value", "Value " + earlierCounter--);
                list.add(0, sn);
            }
        }));
    }

}
