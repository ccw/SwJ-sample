import com.sas.groovy.test.PlatformTestCase;

import java.util.TimeZone;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.Any;

import com.sas.services.connection.ConnectionInterface;

import com.sas.iom.SASIOMCommon.IServerLog;
import com.sas.iom.SASIOMCommon.IServerLogHelper;
import com.sas.iom.SASIOMCommon.IIOMServerAppenderView;

import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.StringSeqHelper;
import com.sas.iom.SASIOMDefs.DateTimeSeqHelper;

import com.sas.net.util.DateConverter;


/* The PlatformServerLog class connects to an IOM server defined in metadata
 * and prints the contents of the server log as maintained by the IOM Server
 * Appender.  The Log4SAS configuration file used by the server must include
 * an IOM Server Appender or there will be no log output from this test.
 * In addition to the PlatformTestCase Class, ThreadedTestCase Class, and
 * TestCase Class properties this test expects the following.
 *
 * -Dfilter:  An optional filter string that controls which events are returned.
 *            See http://sww.sas.com/sas/dev/mva/tkiomc/grm/sappij.html#sappIfaceReadEntriesV1.0
 *            for the format of this string.  The default is "columns=message".
 *
 * -DeventsPerRead: A optional number of events to read per call to the server.  Default is 1000.
 *
 * Example command lines
 *
 * Print the default log over a User connection to a Metadata Server (prompted for logical name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 platformServerLog.groovy
 *
 * Print the INFO level messages, dates, and levels from the "SASApp - Logical Stored Process Server"
 * runvjrscript -Dfilter="level=INFO columns=(datetime,level,message)" -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logical.name="SASApp - Logical Stored Process Server" platformServerLog.groovy
 */

class PlatformServerLog extends PlatformTestCase {

    Logger logger        = Logger.getLogger(appLogger.getName() + ".PlatformServerLog");
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
        setUpWithServerConnectionCLI();
    }

    void testServerLog() {
        onExecutions { iteration, thread ->
            def user = getUser( thread );
            ConnectionInterface connection = connectionFactory.getConnection(user);
            org.omg.CORBA.Object obj = connection.getObject();
            IServerLog iLog = IServerLogHelper.narrow(obj);
            if( iLog == null ) {
                logger.error( "=====>IServerLog interface not supported by this server" );
                removeUser(user);
                connection.close();
                connection = null;
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
                }
            }
            logger.info( "=====>END server log" );
            removeUser(user);
            connection.close();
            connection = null;
        }
    }
}
