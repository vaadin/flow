package com.vaadin.flow.template.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.annotations.Convert;
import com.vaadin.flow.template.model.TemplateModelTest.EmptyDivTemplate;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.VaadinService;

public class TemplateModelWithConvertersTest {

    public static class TemplateWithConverters extends
            EmptyDivTemplate<TemplateWithConverters.TemplateModelWithConverters> {

        public interface TemplateModelWithConverters extends TemplateModel {

            @Convert(value = LongToStringConverter.class)
            public void setLongValue(long longValue);
            public long getLongValue();

            @Convert(value = DateToStringConverter.class)
            public void setDate(Date date);
            public Date getDate();

            @Convert(value = DateToBeanWithStringConverter.class)
            public void setDateString(Date date);
            public Date getDateString();

            @Convert(value = StringToBeanWithStringConverter.class)
            public void setString(String string);
            public String getString();

            @Convert(value = LongToStringConverter.class, path = "longValue")
            @Convert(value = DateToStringConverter.class, path = "date")
            public void setTestBean(TestBean bean);
            public TestBean getTestBean();
        }

        @Override
        protected TemplateModelWithConverters getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithIncompatibleConverter extends
            EmptyDivTemplate<TemplateWithIncompatibleConverter.TemplateModelWithIncompatibleConverter> {

        public interface TemplateModelWithIncompatibleConverter
                extends TemplateModel {

            @Convert(value = LongToStringConverter.class)
            public void setIntValue(int intValue);
        }

        @Override
        protected TemplateModelWithIncompatibleConverter getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithConverterOnParameterizedType extends
            EmptyDivTemplate<TemplateWithConverterOnParameterizedType.TemplateModelWithConverterOnParameterizedType> {

        public interface TemplateModelWithConverterOnParameterizedType
                extends TemplateModel {

            @Convert(value = LongToStringConverter.class)
            public void setList(List<String> list);
        }

        @Override
        protected TemplateModelWithConverterOnParameterizedType getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithSamePathInConverters extends
            EmptyDivTemplate<TemplateWithSamePathInConverters.TemplateModelWithSamePathInConverters> {

        public interface TemplateModelWithSamePathInConverters
                extends TemplateModel {

            @Convert(value = LongToStringConverter.class, path = "same")
            @Convert(value = LongToStringConverter.class, path = "same")
            public void setLongValue(long longValue);
        }

        @Override
        protected TemplateModelWithSamePathInConverters getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithConverterOnConvertedType extends
            EmptyDivTemplate<TemplateWithConverterOnConvertedType.TemplateModelWithConverterOnConvertedType> {
        public interface TemplateModelWithConverterOnConvertedType extends TemplateModel {

            @Convert(value = LongToBeanWithLongConverter.class)
            @Convert(value = LongToStringConverter.class, path = "longValue")
            public void setLongValue(long longValue);
            public long getLongValue();
        }

        @Override
        protected TemplateModelWithConverterOnConvertedType getModel() {
            return super.getModel();
        }
    }
    
    public static class TemplateWithUnsupportedConverterModel extends
            EmptyDivTemplate<TemplateWithUnsupportedConverterModel.TemplateModelWithUnsupportedConverterModel> {
        public interface TemplateModelWithUnsupportedConverterModel extends TemplateModel {
            
            @Convert(value = UnsupportedModelConverter.class)
            public void setString(String string);
        }
        
        @Override
        protected TemplateModelWithUnsupportedConverterModel getModel() {
            return super.getModel();
        }
    }


    public static class TemplateWithConvertedReadOnlyBean extends
            EmptyDivTemplate<TemplateWithConvertedReadOnlyBean.TemplateModelWithConvertedReadOnlyBean> {
        public interface TemplateModelWithConvertedReadOnlyBean
                extends TemplateModel {

            @Convert(value = LongToStringConverter.class, path = "id")
            public void setReadOnlyBean(ReadOnlyBean readOnlyBean);
            public ReadOnlyBean getReadOnlyBean();
        }

        @Override
        protected TemplateModelWithConvertedReadOnlyBean getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithDate
            extends EmptyDivTemplate<TemplateWithDate.TemplateModelWithDate> {
        public interface TemplateModelWithDate extends TemplateModel {

            @Convert(value = DateToDateBeanConverter.class)
            public void setDate(Date date);
            public Date getDate();
        }

        @Override
        protected TemplateModelWithDate getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithListOfBeans extends
            EmptyDivTemplate<TemplateWithListOfBeans.TemplateModelWithListOfBeans> {
        public interface TemplateModelWithListOfBeans extends TemplateModel {

            @Convert(value = LongToStringConverter.class, path = "longValue")
            @Convert(value = DateToStringConverter.class, path = "date")
            public void setTestBeans(List<TestBean> testBeans);
            public List<TestBean> getTestBeans();
        }

        @Override
        protected TemplateModelWithListOfBeans getModel() {
            return super.getModel();
        }
    }

    public static class LongToStringConverter
            implements ModelConverter<Long, String> {
        public LongToStringConverter() {
        }

        @Override
        public Class<Long> getApplicationType() {
            return long.class;
        }

        @Override
        public Class<String> getModelType() {
            return String.class;
        }

        @Override
        public String toModel(Long applicationValue) {
            return applicationValue.toString();
        }

        @Override
        public Long toApplication(String modelValue) {
            return Long.parseLong(modelValue);
        }
    }

    public static class DateToStringConverter
            implements ModelConverter<Date, String> {

        @Override
        public Class<Date> getApplicationType() {
            return Date.class;
        }

        @Override
        public Class<String> getModelType() {
            return String.class;
        }

        @Override
        public String toModel(Date applicationValue) {
            return Long.toString(applicationValue.getTime());
        }

        @Override
        public Date toApplication(String modelValue) {
            return new Date(Long.valueOf(modelValue));
        }
    }

    public static class StringToBeanWithStringConverter
            implements ModelConverter<String, BeanWithString> {

        @Override
        public Class<String> getApplicationType() {
            return String.class;
        }

        @Override
        public Class<BeanWithString> getModelType() {
            return BeanWithString.class;
        }

        @Override
        public BeanWithString toModel(String applicationValue) {
            return new BeanWithString(applicationValue);
        }

        @Override
        public String toApplication(BeanWithString modelValue) {
            return modelValue.getStringValue();
        }
    }

    public static class LongToBeanWithLongConverter
            implements ModelConverter<Long, BeanWithLong> {

        @Override
        public Class<Long> getApplicationType() {
            return long.class;
        }

        @Override
        public Class<BeanWithLong> getModelType() {
            return BeanWithLong.class;
        }

        @Override
        public BeanWithLong toModel(Long applicationValue) {
            return new BeanWithLong(applicationValue);
        }

        @Override
        public Long toApplication(BeanWithLong modelValue) {
            return modelValue.getLongValue();
        }
    }

    public static class DateToBeanWithStringConverter
            implements ModelConverter<Date, BeanWithString> {

        @Override
        public Class<Date> getApplicationType() {
            return Date.class;
        }

        @Override
        public Class<BeanWithString> getModelType() {
            return BeanWithString.class;
        }

        @Override
        public BeanWithString toModel(Date applicationValue) {
            return new BeanWithString(
                    Long.toString(applicationValue.getTime()));
        }

        @Override
        public Date toApplication(BeanWithString modelValue) {
            return new Date(Long.valueOf(modelValue.getStringValue()));
        }
    }

    public static class UnsupportedModelConverter
            implements ModelConverter<String, Long> {

        @Override
        public Class<String> getApplicationType() {
            return String.class;
        }

        @Override
        public Class<Long> getModelType() {
            return long.class;
        }

        @Override
        public Long toModel(String applicationValue) {
            return Long.valueOf(applicationValue);
        }

        @Override
        public String toApplication(Long modelValue) {
            return modelValue.toString();
        }
    }

    public static class DateToDateBeanConverter
            implements ModelConverter<Date, DateBean> {

        @Override
        public Class<Date> getApplicationType() {
            return Date.class;
        }

        @Override
        public Class<DateBean> getModelType() {
            return DateBean.class;
        }

        @Override
        public DateBean toModel(Date applicationValue) {
            if (applicationValue == null) {
                return null;
            }

            Calendar calendar = new GregorianCalendar();
            calendar.setTime(applicationValue);

            DateBean bean = new DateBean();
            bean.setDay(calendar.get(Calendar.DATE));
            bean.setMonth(calendar.get(Calendar.MONTH));
            bean.setYear(calendar.get(Calendar.YEAR));
            return bean;
        }

        @Override
        public Date toApplication(DateBean modelValue) {
            if (modelValue == null) {
                return null;
            }
            int year = modelValue.getYear();
            int day = modelValue.getDay();
            int month = modelValue.getMonth();
            return new GregorianCalendar(year, month, day).getTime();
        }
    }

    public static class BeanWithString implements Serializable {
        private String stringValue;

        public BeanWithString() {

        }

        public BeanWithString(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }
    }

    public static class TestBean {
        private long longValue;
        private Date date;

        public TestBean() {

        }

        public TestBean(long longValue, Date date) {
            this.longValue = longValue;
            this.date = date;
        }

        public long getLongValue() {
            return longValue;
        }

        public void setLongValue(long longValue) {
            this.longValue = longValue;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    public static class BeanWithLong implements Serializable {
        private long longValue;

        public BeanWithLong() {

        }

        public BeanWithLong(long longValue) {
            this.longValue = longValue;
        }

        public long getLongValue() {
            return longValue;
        }

        public void setLongValue(long longValue) {
            this.longValue = longValue;
        }
    }

    public static class ReadOnlyBean implements Serializable {
        public long getId() {
            return 0L;
        }
    }

    public static class DateBean implements Serializable {

        private int day;
        private int month;
        private int year;

        public int getDay() {
            return day;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }
    }

    @Before
    public void setUp() {
        Assert.assertNull(VaadinService.getCurrent());
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        VaadinService.setCurrent(service);
    }

    @After
    public void tearDown() {
        VaadinService.setCurrent(null);
    }

    @Test
    public void unsupported_primitive_type_to_basic_type_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        template.getModel().setLongValue(10L);
        Assert.assertEquals(10L, template.getModel().getLongValue());
    }

    @Test
    public void bean_to_basic_type_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        Date date = new Date();
        template.getModel().setDate(date);
        Assert.assertEquals(date, template.getModel().getDate());
    }

    @Test
    public void bean_to_bean_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        Date date = new Date();
        template.getModel().setDateString(date);
        Assert.assertEquals(date, template.getModel().getDateString());
    }

    @Test
    public void basic_type_to_bean_converter() {
        TemplateWithConverters template = new TemplateWithConverters();
        template.getModel().setString("string to bean");
        Assert.assertEquals("string to bean", template.getModel().getString());
    }

    @Test
    public void bean_with_multiple_converters() {
        TemplateWithConverters template = new TemplateWithConverters();
        Date date = new Date();
        TestBean bean = new TestBean(10L, date);
        template.getModel().setTestBean(bean);
        Assert.assertEquals(10L, template.getModel().getTestBean().getLongValue());
        Assert.assertEquals(date, template.getModel().getTestBean().getDate());
    }

    @Test
    public void converter_on_converted_type() {
        TemplateWithConverterOnConvertedType template = new TemplateWithConverterOnConvertedType();
        template.getModel().setLongValue(10L);
        Assert.assertEquals(10L, template.getModel().getLongValue());
    }

    @Test
    public void converter_on_bean_with_read_only_property() {
        TemplateWithConvertedReadOnlyBean template = new TemplateWithConvertedReadOnlyBean();
        template.getModel().setReadOnlyBean(new ReadOnlyBean());
        Assert.assertEquals(0L, template.getModel().getReadOnlyBean().getId());
    }

    public void convert_date_to_datebean() {
        // DateBean strips time information
        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date dateWithoutTime = calendar.getTime();

        TemplateWithDate template = new TemplateWithDate();
        template.getModel().setDate(dateWithoutTime);
        Assert.assertEquals(dateWithoutTime, template.getModel().getDate());
    }

    @Test
    public void convert_on_list_of_beans() {
        Date date = new Date();
        List<TestBean> testBeans = Collections
                .singletonList(new TestBean(0L, date));
        TemplateWithListOfBeans template = new TemplateWithListOfBeans();
        template.getModel().setTestBeans(testBeans);
        Assert.assertEquals(0L,
                template.getModel().getTestBeans().get(0).getLongValue());
        Assert.assertEquals(date,
                template.getModel().getTestBeans().get(0).getDate());
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void incompatible_converter_throws() {
        new TemplateWithIncompatibleConverter();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void parameterized_type_conversion_throws() {
        new TemplateWithConverterOnParameterizedType();
    }

    @Test(expected = RuntimeException.class)
    public void multiple_converters_for_same_path_throws() {
        new TemplateWithSamePathInConverters();
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void unsupported_model_type_in_converter() {
        new TemplateWithUnsupportedConverterModel();
    }
}
