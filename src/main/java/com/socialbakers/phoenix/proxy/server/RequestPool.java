/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.socialbakers.phoenix.proxy.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author robert
 */
class RequestPool extends ThreadPoolExecutor {

    private static final RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandlerImpl();
    
    RequestPool(int corePoolSize, int maximumPoolSize, long keepAliveTimeMs, int queueSize) {
        super(corePoolSize, maximumPoolSize, keepAliveTimeMs, TimeUnit.MILLISECONDS, 
                new ArrayBlockingQueue<Runnable>(queueSize), Executors.defaultThreadFactory(), rejectionHandler);
    }
    
    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    void execute(RequestProcessor command) throws RejectedExecutionException {
        super.execute(command);
    }
    
    /**
     * Returns the count of commands waiting in queue.
     * @return the count of commands waiting in queue
     */
    public int getCmdCountInQueue() {
        return getQueue().size();
    }
    
    @Override
    public void execute(Runnable r) {
        throw new UnsupportedOperationException("Only the " + RequestProcessor.class.getName() 
                + " instance can be executed.");
    }
    
    /**
     * Sends ZERO to client if command was rejected.
     */
    private static class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                ByteBuffer lenBuf = ByteBuffer.allocate(4);
                lenBuf.putInt(0);
                byte[] zero = lenBuf.array();
                SocketChannel channel = ((RequestProcessor)r).getChannel();
                channel.write(ByteBuffer.wrap(zero));
            } catch (IOException ex) {
                Logger.getLogger(RequestPool.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                System.out.println(r.toString() + " is rejected, sendind ZERO to client.");
            }
        }
    }
}