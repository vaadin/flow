/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui.template;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.vaadin.annotations.Convert;
import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.ModelConverter;
import com.vaadin.flow.template.model.TemplateModel;

@Tag("convert-to-bean")
@HtmlImport("/com/vaadin/flow/uitest/ui/template/ConvertToBean.html")
public class ConvertToBeanView extends
        PolymerTemplate<ConvertToBeanView.ConvertToBeanModel> implements View {

    public interface ConvertToBeanModel extends TemplateModel {
        Date getDate();

        @Convert(DateToBeanConverter.class)
        void setDate(Date date);

        public void setMessage(String message);
    }

    public static class DateBean implements Serializable {
        private String day;
        private String month;
        private String year;

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

    }

    public static class DateToBeanConverter
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
            DateBean bean = new DateBean();
            bean.setDay(Integer.toString(applicationValue.getDate()));
            bean.setMonth(Integer.toString(applicationValue.getMonth() + 1));
            bean.setYear(Integer.toString(applicationValue.getYear() + 1900));
            return bean;
        }

        @Override
        public Date toApplication(DateBean modelValue) {
            if (modelValue == null) {
                return null;
            }
            if (modelValue.getYear() == null || modelValue.getDay() == null
                    || modelValue.getMonth() == null) {
                return null;
            }

            return new Date(Integer.valueOf(modelValue.getYear()) - 1900,
                    Integer.valueOf(modelValue.getMonth()) - 1,
                    Integer.valueOf(modelValue.getDay()));
        }

    }

    public ConvertToBeanView() {
        setId("template");
    }

    @EventHandler
    private void submit() {
        Date date = getModel().getDate();
        getModel().setMessage(new SimpleDateFormat("dd.MM.yyyy").format(date));
    }
}
