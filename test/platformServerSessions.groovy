import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.services.connection.ConnectionInterface;

import com.sas.iom.SASIOMCommon.IServerSessions;
import com.sas.iom.SASIOMCommon.IServerSessionsHelper;

import com.sas.iom.SASIOMDefs.UUIDSeqHolder;
import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.iom.SASIOMDefs.DoubleSeqHolder;
import com.sas.iom.orb.UUIDConverter
import com.sas.services.deployment.CorePlatformServices;

/* The PlatformServerSessions class lists the active sessions on a server using the
 * IServerSessions interface.   In addition to the properties supported by the
 * PlatformTestCase, ThreadedTestCase, and TestCase classes this test expects the following:
 *
 * -Duserfilter: Only list sessions for this user.
 *
 * Example command lines
 *
 * List sessions on a Metadata Server over a User connection (prompted for server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 -Diomsrv.password=123456 platformServerSessions.groovy
 *
 * List sessions on a Stored Process Server named "StoredProcessServer" over an SSPI connection.
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname=StoredProcessServer platformServerSessions.groovy
 *
 * List sessions for user sasiom1@CARYNT on a Metadata Server over a trusted connection.
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedpeer -Duserfilter=sasiom1@CARYNT platformServerSessions.groovy
 *
 */

class PlatformServerSessions extends PlatformTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformServerSessions");
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
        setUpWithServerConnectionCLI();
    }

    void testServerSessions() {
        onExecutions { iteration, thread ->
            def user = getUser( thread );
            //def session = CorePlatformServices.sessionService.newSessionContext(CorePlatformServices.userService.newUser("sassrv@SAS-TEST", "Sas1234"))
            ConnectionInterface connection = connectionFactory.getConnection(user);
            org.omg.CORBA.Object obj = connection.getObject();

            IServerSessions serverSessions = IServerSessionsHelper.narrow(obj);

            UUIDSeqHolder sessionIDs = new UUIDSeqHolder();
            StringSeqHolder owners = new StringSeqHolder();
            DoubleSeqHolder inactiveTimes = new DoubleSeqHolder();

            serverSessions.SessionList( userfilter, sessionIDs, owners, inactiveTimes );
            for( int i = 0; i < sessionIDs.value.length; ++i ) {
                logger.info( owners.value[i] );
                logger.info(  "\tSession ID    : " + UUIDConverter.uuidToString( sessionIDs.value[i] ).toUpperCase() );
                logger.info( "\tInactive time : " + inactiveTimes.value[i] );
            }

            removeUser( user );
            connection.close();
            connection = null;
        }
    }
}
