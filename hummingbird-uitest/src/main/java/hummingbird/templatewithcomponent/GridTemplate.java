package hummingbird.templatewithcomponent;

import com.vaadin.annotations.Bower;
import com.vaadin.annotations.Id;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Template;

// Template uses <vaadin-grid>
@Bower("vaadin-grid")
public class GridTemplate extends Template {

    private Button button;
    @Id("button")
    private Button theSameButton;

    private Grid localid; // Lower case i because of #22
    private Grid globalId; // Should be same instance as localid

    public GridTemplate() {

    }

    @Override
    public void attach() {
        super.attach();

        button.addClickListener(e -> {
            Notification.show("Hello!");
        });
        theSameButton.addStyleName("foo");

        localid.addColumn("Hello");
        globalId.addColumn("World");
    }

}
