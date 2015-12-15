package hummingbird.template;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.annotations.TemplateHTML;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Template;
import com.vaadin.ui.Template.Model;

public class ComputedPropertyInTemplate extends AbstractTestUIWithLog {
    public interface MyModel extends Model {
        public void setValue(int value);

        public int getValue();
    }

    @TemplateHTML("ComputedPropertyInTemplate.html")
    public class MyTemplate extends Template {
        public MyTemplate() {
            getModel().setValue(3);
        }

        @Override
        protected MyModel getModel() {
            return (MyModel) super.getModel();
        }

        @TemplateEventHandler
        protected void log() {
            ComputedPropertyInTemplate.this.log("Server-side value: "
                    + getElementById("squared").getTextContent());
        }
    }

    @Override
    protected void setup(VaadinRequest request) {
        addComponent(new MyTemplate());
    }

}
