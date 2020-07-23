package com.vaadin.flow.data.provider;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HasLazyDataViewTest {

    @Tag("test-component")
    private static class TestComponent extends Component
            implements HasLazyDataView<String, AbstractLazyDataView<String>> {

        private DataCommunicator<String> dataCommunicator;

        public TestComponent() {
            dataCommunicator = new DataCommunicator<>((item, jsonObject) -> {
            }, null, null, getElement().getNode());
        }

        @Override
        public AbstractLazyDataView<String> setItems(
                BackEndDataProvider<String, Void> dataProvider) {
            dataCommunicator.setDataProvider(dataProvider, null);
            return getLazyDataView();
        }

        @Override
        public AbstractLazyDataView<String> getLazyDataView() {
            return new AbstractLazyDataView<String>(dataCommunicator, this) {
            };
        }

        public DataCommunicator<String> getDataCommunicator() {
            return dataCommunicator;
        }
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void setItemsCountCallback_switchesToDefinedSize_throwsOnSizeQuery() {
        TestComponent testComponent = new TestComponent();
        // uses a NOOP count callback that will throw when called
        testComponent.setItems(query -> Stream.of("foo", "bar", "baz"));

        Assert.assertFalse(testComponent.getLazyDataView().getDataCommunicator()
                .isDefinedSize());

        testComponent.getLazyDataView().setItemCountFromDataProvider();

        Assert.assertTrue(testComponent.getLazyDataView().getDataCommunicator()
                .isDefinedSize());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
                "Trying to use exact size with a lazy loading component"
                        + " without either providing a count callback for the"
                        + " component to fetch the count of the items or a data"
                        + " provider that implements the size query. Provide the "
                        + "callback for fetching item count with%n"
                        + "component.getLazyDataView().withDefinedSize(CallbackDataProvider.CountCallback);"
                        + "%nor switch to undefined size with%n"
                        + "component.getLazyDataView().withUndefinedSize();");
        // to make things fail, just need to call size() which will trigger a
        // size query
        //
        // Although we don't have getSize() method for lazy data view, it is
        // still possible for developer to call getItemCount() from
        // dataCommunicator.
        testComponent.getDataCommunicator().getItemCount();
    }

}
