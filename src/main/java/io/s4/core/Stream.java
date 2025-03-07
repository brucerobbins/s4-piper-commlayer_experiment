/*
 * Copyright (c) 2011 Yahoo! Inc. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License. See accompanying LICENSE file. 
 */
package io.s4.core;

import io.s4.comm.ReceiverListener;
import io.s4.comm.Receiver;
import io.s4.comm.Sender;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Stream} and {@link ProcessingElement} objects represent the links and
 * nodes in the application graph. A stream sends an {@link Event} object to
 * {@link ProcessingElement} instances located anywhere in a cluster.
 * <p>
 * Once a stream is instantiated, it is immutable.
 * <p>
 * To build an application create stream objects using use the
 * {@link StreamFactory} class.
 */
public class Stream<T extends Event> implements Runnable, ReceiverListener {

    private static final Logger logger = LoggerFactory.getLogger(Stream.class);

    final static private String DEFAULT_SEPARATOR = "^";
    final static private int CAPACITY = 1000;
    private static int idCounter = 0;
    final private String name;
    final private Key<T> key;
    final private ProcessingElement[] targetPEs;
    final private BlockingQueue<T> queue = new ArrayBlockingQueue<T>(CAPACITY);
    final private Thread thread;
    final private Sender sender;
    final private Receiver receiver;
    final private int id;

    /**
     * Send events using a {@link KeyFinder<T>}. The key finder extracts the
     * value of the key which is used to determine the target
     * {@link io.s4.comm.topology.ClusterNode} for an event.
     * 
     * @param app
     *            we always register streams with the parent application.
     * @param name
     *            give this stream a meaningful name in the context of your
     *            application.
     * @param finder
     *            the finder object to find the value of the key in an event.
     * @param processingElements
     *            the target PE prototypes for this stream.
     */
    public Stream(App app, String name, KeyFinder<T> finder,
            ProcessingElement... processingElements) {
        synchronized (Stream.class) {
            id = idCounter++;
        }

        app.addStream(this);
        this.name = name;

        if (finder == null) {
            this.key = null;
        } else {
            this.key = new Key<T>(finder, DEFAULT_SEPARATOR);
        }
        this.sender = app.getSender();
        this.receiver = app.getReceiver();
        this.targetPEs = processingElements;

        /* Start streaming. */
        thread = new Thread(this, name);
        thread.start();
        this.receiver.addListener(this);
    }

    /**
     * Send events to all available {@link ProcessingElement} instances
     * contained by the {@link ProcessingElement} prototypes passed to this
     * constructor.
     * 
     * @param app
     *            we always register streams with the parent application.
     * @param name
     *            give this stream a meaningful name in the context of your
     *            application.
     * @param processingElements
     *            the target PE prototypes for this stream.
     */
    public Stream(App app, String name, ProcessingElement... processingElements) {
        this(app, name, null, processingElements);
    }

    /**
     * Sends an event.
     * 
     * @param event
     */
    @SuppressWarnings("unused")
    public void put(T event) {
        try {
            event.setTargetStreamId(this.id);
            if (key != null) {
                sender.send(key.get(event), event);
            } else {
                // no key, send to all partitions
                sender.send(event);
            }
            // maybe have sender return some code if the event belongs to this
            // node
            if (event == null) { // for now, don't run the code in the following
                                 // blocks
                queue.put(event);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error(
                    "Interrupted while waiting to put an event in the queue: {}.",
                    e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Implements the {@link ReceiverListener} interface. The low level
     * {@link Receiver} object call this method when a new {@link Event} is
     * available.
     */
    public void receiveEvent(Event event) {
        // TODO: better method for determining if a stream should use an event
        if (event.getTargetStreamId() != this.id) {
            return;
        }
        try {
            queue.put((T) event);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error(
                    "Interrupted while waiting to put an event in the queue: {}.",
                    e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the key
     */
    public Key<T> getKey() {
        return key;
    }

    /**
     * @return the list of target processing element prototypes.
     */
    public ProcessingElement[] getTargetPEs() {
        return targetPEs;
    }

    /**
     * Stop and close this stream.
     */
    public void close() {
        thread.interrupt();
    }

    /**
     * @return the sender object
     */
    public Sender getSender() {
        return sender;
    }

    /**
     * @return the receiver object
     */
    public Receiver getReceiver() {
        return receiver;
    }

    @Override
    public void run() {
        while (true) {
            try {
                /* Get oldest event in queue. */
                T event = queue.take();

                /* Send event to each target PE. */
                for (int i = 0; i < targetPEs.length; i++) {

                    if (key == null) {

                        /* Broadcast to all PE instances! */

                        /* STEP 1: find all PE instances. */

                        Collection<ProcessingElement> pes = targetPEs[i]
                                .getInstances();

                        /* STEP 2: iterate and pass event to PE instance. */
                        for (ProcessingElement pe : pes) {

                            pe.handleInputEvent(event);
                        }

                    } else {

                        /* We have a key, send to target PE. */

                        /* STEP 1: find the PE instance for key. */
                        ProcessingElement pe = targetPEs[i]
                                .getInstanceForKey(key.get(event));

                        /* STEP 2: pass event to PE instance. */
                        pe.handleInputEvent(event);
                    }
                }

            } catch (InterruptedException e) {
                logger.info("Closing stream {}.", name);
                receiver.removeListener(this);
                return;
            }
        }
    }
}
