package hummingbird.component.grid;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.elements.core.grid.Grid;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Button;

public class GridContainerData extends AbstractTestUIWithLog {

    private Grid grid;
    private int generatedId = 1;

    public static class Person {
        private String first, last;
        private int age;
        private boolean alive;

        public Person(String first, String last, int age, boolean alive) {
            super();
            this.first = first;
            this.last = last;
            this.age = age;
            this.alive = alive;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isAlive() {
            return alive;
        }

        public void setAlive(boolean alive) {
            this.alive = alive;
        }

    }

    @Override
    protected void setup(VaadinRequest request) {
        grid = new Grid();
        BeanItemContainer<Person> bic = new BeanItemContainer<>(Person.class);
        bic.addBean(new Person("Artur", "Signell", 35, true));
        grid.setContainerDataSource(bic);
        Button addRow = new Button("Add row", e -> {
            BeanItemContainer<Person> c = (BeanItemContainer<Person>) grid
                    .getContainerDataSource();
            if (generatedId == 5) {
                log("Set height to 5 rows");
                grid.setHeightByRows(5);

            }
            Person p = new Person("First " + generatedId, "Last " + generatedId,
                    generatedId, generatedId % 3 == 0);
            c.addBean(p);
            generatedId++;
            log("Add row");
        });

        Button removeRow = new Button("Remove selected", e -> {
            BeanItemContainer<Person> c = (BeanItemContainer<Person>) grid
                    .getContainerDataSource();
            if (grid.getSelectedRow() != null) {
                c.removeItem(grid.getSelectedRow());
                log("Removed row");
            } else {
                log("No row selected");
            }
        });

        Button updateRow = new Button("Update selected row", e -> {
            BeanItemContainer<Person> c = (BeanItemContainer<Person>) grid
                    .getContainerDataSource();

            if (grid.getSelectedRow() != null) {
                Item item = c.getItem(grid.getSelectedRow());
                for (Object pid : item.getItemPropertyIds()) {
                    Property property = item.getItemProperty(pid);
                    if (property.getType() == String.class) {
                        property.setValue(property.getValue() + ".");
                    }
                }
                log("Updated row");
            } else {
                log("No row selected");
            }
        });

        Button changeContainer = new Button("Replace container", e -> {
            BeanItemContainer<Person> newBic = new BeanItemContainer<>(
                    Person.class);
            newBic.addBean(new Person("New first", "NEw last", 12, true));
            newBic.addBean(new Person("New second first", "New second last", 88,
                    false));

            grid.setContainerDataSource(newBic);
        });

        addComponent(addRow);
        addComponent(removeRow);
        addComponent(updateRow);
        addComponent(changeContainer);
        addComponent(grid);

        log("Initial render");
    }

}
