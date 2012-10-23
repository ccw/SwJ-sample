package com.sas.groovy.test;

class ThreadedTestCaseException extends Error {
    def threadExceptions = [:]
    String toString() {
        StringBuffer buffer = new StringBuffer();
        def i = 0;
        for( e in threadExceptions ) {
            if( i != 0 ) {
                buffer.append( "\n" );
            }
            ++i;
            buffer.append("Thread " + e.key + " threw an exception: " + e.value );
        }
        return buffer.toString();
    }
}
