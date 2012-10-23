import com.sas.groovy.test.ServerTestCase;

import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.Any;

import com.sas.iom.SASIOMCommon.IServerLog;
import com.sas.iom.SASIOMCommon.IServerLogHelper;
import com.sas.iom.SASIOMCommon.IIOMServerAppenderView;

import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.StringSeqHelper;
import com.sas.iom.SASIOMDefs.DateTimeSeqHelper;

import com.sas.net.util.DateConverter;


/* The ServerServerLog class connects to an IOM server and prints the contents
 * of the server log as maintained by the IOM Server Appender.  The Log4SAS
 * configuration file must include an IOM Server Appender or there will
 * be no log output from this test.  In addition to the ServerTestCase Class,
 * ThreadedTestCase Class, and TestCase Class properties this test expects the
 * following.
 *
 * -Dfilter:  An optional filter string that controls which events are returned.
 *            See http://sww.sas.com/sas/dev/mva/tkiomc/grm/sappij.html#sappIfaceReadEntriesV1.0
 *            for the format of this string.  The default is "columns=message".
 *
 * -DeventsPerRead: A optional number of events to read per call to the server.  Default is 1000.
 *
 * Example command lines
 *
 * Print the default log over a User connection to a Metadata Server
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 serverServerLog.groovy
 *
 * Print the INFO level messages and dates over an SSPI connection to a Stored Process Server
 * runvjrscript -Dfilter="level=INFO columns=(message, datetime) -Diomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP serverServerLog.groovy
 *
 */

class ServerServerLog extends ServerTestCase {

    Logger logger        = Logger.getLogger(appLogger.getName() + ".ServerServerLog");
    String filter        = null;
    int    eventsPerRead = 0;

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

filter
An optional filter string that controls which events are returned.  See http://sww.sas.com/sas/dev/mva/tkiomc/grm/sappij.html#sappIfaceReadEntriesV1.0 for the format of this string.  The default is \"columns=message\".

eventsPerRead
A optional number of events to read per call to the server.  Default is 1000.""";
    }

    void setUp() {
        Properties p = getProperties();
        filter = p.getProperty("filter", "columns=message");
        eventsPerRead = Integer.parseInt( p.getProperty("eventsPerRead", "1000") );
        super.setUp();
    }

    void testServerLog() {
        onExecutions { iteration, thread ->
            org.omg.CORBA.Object obj = getTopLevelObject( thread );
            IServerLog iLog = IServerLogHelper.narrow(obj);
            if( iLog == null ) {
                logger.error( "=====>IServerLog interface not supported by this server" );
                return;
            }
            IIOMServerAppenderView view = iLog.GetIOMServerAppenderView( filter );
            Boolean      done    = false;
            AnySeqHolder entries = new AnySeqHolder();
            IntHolder    numRead = new IntHolder();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
            sdf.setTimeZone(TimeZone.getDefault());

            logger.info( "=====>BEGIN server log" );
            while( !done )
            {
                try
                {
                    view.ReadEntries( eventsPerRead, entries, numRead );
                    Any[]    values           = entries.value;
                    Object[] lists            = new Object[values.length];
                    for( int i = 0; i < values.length; ++i )
                    {
                        Any anyValue = values[i];
                        if( anyValue.type().name().equals( StringSeqHelper.type().name() ) )
                        {
                            lists[i] = StringSeqHelper.extract( anyValue );
                        }
                        else if( anyValue.type().name().equals( DateTimeSeqHelper.type().name() ) )
                        {
                            lists[i] = DateTimeSeqHelper.extract( anyValue );
                        }
                    }
                    for( int j = 0; j < lists[0].length; ++j )
                    {
                        StringBuilder buffer = new StringBuilder();
                        for( int k = 0; k < lists.length; ++k )
                        {
                            if( k > 0 )
                            {
                                buffer.append( ", " );
                            }
                            if( lists[k][j] instanceof Long )
                            {
                                Date d = DateConverter.corbaToJavaGMT(lists[k][j]);
                                buffer.append( sdf.format(d)  );
                            }
                            else
                            {
                                buffer.append( lists[k][j].toString() );
                            }
                        }
                        logger.info( buffer.toString() );
                    }
                    if( numRead.value < eventsPerRead )
                    {
                        done = true;
                    }
                }
                catch( Throwable t )
                {
                    logger.info( t.toString() );
                    done = true;
                    //throw t;
                }
            }
            logger.info( "=====>END server log" );
        }
    }
}
