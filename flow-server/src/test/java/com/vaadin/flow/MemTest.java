package com.vaadin.flow;

import org.junit.Test;

public class MemTest {

    private static final int SIZE_MB = 100;
    private static final int TOTAL_MB = 1024 * 16;

    @Test
    public void allocateALotFast() {
        System.out.println("Start");
        for (int i = 0; i < (TOTAL_MB / SIZE_MB); i++) {
            byte[] buffer = new byte[SIZE_MB * 1024 * 1024];
            System.out.println("Allocated " + i);
            System.out.println("buffer[0] " + buffer[0]);
        }
        System.out.println("End");
        System.exit(123);
    }
}
