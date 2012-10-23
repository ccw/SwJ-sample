package com.sas.groovy.test;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

class ClosureRunner implements Runnable {
    static Logger testLogger = Logger.getLogger("com.sas.groovy.test.TestCase");
    static Logger armLogger = Logger.getLogger("Perf.ARM.com.sas.groovy.test.TestCase");
    def thread;
    def threadTimes;
    def threadExceptions;
    def closure;

    void run() {
        long startTime = System.currentTimeMillis();
        long endTime = 0;
        MDC.put( "thread", thread );
        armLogger.info( "START Thread$thread" );
        try {
            closure( thread );
            armLogger.info( "STOP Thread$thread successful" );
        } catch( Throwable t ) {
            /* Even though ThreadedTestCase.onThreads will batch up and throw
             * all the exceptions from the threads we go ahead and print a
             * a message here.  This gives an early warning in case there is
             * a long running thread that onThreads is waiting for before
             * dealing with any exceptions.
             */
            armLogger.error( "STOP Thread$thread failed" );
            testLogger.error( "Thread$thread threw an exception: " + t.toString() );
            synchronized( threadExceptions ) {
                threadExceptions["$thread"] = t;
            }
        } finally {
            endTime = System.currentTimeMillis();
            // When ARM Logging works (S0529780) this can be removed
            testLogger.info( "Thread$thread completed: " + (endTime - startTime) + " milliseconds" );
            synchronized( threadTimes ) {
                threadTimes["$thread"] = endTime - startTime;
            }
        }
    }
}
