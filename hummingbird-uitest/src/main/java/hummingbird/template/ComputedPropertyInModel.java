package hummingbird.template;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.annotations.TemplateHTML;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Template;
import com.vaadin.ui.Template.Model;
import com.vaadin.ui.UI;

public class ComputedPropertyInModel extends UI {
    public interface MyModel extends Model {
        public int getValue();

        public void setValue(int value);

        public default int getSquaredJava() {
            int value = getValue();
            return value * value;
        }
    }

    @TemplateHTML("ComputedPropertyInModel.html")
    public static class MyTempalte extends Template {
        @Override
        protected void init() {
            getModel().setValue(2);
        }

        @Override
        protected MyModel getModel() {
            return (MyModel) super.getModel();
        }

        @TemplateEventHandler
        public void incrementValue() {
            getModel().setValue(getModel().getValue() + 1);
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        MyTempalte template = new MyTempalte();
        addComponent(template);
    }

}
