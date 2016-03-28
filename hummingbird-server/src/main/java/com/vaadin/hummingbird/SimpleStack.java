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
package com.vaadin.hummingbird;

import java.util.EmptyStackException;

/**
 * Simple stack implementation.
 * 
 * @author Vaadin Ltd
 *
 */
public class SimpleStack<T> {

    private static class StackNode<T> {
        private final T data;
        private final StackNode<T> next;

        private StackNode(T data, StackNode<T> next) {
            this.data = data;
            this.next = next;
        }

        public T getData() {
            return data;
        }

        public StackNode<T> getNext() {
            return next;
        }

    }

    private StackNode<T> head;

    /**
     * Pushes an item onto the top of this stack.
     *
     * @param item
     *            the item to be pushed onto this stack
     */
    public void push(T item) {
        StackNode<T> newHead = new StackNode<>(item, head);
        head = newHead;
    }

    /**
     * Removes the object at the top of this stack and returns that object as
     * the value of this function.
     *
     * @return The object at the top of this stack
     * @throws EmptyStackException
     *             if this stack is empty.
     */
    public T pop() throws EmptyStackException {
        if (isEmpty()) {
            throw new EmptyStackException();
        }
        T data = head.getData();
        head = head.getNext();
        return data;
    }

    /**
     * Tests if this stack is empty.
     *
     * @return <code>true</code> if and only if this stack contains no items;
     *         <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return head == null;
    }
}
