package com.sas.groovy.test;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/* The ThreadedTestCase class provides a harness for writing tests that use
 * multiple iterations and multiple threads.  It provides two methods: onIterations
 * and onThreads.  They both take a range (or list) and a closure.  onIterations
 * runs the given closure sequentially for each element in the range (or list).
 * onThreads runs the given closure on a separate thread for each element in the
 * range (or list).  In both cases the current element from the range (or list)
 * is passed as the first argument to the closure.  There are two other forms
 * of onIterations and onThreads.  One form takes a delay argument for the number
 * of milliseconds to pause between processing each element in the range (or list).
 * The other form just takes a closure and expects the iteration count and dely
 * as well as the thread count and delay to be specified in the system properties.
 * Classes that extend this class can override the getProperties methods if they
 * want to explicitly set these properties rather than using what was on the command
 * line.
 *
 * -Diteration.count      : Default = 1
 * -Diteration.delay      : Default = 0
 * -Dthread.count         : Default = 1
 * -Dthread.delay         : Default = 0
 * -Diteration.minruntime : Default = 0
 * -Dexecution.order      : A property that controls whether the onExecutions method
 *      executes iterations of threads or threads of iterations.  One of
 *         iterationsofthreads : Execute iterations of threads.  This is the default.
 *         threadsofiterations : Execute threads of iterations.
 *
 * If an exception is taken in an iteration, that exception is allowed to flow out
 * to JUnit so that reporting can be preserved.  Exceptions that occur in threads
 * are batched up into a single ThreadedTestCaseException which is also then thrown
 * to JUnit.
 *
 * Times and exceptions are repported to the Log4J Logging facility.  Times are also
 * collected in maps using the elements of the range as keys.  If a test wants it's
 * own reporting then it should turn the logger levels down and use these maps when
 * the tests are done.  Note, however, that when a new iteration starts the thread times
 * from the previous run will be overwritten.  The times are also unreliable if
 * onIterations is nested within onThreads because the same iterations will be
 * compeeting for the slot in those maps.  If your test needs to be organized
 * that way it's best to use the logger output instead.
 *
 * Example:
 *
 * class MyTest extends ThreadedTestCase {
 *     void testSomething() {
 *         Logger rootLogger = Logger.getLogger("");
 *         rootLogger.setLevel( Level.SEVERE);
 *
 *         onIterations(1..5){ iteration ->
 *             println "I'm starting iteration $iteration";
 *             // Add iteration tests here
 *             onThreads(1..10){ thread ->
 *                 println "I'm starting thread $thread in iteration $iteration";
 *                 // Add thread tests here
 *             }
 *             for( t in threadTimes ) {
 *                 println "Thread " + t.key + " ran in " + t.value + " milliseconds";
 *             }
 *             // Add additional iteration tests here
 *         }
 *
 *         for( t in iterationTimes ) {
 *             println "Iteration " + t.key + " ran in " + t.value + " milliseconds";
 *         }
 *     }
 * }
 *
 *
 */

class ThreadedTestCase extends TestCase {
    def iterationTimes = [:];
    def threadTimes = [:];

    void printHelp() {
        super.printHelp();
        println """
=============Threaded Test Case Properties=============

iteration.count
The number of iterations to perform.  The default is 1.

iteration.delay
The amount of time to wait between each iteration.  Can be specified as either N or Nunits where N is a number and units can be either s for seconds, m for minutes, or h for hours.  If there is no explicit unit then seconds is assumed. The default is 0.

iteration.minruntime
The amount of time to make sure the test continues to run.  Can be specified as either N or Nunits where N is a number and units can be either s for seconds, m for minutes, or h for hours.  If there is no explicit unit then seconds is assumed.  The default is 0.

thread.count
The number of threads to execute at once.  The default is 1.

thread.delay
The amount of time to wait between starting each thread.  Can be specified as either N or Nunits where N is a number and units can be either s for seconds, m for minutes, or h for hours.  If there is no explicit unit then seconds is assumed.  The default is 0.

execution.order
A property that controls whether the onExecutions method executes iterations of threads or threads of iterations.  One of
    iterationsofthreads : Execute iterations of threads.  This is the default.
    threadsofiterations : Execute threads of iterations.""";
    }

