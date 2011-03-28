/*
 * PriorityReference.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.util;

/**
 * A generic reference, capable of associating an element
 * with a priority. Designed specifically for using
 * PriorityBlockingQueues with classes that do not implement
 * <tt>Comparable</tt> and that do not have a field which
 * can be used to order by priority using a <tt>Comparator</tt>
 *
 * @author Ryan Wilson
 */
public class PriorityReference<E> implements Comparable<PriorityReference<E>> {

    private final Priority priority;
    private final E        element;

    public PriorityReference(E element) {
        this(element, Priority.DEFAULT);
    }

    public PriorityReference(E element, Priority priority) {
        this.element  = element;
        this.priority = priority;
    }

    public E get() {
        return element;
    }

    public Priority getPriority() {
        return priority;
    }

    public int compareTo(PriorityReference<E> other) {
        if (other == null)
            return 1;

        return this.priority.compareTo(other.priority);
    }

}
