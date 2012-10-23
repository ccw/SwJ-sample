import groovy.util.AllTestSuite;
import junit.framework.TestResult;
import junit.framework.Test;
import junit.framework.TestListener;

/* The runAllTests groovy script runs all the Groovy JUnit tests that match
 * a combination of directory and filename pattern.  It uses the AllTestSuite
 * provided by Groovy.  This script is a basic JUnit test runner that simply
 * runs the tests and prints the results to stdout.  The options that can be
 * passed on the command line are just the AllTestSuite options.
 *
 *     -Dgroovy.test.dir
 *          The directory to search for groovy tests.
 *          Default = .
 *     -Dgroovy.test.pattern
 *          The ant fileset pattern to search below the dir.
 *          Default = *Test.groovy
 */

if( System.properties.'groovy.test.dir' == null ) {
    System.properties.'groovy.test.dir' = ".";
}

if( System.properties.'groovy.test.pattern' ) {
    System.properties.'groovy.test.pattern' = "*Test.groovy";
}

suite = AllTestSuite.suite();

println "Running " + suite.testCount() + " test(s).";

TestResult result = new TestResult();

class Listener implements TestListener {
    void addError( Test test, Throwable t ) {
        println "       ERROR: " + t;
    }
    void addFailure( Test test, junit.framework.AssertionFailedError t ) {
        println "       FAILURE: " + t;
    }
    void startTest( Test test ) {
        println "\n=====> Starting " + test.name;
    }
    void endTest( Test test ) {
        println "=====> Finished " + test.name;
    }
}

result.addListener( new Listener() );

suite.run(result);

println "\n===============";
println "Tests run: " + result.runCount();
println "Failures : " + result.failureCount();
result.failures().each{ println "           " + it }
println "Errors   : " + result.errorCount();
result.errors().each{ println "           " + it }
System.exit(0);
