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
        public AbstractLazyDataView<String> setDataSource(BackEndDataProvider<String, Void> dataProvider) {
            dataCommunicator.setDataProvider(dataProvider,null);
            return getLazyDataView();
        }

        @Override
        public AbstractLazyDataView<String> getLazyDataView() {
            return new AbstractLazyDataView<String>(dataCommunicator, this) {
            };
        }
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void setDataSourceCountCallback_switchesToDefinedSize_throwsOnSizeQuery() {
        TestComponent testComponent = new TestComponent();
        // uses a NOOP count callback that will throw when called
        testComponent.setDataSource(query -> Stream.of("foo","bar","baz"));

        Assert.assertFalse(testComponent.getLazyDataView().getDataCommunicator().isDefinedSize());

        testComponent.getLazyDataView().setRowCountFromDataProvider();

        Assert.assertTrue(testComponent.getLazyDataView().getDataCommunicator().isDefinedSize());

        expectedException.expect(IllegalStateException.class);
        // to make things fail, just need to call size() which will trigger a size query
        testComponent.getLazyDataView().getSize();
    }

}
