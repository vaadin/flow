package hummingbird.templatewithcomponent;

import com.vaadin.annotations.Bower;
import com.vaadin.annotations.Id;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.elements.core.grid.Grid;
import com.vaadin.ui.Button;
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

        TestIndexedContainer ic = new TestIndexedContainer("First", "Last");
        ic.addRow("Artur", "Signell");
        ic.addRow("Olli", "Tietäväinen");
        ic.addRow("Leif", "Åstrand");
        globalId.setContainerDataSource(ic);

        localid.addSelectionListener(e -> {
            if (localid.getSelectedRow() == null) {
                Notification.show("Deselected "
                        + getName(ic, e.getRemoved().iterator().next()));
            } else {
                Notification.show(
                        "Selected: " + getName(ic, localid.getSelectedRow()));
            }
        });
    }

    private String getName(Container c, Object itemId) {
        Item item = c.getItem(itemId);
        return item.getItemProperty("First").getValue() + " "
                + item.getItemProperty("Last").getValue();
    }

    public static class TestIndexedContainer extends IndexedContainer {

        public TestIndexedContainer(Object... propertyIds) {
            super();
            for (Object propertyId : propertyIds) {
                addContainerProperty(propertyId, String.class, "");
            }
        }

        public void addRow(Object... rowData) {
            int i = 0;
            Object itemId = addItem();
            for (Object propertyId : getContainerPropertyIds()) {
                getItem(itemId).getItemProperty(propertyId)
                        .setValue(rowData[i++]);
            }
        }

    }

}
