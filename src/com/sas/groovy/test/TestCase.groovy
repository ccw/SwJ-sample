package com.sas.groovy.test;

import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

import java.lang.reflect.Method;

/* The TestCase class provides a harness for writing tests. Extending this class
 * gives base classes access to a default set of properties read from a properties
 * file specified on the command line as -Dprops=<filename>.  It also provides a
 * runAllTests method that behaves like a JUnit test runner.  It will run all the
 * methods that start with "test" wrapped with calls to setUp and tearDown.  It
 * will also output some statistics about tests that ran as well as the time it
 * took.  This class expects the following.
 *
 *    -Dhelp
 *        Print the properties supported by this test and exit.
 *
 *    -DforceExit
 *        Make a call to System.exit(0) when the run() method is finished.  This
 *        is needed on R64 where there is a hang that I have been unable to resolve.
 *
 *    -Dscript.props
 *        An optional properties file to use as the default script properties for
 *        the test. Tests that use the TestCase Groovy Class should always be started
 *        with runvjrscript. That launcher script figures out where the Groovy script
 *        is located and adds -Dscript.props=<test_name>.properties to the command line
 *        for you. It assumes that the <test_name>.properties file is located in the same
 *        directory as the Groovy script itself. A user should never have to manually
 *        set this property but it is available if the need arises.
 *
 *    -Dconfig.props
 *        An optional properties file that is intended to be pointed at a SAS install
 *        configuration.properties file.
 *
 *    -Dprops
 *        An optional properties file to use as the default user properties for the
 *        test. Properties in this file will override properties specified in
 *        script.props.  The default for this property is user.properties.
 *
 */

class TestCase implements Runnable {
    static Logger appLogger = Logger.getLogger("App");
    static Logger testLogger = Logger.getLogger("com.sas.groovy.test.TestCase");
    static Logger armLogger = Logger.getLogger("Perf.ARM.com.sas.groovy.test.TestCase");
    Properties defaultProperties = null;

    void printHelp() {
        println """
=============Test Case Properties=============

help
Print this message and exit.

forceExit
Make a call to System.exit(0) when the run() method is finished.  This is needed on R64 where there is a hang that I have been unable to resolve.

script.props
An optional properties file to use as the default script properties for the test. Tests that use the TestCase Groovy Class should always be started with runvjrscript. That launcher script figures out where the Groovy script is located and adds -Dscript.props=<test_name>.properties to the command line for you. It assumes that the <test_name>.properties file is located in the same directory as the Groovy script itself. A user should never have to manually set this property but it is available if the need arises.

config.props
An optional properties file that is intended to be pointed at a SAS install configuration.properties file.

props
An optional properties file to use as the default user properties for the test. Properties in this file will override properties specified in script.props.  The default for this property is user.properties."""
    }

    void setUp() {
        /* If Log4J hasn't been set up with appenders then do it now.
         * Note that Platform Services set up appenders by default so
         * to try to make all the tests behave the same we copy that
         * format here. */
        if( !hasAppender( testLogger ) || !hasAppender( armLogger ) ) {
            Logger testLogger = Logger.getRootLogger();
            ConsoleAppender appender = new ConsoleAppender( new PatternLayout("%d [%t] %-5p %c - %m%n") );
            testLogger.addAppender( appender );
        }
    }

    void tearDown() {
        /* Nothing to do */
    }

    void run() {
        Properties p = getProperties();
        if( p.containsKey( "help" ) ) {
            printHelp();
            return;
        }
        long start = System.currentTimeMillis();
        def errors = [];
        def errorsDesc = [];
        long tests = 0;
        Method setUp = this.class.getMethod( "setUp", null );
        Method tearDown = this.class.getMethod( "tearDown", null );
        this.class.methods.each{
            if( it.name.startsWith( "test" ) ) {
                ++tests;
                try{
                    if( setUp ){
                        setUp.invoke( this, null );
                    }
                    try {
                        it.invoke( this, null );
                    } catch( Throwable t ) {
                        errors.push( t.getCause() );
                        errorsDesc.push( it.name + "(" + this.class.name + ")" );
                    } finally {
                        if( tearDown ) {
                            try {
                                tearDown.invoke( this, null );
                            } catch( Throwable t ) {
                                errors.push( t.getCause() );
                                errorsDesc.push( "tearDown(" + this.class.name + ")" );
                            }
                        }
                    }
                } catch( Throwable t ) {
                    errors.push( t.getCause() );
                    errorsDesc.push( "setUp(" + this.class.name + ")" );
                }
            }
        }
        long elapsed = System.currentTimeMillis() - start;

        testLogger.info( "Total time: " + elapsed/1000F + " seconds" );
        if( errors.size > 0 ) {
            testLogger.error( "FAILURES!!! (" + tests + (tests==1 ? " test" : " tests") + ", " +
                                         errors.size + (errors.size==1 ? " error" : "errors") + ")" );
        } else {
            testLogger.info( "OK (" + tests + (tests==1 ? " test" : " tests") + ")" );
        }

        /* Attempting to match the output from JUnit */
        println "\nTime: " + elapsed/1000F + " seconds";
        if( errors.size > 0 ) {
            println "There " + (errors.size==1 ? "was " : "were ") + errors.size + (errors.size==1 ? " error:" : " errors:");
            for( int i = 0; i < errors.size; ++i ) {
                print "" + (i+1).toString() + ") " + errorsDesc[i];
                errors[i].printStackTrace( System.out );
            }
            println "\nFAILURES!!!"
            println "Tests run: " + tests + ",  Errors: " + errors.size;

        } else {
            println "\nOK (" + tests + (tests==1 ? " test" : " tests") + ")";
        }
        /* This is a kludge b/c I can't figure out what is leaking
         * and or hanging on R64.  By default these tests will not call
         * System.exit(0), the run() method will just end.  This works
         * on all platforms except for R64 and is appropriate for trying
         * to include multiple tests in a suite.  If you are running on
         * R64 or need an explicit exit for any reason then set the
         * forceExit property.
         */
        if( p.containsKey( "forceExit" ) ) {
            System.exit(0);
        }
    }

    Boolean hasAppender( Logger logger ) {
        Boolean appenderExists = false;
        Logger l = logger;
        while( l ) {
            if( !( l.getAllAppenders() instanceof org.apache.log4j.helpers.NullEnumeration ) ) {
                appenderExists = true;
                break;
            }
            l = l.parent;
        }
        return appenderExists;
    }

    Properties getProperties() {
        synchronized( System.properties ) {
            if( defaultProperties == null ) {
                defaultProperties = new Properties();

                /* Start with the test's defaults */
                try{
                    defaultProperties.load( new FileInputStream( System.properties."script.props" ) );
                } catch( Throwable t ) { /* ignore */ }

                /* Override that with the local configuration defaults */
                try {
                    defaultProperties.load( new FileInputStream( System.properties."config.props" ) );
                } catch( Throwable t ) { /* ignore */ }

                /* Override that with the user defaults */
                try{
                    String userProps = System.properties.getProperty( "props", "user.properties" );
                    defaultProperties.load( new FileInputStream( userProps ) );
                } catch( Throwable t ) { /* ignore */ }

                /* Override everything with what's on the command line */
                defaultProperties.putAll( System.properties );
            }
        }
        return defaultProperties;
    }
}
