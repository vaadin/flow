/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui.template;

import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.ui.Template;

public class TemplateTextFormattingView extends Template {

    public interface Model extends TemplateModel {
        public void setIntZero(int i);

        public void setIntMax(int i);

        public void setIntMin(int i);

        public void setDoubleZero(double d);

        public void setDoubleHalf(double d);

        public void setDoubleNaN(double d);

        public void setDoubleSmall(double d);

        public void setDoubleLarge(double d);

        public void setDoubleSemiSmall(double d);

        public void setDoubleSemiLarge(double d);

        public void setDoubleMin(double d);

        public void setDoubleMinPlus(double d);

        public void setDoubleMax(double d);

        public void setDoublePosInf(double d);

        public void setDoubleNegInf(double d);
    }

    @Override
    protected Model getModel() {
        return (Model) super.getModel();
    }

    public TemplateTextFormattingView() {
        getModel().setIntMax(Integer.MAX_VALUE);
        getModel().setIntMin(Integer.MIN_VALUE);
        getModel().setIntZero(0);

        getModel().setDoubleZero(0);
        getModel().setDoubleHalf(0.50);
        getModel().setDoubleNaN(Double.NaN);
        getModel().setDoubleSemiSmall(-1e20);
        getModel().setDoubleSemiLarge(1e20);
        getModel().setDoubleSmall(-123e123);
        getModel().setDoubleLarge(123e123);
        getModel().setDoubleMin(Double.MIN_VALUE);
        double minPlus = Double.longBitsToDouble(
                Double.doubleToLongBits(Double.MIN_VALUE) + 1);
        getModel().setDoubleMinPlus(minPlus);
        getModel().setDoubleMax(Double.MAX_VALUE);
        getModel().setDoublePosInf(Double.POSITIVE_INFINITY);
        getModel().setDoubleNegInf(Double.NEGATIVE_INFINITY);

    }

}
