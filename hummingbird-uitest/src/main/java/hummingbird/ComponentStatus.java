package hummingbird;

import java.util.List;

import com.vaadin.annotations.Implemented;
import com.vaadin.annotations.NotYetImplemented;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.VaadinClasses;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.UI;

public class ComponentStatus extends UI {

    @Override
    protected void init(VaadinRequest request) {
        Grid grid = new Grid();
        grid.setSizeFull();

        grid.addColumn("Component");
        grid.addColumn("Status");
        grid.addColumn("Comment");

        for (Class<?> c : getComponents()) {
            Implemented impl = c.getAnnotation(Implemented.class);
            NotYetImplemented notImpl = c
                    .getAnnotation(NotYetImplemented.class);
            String status;
            String comment = null;
            if (impl != null) {
                status = "Implemented";
                comment = impl.value();
            } else if (notImpl != null) {
                status = "Not implemented";
                comment = notImpl.value();
            } else {
                System.out.println("Component without status: " + c.getName());
                continue;
            }

            grid.addRow(c.getSimpleName(), status, comment);

        }

        addComponent(grid);
    }

    private List<Class<? extends Component>> getComponents() {
        return VaadinClasses.getComponents();
    }

}
