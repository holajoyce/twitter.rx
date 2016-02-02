package uk.co.flax.luwak;

/*
 *   Copyright (c) 2015 Lemur Consulting Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import uk.co.flax.luwak.util.CollectionUtils;

/**
 * Utility class for concurrently loading queries into a Monitor.
 *
 * This is useful to speed up startup times for a Monitor.  You can use multiple
 * threads to parse and index queries before starting matches.
 *
 * Use as follows:
 * <pre class="prettyprint">
 *     try (ConcurrentQueryLoader loader = new ConcurrentQueryLoader(monitor)) {
 *         for (MonitorQuery mq : getQueries()) {
 *             loader.add(mq);
 *         }
 *     }
 * </pre>
 *
 * The Monitor's MonitorQueryParser must be thread-safe for this to work correctly.
 */
public class ConcurrentQueryLoader implements Closeable {

    private final Monitor monitor;
    private final ExecutorService executor;
    private final CountDownLatch shutdownLatch;
    private final BlockingQueue<MonitorQuery> queue;
    private final List<QueryError> errors = new CopyOnWriteArrayList<>();
    private final List<QueryError> errorOutput;

    private boolean shutdown = false;
    private IOException error;

    public static final int DEFAULT_QUEUE_SIZE = 2000;

    /**
     * Create a new ConcurrentQueryLoader for a {@link Monitor}
     * @param monitor Monitor
     * @param errors a List that will be populated with any query errors
     */
    public ConcurrentQueryLoader(Monitor monitor, List<QueryError> errors) {
        this(monitor, errors, Runtime.getRuntime().availableProcessors(), DEFAULT_QUEUE_SIZE);
    }

    /**
     * Create a new ConcurrentQueryLoader
     * @param monitor the Monitor to load queries to
     * @param errors a List that will be populated with any query errors
     * @param threads the number of threads to use
     * @param queueSize the size of the buffer to hold queries in
     */
    public ConcurrentQueryLoader(Monitor monitor, List<QueryError> errors, int threads, int queueSize) {
        this.monitor = monitor;
        this.errorOutput = errors;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.executor = Executors.newFixedThreadPool(threads);
        this.shutdownLatch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            this.executor.submit(new Worker(queueSize / threads));
        }
    }

    /**
     * Add a MonitorQuery to the loader's internal buffer
     *
     * If the buffer is full, this will block until there is room to add
     * the MonitorQuery
     *
     * @param mq the monitor query
     * @throws InterruptedException if interrupted while waiting
     */
    public void add(MonitorQuery mq) throws InterruptedException {
        if (shutdown)
            throw new IllegalStateException("ConcurrentQueryLoader has been shutdown, cannot add new queries");
        this.queue.put(mq);
    }

    @Override
    public void close() throws IOException {
        this.shutdown = true;
        this.executor.shutdown();
        try {
            this.shutdownLatch.await();
        } catch (InterruptedException e) {
            // fine
        }
        errorOutput.addAll(errors);
        if (error != null)
            throw error;
    }

    private class Worker implements Runnable {

        final List<MonitorQuery> workerQueue;
        final int queueSize;
        boolean running = true;

        Worker(int queueSize) {
            workerQueue = new ArrayList<>(queueSize);
            this.queueSize = queueSize;
        }

        @Override
        public void run() {
            try {
                while (running) {
                    workerQueue.clear();
                    CollectionUtils.drain(queue, workerQueue, queueSize, 100, TimeUnit.MILLISECONDS);
                    if (workerQueue.size() == 0 && shutdown)
                        running = false;
                    if (workerQueue.size() > 0) {
                        errors.addAll(monitor.update(workerQueue));
                    }
                }
            }
            catch (IOException e) {
                error = e;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finally {
                shutdownLatch.countDown();
            }
        }
    }
}
