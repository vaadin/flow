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

package com.vaadin.flow.server.connect;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BeanValueTypeCheckHelperTest {
    private BeanValueTypeCheckHelper beanValueTypeCheckHelper;
    private Type beanType;

    @Before
    public void setup() throws NoSuchMethodException {
        beanValueTypeCheckHelper = new BeanValueTypeCheckHelper();
        beanType = Bean.class;
    }

    @Test
    public void should_NotHaveVisited_When_NotMarkedYet() {
        Bean bean = new Bean();
        Assert.assertFalse(beanValueTypeCheckHelper.hasVisited(bean, beanType));

        beanValueTypeCheckHelper.markAsVisited(bean, beanType);
        Assert.assertFalse(beanValueTypeCheckHelper.hasVisited(new Bean(), beanType));
    }

    @Test
    public void should_HaveVisited_When_Marked() {
        Bean bean = new Bean();
        beanValueTypeCheckHelper.markAsVisited(bean, beanType);
        Assert.assertTrue(beanValueTypeCheckHelper.hasVisited(bean, beanType));
    }

    @Test
    public void should_NotHaveVisited_When_MarkedOtherObject() {
        Bean bean = new Bean();
        beanValueTypeCheckHelper.markAsVisited(bean, beanType);
        Assert.assertFalse(beanValueTypeCheckHelper.hasVisited(new Bean(), beanType));
    }

    @Test
    public void should_NotHaveVisited_When_MarkedForAnotherType() {
        Bean bean = new Bean();
        beanValueTypeCheckHelper.markAsVisited(bean, beanType);
        Assert.assertFalse(beanValueTypeCheckHelper.hasVisited(bean, Object.class));
    }

    @Test
    public void should_BeThreadSafe()
            throws InterruptedException, ExecutionException {
        final int nThreads = 1000;
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        List<Future<Boolean>> marks = new ArrayList<>(nThreads);

        CountDownLatch beforeStart = new CountDownLatch(1);
        for (int i = 0; i < nThreads; i++) {
            marks.add(service.submit(() -> {
                beforeStart.await();
                Bean bean = new Bean();
                beanValueTypeCheckHelper.markAsVisited(bean, beanType);
                return beanValueTypeCheckHelper.hasVisited(bean, beanType);
            }));
        }
        beforeStart.countDown();
        for (int i = 0; i < nThreads; i++) {
            Assert.assertTrue(marks.get(i).get());
        }
    }

    static private class Bean {
    }
}
