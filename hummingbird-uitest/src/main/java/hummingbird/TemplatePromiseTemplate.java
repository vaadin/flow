package hummingbird;

import java.util.concurrent.ThreadLocalRandom;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.ui.Template;

public class TemplatePromiseTemplate extends Template {
    @TemplateEventHandler
    public String doSomething(String input) {
        if (ThreadLocalRandom.current().nextDouble() > 0.8d) {
            throw new RuntimeException("Though luck");
        }
        return input.toUpperCase();
    }

}