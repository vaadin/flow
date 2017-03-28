/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.declarative;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vaadin.spring.annotation.EnableVaadin;
import com.vaadin.spring.internal.SpringTextField;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class DeclarativeTest {

    @Configuration
    @EnableVaadin
    @ComponentScan(basePackageClasses = { SpringTextField.class })
    public static class Config {
    }

    @Test
    public void loadAutowired() {
        RootView rootView = new RootView();
        Assert.assertEquals("Spring", rootView.tf.getCaption());
        // Ensure that "OwnComponentFactory" is not used
        Assert.assertNull(rootView.tf.getId());
    }

}
