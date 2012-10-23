import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.iom.SASIOMCommon.IServerSessions;
import com.sas.iom.SASIOMCommon.IServerSessionsHelper;

import com.sas.iom.SASIOMDefs.UUIDSeqHolder;
import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.iom.SASIOMDefs.DoubleSeqHolder;
import com.sas.iom.orb.UUIDConverter;

/* The ServerServerSessions class lists the active sessions on a server using the
 * IServerSessions interface.   In addition to the properties supported by the
 * ServerTestCase, ThreadedTestCase classes, and TestCase classes this test expects
 * the following:
 *
 * -Duserfilter: Only list sessions for this user.
 *
 * Example command lines
 *
 * List sessions on a Metadata Server over a User connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 serverServerStatus.groovy
 *
 * List sessions on a Stored Process Server over an SSPI connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP serverServerStatus.groovy
 *
 * List sessions for user sasiom1@CARYNT on a Metadata Server over a trusted connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,TRUSTEDSAS -Duserfilter=sasiom1@CARYNT serverServerStatus.groovy
 *
 */

class ServerServerSessions extends ServerTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".ServerServerSessions");
    String userfilter = "";

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

userfilter
Only list sessions for this user.""";
    }

    void setUp() {
        Properties p = getProperties();
        userfilter = p.getProperty("userfilter");
        if( userfilter == null ) {
            userfilter = "";
        }
        super.setUp();
    }

    void testServerSessions() {
        onExecutions { iteration, thread ->
            org.omg.CORBA.Object obj = getTopLevelObject( thread );
            IServerSessions serverSessions = IServerSessionsHelper.narrow(obj);

            UUIDSeqHolder sessionIDs = new UUIDSeqHolder();
            StringSeqHolder owners = new StringSeqHolder();
            DoubleSeqHolder inactiveTimes = new DoubleSeqHolder();

            serverSessions.SessionList( userfilter, sessionIDs, owners, inactiveTimes );
            for( int i = 0; i < sessionIDs.value.length; ++i ) {
                logger.info( owners.value[i] );
                logger.info( "\tSession ID    : " + UUIDConverter.uuidToString( sessionIDs.value[i] ).toUpperCase() );
                logger.info( "\tInactive time : " + inactiveTimes.value[i] );
            }
        }
    }
}
