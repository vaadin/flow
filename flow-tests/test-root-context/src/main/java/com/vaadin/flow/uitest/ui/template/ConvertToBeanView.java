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

import com.vaadin.flow.model.Convert;
import com.vaadin.flow.model.ModelConverter;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.EventHandler;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.ConvertToBeanView", layout = ViewTestLayout.class)
@Tag("convert-to-bean")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ConvertToBean.html")
public class ConvertToBeanView
        extends PolymerTemplate<ConvertToBeanView.ConvertToBeanModel> {

    public interface ConvertToBeanModel extends TemplateModel {
        Date getDate();

        @Convert(DateToBeanConverter.class)
        void setDate(Date date);

        void setMessage(String message);
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
        public DateBean toPresentation(Date modelValue) {
            if (modelValue == null) {
                return null;
            }
            DateBean bean = new DateBean();
            bean.setDay(Integer.toString(modelValue.getDate()));
            bean.setMonth(Integer.toString(modelValue.getMonth() + 1));
            bean.setYear(Integer.toString(modelValue.getYear() + 1900));
            return bean;
        }

        @Override
        public Date toModel(DateBean presentationValue) {
            if (presentationValue == null) {
                return null;
            }
            if (presentationValue.getYear() == null
                    || presentationValue.getDay() == null
                    || presentationValue.getMonth() == null) {
                return null;
            }

            return new Date(Integer.valueOf(presentationValue.getYear()) - 1900,
                    Integer.valueOf(presentationValue.getMonth()) - 1,
                    Integer.valueOf(presentationValue.getDay()));
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
