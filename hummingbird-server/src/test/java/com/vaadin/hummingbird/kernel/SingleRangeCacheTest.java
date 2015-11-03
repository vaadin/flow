package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.hummingbird.kernel.SingleRangeCache.AddListener;
import com.vaadin.hummingbird.kernel.SingleRangeCache.RemoveListener;
import com.vaadin.shared.ui.grid.Range;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SingleRangeCacheTest {

    public static class MyBean {
        private int id;

        public MyBean(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MyBean other = (MyBean) obj;
            if (id != other.id) {
                return false;
            }
            return true;
        }

    }

    private SingleRangeCache<MyBean> cache;
    private SingleRangeCache.AddListener<MyBean> addListener;
    private SingleRangeCache.RemoveListener<MyBean> removeListener;
    private List<List<MyBean>> addLog = new ArrayList<>();
    private List<List<MyBean>> removeLog = new ArrayList<>();

    @Before
    public void setup() {
        addListener = new AddListener<MyBean>() {
            @Override
            public void addedToCache(List<MyBean> added) {
                addLog.add(added);
            }
        };
        removeListener = new RemoveListener<MyBean>() {
            @Override
            public void removedFromCache(List<MyBean> removed) {
                removeLog.add(removed);
            }
        };
        cache = new SingleRangeCache<>(addListener, removeListener);
    }

    @Test
    public void defaultRange() {
        Assert.assertEquals(0, cache.getRangeStart());
        Assert.assertEquals(0, cache.getRangeEnd());
    }

    @Test
    public void setRangeEnd() {
        cache.setRangeEnd(10);
        Assert.assertEquals(0, cache.getRangeStart());
        Assert.assertEquals(10, cache.getRangeEnd());
    }

    @Test
    public void setRangeStartToEnd() {
        cache.setRangeEnd(10);
        cache.setRangeStart(10);
        Assert.assertEquals(10, cache.getRangeStart());
        Assert.assertEquals(10, cache.getRangeEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRangeStartAfterEnd() {
        cache.setRangeStart(12);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRangeEndBeforeStart() {
        cache.setRangeStart(12);
        cache.setRangeEnd(10);
    }

    @Test(expected = AssertionError.class)
    public void setRangeStartNegative() {
        cache.setRangeStart(-2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDataOutsideRangeEnd() {
        cache.get(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDataOutsideRangeEnd2() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        cache.get(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDataOutsideRangeStart() {
        cache.get(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDataOutsideRangeStart2() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        cache.get(4);
    }

    @Test
    public void getUninitializedFirstItemInRange() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        Assert.assertNull(cache.get(5));
    }

    @Test
    public void getUninitializedLastItemInRange() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        Assert.assertNull(cache.get(9));
    }

    @Test
    public void getFirstItemInRange() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRange(cache);
        Assert.assertEquals(new MyBean(5), cache.get(5));
    }

    private void populateRange(SingleRangeCache<MyBean> cache) {
        for (int i = cache.getRangeStart(); i < cache.getRangeEnd(); i++) {
            cache.set(i, new MyBean(i));
        }

    }

    private void populateRangeList(SingleRangeCache<MyBean> cache) {
        ArrayList<MyBean> data = new ArrayList<>();
        for (int i = cache.getRangeStart(); i < cache.getRangeEnd(); i++) {
            data.add(new MyBean(i));
        }
        cache.set(cache.getRangeStart(), data);

    }

    @Test
    public void getLastItemInRange() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRange(cache);
        Assert.assertEquals(new MyBean(9), cache.get(9));
    }

    @Test
    public void getFirstItemsInRange() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRange(cache);
        List<MyBean> twoFirst = cache.get(5, 7);
        Assert.assertEquals(new MyBean(5), twoFirst.get(0));
        Assert.assertEquals(new MyBean(6), twoFirst.get(1));
    }

    @Test
    public void getLastItemsInRange() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRange(cache);
        List<MyBean> twoFirst = cache.get(8, 10);
        Assert.assertEquals(new MyBean(8), twoFirst.get(0));
        Assert.assertEquals(new MyBean(9), twoFirst.get(1));
    }

    @Test
    public void addItemCalledOnPopulate() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRange(cache);
        Assert.assertEquals(5, addLog.size());
    }

    @Test
    public void addItemCalledOnPopulateMany() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRangeList(cache);
        Assert.assertEquals(1, addLog.size());
        Assert.assertEquals(5, addLog.get(0).size());
    }

    @Test
    public void availableDataRangeAfterReduceFromEnd() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRangeList(cache);
        cache.setRangeEnd(7);
        Assert.assertEquals(Range.between(5, 7),
                cache.getAvailableDataRange(5, 7));
    }

    @Test
    public void availableDataRangeAfterReduceFromStart() {
        cache.setRangeEnd(10);
        cache.setRangeStart(5);
        populateRangeList(cache);
        cache.setRangeStart(7);
        Assert.assertEquals(Range.between(7, 10),
                cache.getAvailableDataRange(7, 10));
    }

    @Test
    public void getDataAfterReduceFromEnd() {
        cache.setRangeEnd(10);
        populateRangeList(cache);
        cache.setRangeEnd(7);
        Assert.assertEquals(new MyBean(5), cache.get(5));
        Assert.assertEquals(new MyBean(6), cache.get(6));
    }

    @Test
    public void getDataAfterMoveRangeBackwards() {
        cache.setRangeEnd(20);
        cache.setRangeStart(10);
        populateRangeList(cache);
        cache.setRangeEnd(10);
        cache.setRangeStart(0);
        Assert.assertFalse(cache.hasAvailableData(0));
        populateRangeList(cache);
        Assert.assertTrue(cache.hasAvailableData(0));
        Assert.assertTrue(cache.hasAvailableData(9));
        Assert.assertFalse(cache.hasAvailableData(10));
        Assert.assertEquals(new MyBean(0), cache.get(0));
        Assert.assertEquals(new MyBean(9), cache.get(9));
    }

    @Test
    public void getDataAfterMoveRangeForwards() {
        cache.setRangeEnd(20);
        cache.setRangeStart(10);
        populateRangeList(cache);
        cache.setRangeEnd(30);
        cache.setRangeStart(20);
        Assert.assertFalse(cache.hasAvailableData(20));
        Assert.assertFalse(cache.hasAvailableData(29));
        Assert.assertFalse(cache.hasAvailableData(30));
        populateRangeList(cache);

        Assert.assertTrue(cache.hasAvailableData(20));
        Assert.assertTrue(cache.hasAvailableData(29));
        Assert.assertFalse(cache.hasAvailableData(30));

        Assert.assertEquals(new MyBean(20), cache.get(20));
        Assert.assertEquals(new MyBean(29), cache.get(29));
    }

    @Test
    public void addRemoveEventsWhenMovingRangeBackward() {
        cache.setRangeEnd(20);
        cache.setRangeStart(10);
        assertAddLog();
        assertRemoveLog();
        populateRangeList(cache);
        assertAddLog(10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        assertRemoveLog();

        addLog.clear();
        cache.setRangeEnd(10); // Remove for [10-19]
        assertRemoveLog(10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        cache.setRangeStart(0); // Add for [0-9]
        assertAddLog();
        populateRangeList(cache);
        assertAddLog(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void addRemoveEventsWhenMovingRangeForward() {
        cache.setRangeEnd(20);
        cache.setRangeStart(10);
        populateRangeList(cache);
        cache.setRangeEnd(30);
        cache.setRangeStart(20);
        Assert.assertFalse(cache.hasAvailableData(20));
        Assert.assertFalse(cache.hasAvailableData(29));
        Assert.assertFalse(cache.hasAvailableData(30));
        populateRangeList(cache);

        Assert.assertTrue(cache.hasAvailableData(20));
        Assert.assertTrue(cache.hasAvailableData(29));
        Assert.assertFalse(cache.hasAvailableData(30));

        Assert.assertEquals(new MyBean(20), cache.get(20));
        Assert.assertEquals(new MyBean(29), cache.get(29));
    }

    private void assertAddLog(int... expected) {
        List<MyBean> flattenedAddLog = new ArrayList<>();
        for (List<MyBean> adds : addLog) {
            flattenedAddLog.addAll(adds);
        }

        Assert.assertEquals(expected.length, flattenedAddLog.size());

        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(new MyBean(expected[i]),
                    flattenedAddLog.get(i));
        }
    }

    private void assertRemoveLog(int... expected) {
        List<MyBean> flattenedRemoveLog = new ArrayList<>();
        for (List<MyBean> Removes : removeLog) {
            flattenedRemoveLog.addAll(Removes);
        }

        Assert.assertEquals(expected.length, flattenedRemoveLog.size());

        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(new MyBean(expected[i]),
                    flattenedRemoveLog.get(i));
        }
    }
}
