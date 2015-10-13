package hummingbird;

import java.util.List;

import com.vaadin.annotations.Implemented;
import com.vaadin.annotations.NotYetImplemented;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
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

        IndexedContainer ic = new IndexedContainer();
        ic.addContainerProperty("Component", String.class, "");
        ic.addContainerProperty("Status", String.class, "");
        ic.addContainerProperty("Comment", String.class, "");

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
                if (notImpl != null) {
                    comment = notImpl.value();
                }
            } else {
                System.out.println("Component without status: " + c.getName());
                continue;
            }

            Item item = ic.addItem(c);
            item.getItemProperty("Component").setValue(c.getSimpleName());
            item.getItemProperty("Status").setValue(status);
            item.getItemProperty("Comment").setValue(comment);
        }
        grid.setContainerDataSource(ic);

        addComponent(grid);
    }

    private List<Class<? extends Component>> getComponents() {
        return VaadinClasses.getComponents();
    }

}
