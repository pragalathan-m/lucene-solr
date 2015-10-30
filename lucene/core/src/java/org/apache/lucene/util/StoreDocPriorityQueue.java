package org.apache.lucene.util;

import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.search.ScoreDoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * A PriorityQueue maintains a partial ordering of its elements such that the
 * least element can always be found in constant time. Put()'s and pop()'s
 * require log(size) time.
 *
 * <p>
 * <b>NOTE</b>: This class will pre-allocate a full array of length
 * <code>maxSize+1</code> if instantiated via the
 * {@link #StoreDocPriorityQueue(int,boolean)} constructor with
 * <code>prepopulate</code> set to <code>true</code>.
 *
 * @lucene.internal
 */
public abstract class StoreDocPriorityQueue<T extends ScoreDoc> extends PriorityQueue<T> {

    private final Map<Integer, Integer> dataIndex;

    public StoreDocPriorityQueue(int maxSize) {
        this(maxSize, true);
    }

    @SuppressWarnings("unchecked")
    public StoreDocPriorityQueue(int maxSize, boolean prepopulate) {
        super(maxSize, prepopulate);
        dataIndex = new HashMap<Integer, Integer>(heap.length);
    }

    @Override
    public final T add(T element) {
        dataIndex.put(element.doc, size);
        return super.add(element);
    }

    public T getElement(int docId) {
        Integer heapIndex = dataIndex.get(docId);
        return heapIndex == null ? null : heap[heapIndex];
    }

    @Override
    public T insertWithOverflow(T element) {
        if (size > 0 && !lessThan(element, heap[1])) {
            dataIndex.remove(heap[1].doc);
            dataIndex.put(element.doc, 1);
        }
        return super.insertWithOverflow(element);
    }

    @Override
    public final T pop() {
        if (size > 0) {
            T result = heap[1];       // save first value
            dataIndex.remove(result.doc);
            heap[1] = heap[size];     // move last to first
            dataIndex.put(heap[1].doc, 1);
            heap[size] = null;        // permit GC of objects
            size--;
            downHeap(1);               // adjust heap
            return result;
        } else {
            return null;
        }
    }

    @Override
    public final void clear() {
        super.clear();
        dataIndex.clear();
    }

    @Override
    protected boolean upHeap(int origPos) {
        int i = origPos;
        T node = heap[i];          // save bottom node
        int j = i >>> 1;
        while (j > 0 && lessThan(node, heap[j])) {
            heap[i] = heap[j];       // shift parents down
            dataIndex.put(heap[i].doc, i);
            i = j;
            j = j >>> 1;
        }
        heap[i] = node;            // install saved node
        dataIndex.put(node.doc, i);
        return i != origPos;
    }

    @Override
    protected void downHeap(int i) {
        T node = heap[i];          // save top node
        int j = i << 1;            // find smaller child
        int k = j + 1;
        if (k <= size && lessThan(heap[k], heap[j])) {
            j = k;
        }
        while (j <= size && lessThan(heap[j], node)) {
            heap[i] = heap[j];       // shift up child
            dataIndex.put(heap[i].doc, i);
            i = j;
            j = i << 1;
            k = j + 1;
            if (k <= size && lessThan(heap[k], heap[j])) {
                j = k;
            }
        }
        heap[i] = node;            // install saved node
        if (node != null) {
            dataIndex.put(node.doc, i);
        }
    }

    public final T swap(T from, int toDocId, float toScore) {
        Integer fromPos = dataIndex.get(from.doc);

        dataIndex.remove(from.doc);
        dataIndex.put(toDocId, fromPos);
        from.doc = toDocId;
        from.score = toScore;

        downHeap(fromPos);
        upHeap(fromPos);

        return heap[1];
    }
}
