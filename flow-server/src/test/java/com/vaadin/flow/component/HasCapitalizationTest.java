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
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

public class HasCapitalizationTest {

    @Tag("div")
    public static class HasCapitalizationComponent extends Component implements HasCapitalization {

    }

    @Test
    public void defaultValue() {
        HasCapitalizationComponent c = new HasCapitalizationComponent();
        Capitalization autocapitalize = c.getAutocapitalize();
        Assert.assertNull(autocapitalize);
    }

    @Test
    public void emptyValue() {
        HasCapitalizationComponent c = new HasCapitalizationComponent();
        c.getElement().setAttribute("autocapitalize", "");
        Capitalization autocapitalize = c.getAutocapitalize();
        Assert.assertEquals(Capitalization.SENTENCES, autocapitalize);
    }

    @Test
    public void noCapitalization() {
        HasCapitalizationComponent c = new HasCapitalizationComponent();
        c.setAutocapitalize(Capitalization.NONE);
        Assert.assertEquals(Capitalization.NONE, c.getAutocapitalize());
    }

    @Test
    public void sentencesCapitalization() {
        HasCapitalizationComponent c = new HasCapitalizationComponent();
        c.setAutocapitalize(Capitalization.SENTENCES);
        Assert.assertEquals(Capitalization.SENTENCES, c.getAutocapitalize());
    }

    @Test
    public void wordsCapitalization() {
        HasCapitalizationComponent c = new HasCapitalizationComponent();
        c.setAutocapitalize(Capitalization.WORDS);
        Assert.assertEquals(Capitalization.WORDS, c.getAutocapitalize());
    }

    @Test
    public void charsCapitalization() {
        HasCapitalizationComponent c = new HasCapitalizationComponent();
        c.setAutocapitalize(Capitalization.CHARACTERS);
        Assert.assertEquals(Capitalization.CHARACTERS, c.getAutocapitalize());
    }
}
