package com.vaadin.flow.data.provider.hierarchy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;

import elemental.json.JsonArray;

public class HierarchicalDataCommunicatorTest {
    // private static class Item {
    //     private String name;

    //     public Item(String name) {
    //         this.name = name;
    //     }

    //     public String getName() {
    //         return name;
    //     }
    // }

    // private MockUI ui = new MockUI();
    // private Element element = new Element("div");
    // private Update arrayUpdate = mock(Update.class);
    // private ArrayUpdater arrayUpdater = mock(ArrayUpdater.class);
    // private SerializableConsumer<JsonArray> dataUpdater = (jsonArray) -> {};
    // private CompositeDataGenerator<Item> dataGenerator = new CompositeDataGenerator<>();
    // private HierarchicalDataCommunicator<Item> communicator = new HierarchicalDataCommunicator<>(
    //         dataGenerator, arrayUpdater, dataUpdater, element.getNode(), () -> null);

    // @Before
    // public void init() {
    //     ui.getElement().appendChild(element);

    //     when(arrayUpdater.startUpdate(Mockito.anyInt()))
    //             .thenAnswer((answer) -> arrayUpdate);
    // }

    // @After
    // public void tearDown() {
    //     UI.setCurrent(null);
    // }

    // @Test
    // public void flush_initialRangeSent() {
    //     fakeClientCommunication();
    // }

    // @Test
    // public void setViewportRange() {

    // }

    // private void fakeClientCommunication() {
    //     ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    //     ui.getInternals().getStateTree().collectChanges(ignore -> {
    //     });
    // }
}