    void onExecutions( closure ) {
        Properties p = getProperties();
        String order = p.getProperty( "execution.order", "iterationsofthreads" );
        switch( order ) {
            case "threadsofiterations":
                onThreads { thread ->
                    onIterations { iteration ->
                        closure( iteration, thread );
                    }
                }
                break;
            case "iterationsofthreads":
                onIterations { iteration ->
                    onThreads { thread ->
                        closure( iteration, thread );
                    }
                }
                break;
            default:
                throw IllegalArgumentException("Unknown execution order: $order");
                break;
        }
    }

    void onIterations( closure ) {
        Properties p = getProperties();
        onIterations(
            (0..<Integer.parseInt(p.getProperty( "iteration.count", "1") ) ),
            p.getProperty( "iteration.delay", "0"),
            p.getProperty( "iteration.minruntime", "0"),
            closure );
    }

    void onIterations( range, closure ) {
        Properties p = getProperties();
        onIterations(range,
            p.getProperty( "iteration.delay", "0"),
            p.getProperty( "iteration.minruntime", "0"),
            closure );
    }

    void onIterations( range, delay, closure ) {
        Properties p = getProperties();
        onIterations(range,
            delay,
            p.getProperty( "iteration.minruntime", "0"),
            closure );
    }

    long timeInMillis( String time ) {
        if( time.endsWith("s") ) {
            return Long.parseLong( time.substring( 0, time.length()-1 ) ) * 1000;
        } else if( time.endsWith("m") ) {
            return Long.parseLong( time.substring( 0, time.length()-1 ) ) * 60 * 1000;
        } else if( time.endsWith("h") ) {
            return Long.parseLong( time.substring( 0, time.length()-1 ) ) * 60 * 60 * 1000;
        } else /* assume seconds by default */{
            return Long.parseLong( time ) * 1000;
        }
    }

    void onIterations( range, String delay, String minruntime, closure ) {
        int iterationNum = 0;
        long minRunTimeMillis = timeInMillis( minruntime );
        long delayMillis = timeInMillis( delay );
        long overallStartTime = System.currentTimeMillis();
        // Groovy doesn't support do-while so we have to put the time
        // check at the top.  But if there is no limit then we still
        // want to do the loop once so add a check against the iterationNum.
        while( iterationNum == 0 || System.currentTimeMillis() - overallStartTime < minRunTimeMillis ) {
            for( i in range ) {
                synchronized( iterationTimes ) {
                    iterationTimes["$iterationNum"] = -1;
                }
                long startTime = System.currentTimeMillis();
                MDC.put( "iteration", iterationNum );
                armLogger.info( "START Iteration$iterationNum" );
                try {
                    closure(iterationNum);
                    armLogger.info( "STOP Iteration$iterationNum successful" );
                } catch ( Throwable t ) {
                    armLogger.error( "STOP Iteration$iterationNum failed" );
                    throw t;
                } finally {
                    long endTime = System.currentTimeMillis();
                    testLogger.info( "Iteration$iterationNum completed: " + (endTime - startTime) + " milliseconds" );
                    synchronized( iterationTimes ) {
                        iterationTimes["$iterationNum"] = endTime - startTime;
                    }
                    /* Let exceptions flow on out to JUnit so that they
                     * can be reported in the normal way */
                }
                if( delayMillis > 0 ) {
                    Thread.sleep(delayMillis);
                }
                ++iterationNum;
            }
        }
        MDC.remove( "iteration" );
    }

    void onThreads( closure ) {
        Properties p = getProperties();
        onThreads(
            (0..<Integer.parseInt(p.getProperty( "thread.count", "1") ) ),
            p.getProperty( "thread.delay", "0"),
            closure );

    }

    void onThreads( range, closure ) {
        Properties p = getProperties();
        onThreads( range,
            p.getProperty( "thread.delay", "0"),
            closure );
    }

    void onThreads( range, String delay, closure ) {
        def threadExceptions = [:];
        def threads = [];
        long delayMillis = timeInMillis( delay );

        for( i in range ) {
            synchronized( threadTimes ) {
                threadTimes["$i"] = -1;
            }
            Thread t = new Thread( new ClosureRunner( thread:i, threadTimes:threadTimes, threadExceptions:threadExceptions, closure:closure ) );
            t.start();
            threads.add( t );
            if ( delayMillis > 0 ) {
                Thread.sleep( delayMillis );
            }
        }

        /* Wait for all the threads to complete
         */
        threads.each{ it.join() }

        /* If there were thread failures we need to
         * let JUnit know somehow.
         */
        if( threadExceptions.size() > 0 ) {
            throw new ThreadedTestCaseException( threadExceptions:threadExceptions );
        }
    }

}
