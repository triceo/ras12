package org.drools.planner.examples.ras2012.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Provides a means of randomly picking items from a collection.
 * 
 * @param <T> Generic type of the collection.
 */
public class RandomAccessor<T> {

    private final List<T> data = new ArrayList<>();
    private final Random  random;

    public RandomAccessor(final Collection<T> collection) {
        this(collection, new Random(System.nanoTime()));
    }

    public RandomAccessor(final Collection<T> collection, final Random random) {
        this.data.addAll(collection);
        this.random = random;
    }

    public T get() {
        return this.data.get(this.random.nextInt(this.data.size()));
    }

}
