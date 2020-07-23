/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.data.provider;

import com.vaadin.flow.tests.data.bean.Item;
import org.junit.Assert;
import org.junit.Test;

public class QueryTest {

    @Test
    public void pageAccess_limitAndOffsetProvided_correctPageAndPageSizeReturned() {
        Query<Item, Void> query;

        query = new Query<>(0, 20, null, null, null);
        Assert.assertEquals(0L, query.getPage());
        Assert.assertEquals(20, query.getPageSize());

        query = new Query<>(20, 20, null, null, null);
        Assert.assertEquals(1L, query.getPage());
        Assert.assertEquals(20, query.getPageSize());

        query = new Query<>(200, 40, null, null, null);
        Assert.assertEquals(5L, query.getPage());
        Assert.assertEquals(40, query.getPageSize());
    }
}
