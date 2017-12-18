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
package com.vaadin.flow.tutorial.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.router.Route;

@CodeFor("spring/tutorial-spring-routing.asciidoc")
public class SpringRouting {

    @Route("")
    public class RootComponent extends Div {

        @Autowired
        private DataBean dataBean;

        public RootComponent() {
            setText("Default path");
            setText(dataBean.getMessage());
        }
    }

    public interface DataBean {
        String getMessage();
    }

    @Component
    public class DataBeanImpl implements DataBean {

        @Override
        public String getMessage() {
            return "message";
        }
    }
}
