import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.services.connection.ConnectionInterface;

import com.sas.iom.SASIOMCommon.IServerStatus;
import com.sas.iom.SASIOMCommon.IServerStatusHelper;


/* The PlatformServerStatus class dumps all the information from the
 * IServerStatus interface of a server defined in metadata.  This test
 * expects no options other than those supported by the PlatformTestCase,
 * ThreadedTestCase and TestCase classes.
 *
 * The status over a User connection (prompted for a password and for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 platformServerStatus.groovy
 *
 * The status over an SSPI connection to the Stored Process Server named "SASApp - Logical Stored Process Server"
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerStatus.groovy
 *
 * The status over a Trusted Peer connection (prompted for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedpeer platformServerStatus.groovy
 *
 * The status over a Trusted User connection (prompted for trusted password and user name but not user password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trusteduser -Doma.person.trustusr.login.userid=sastrust@saspw -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerStatus.groovy
 */

class PlatformServerStatus extends PlatformTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformServerStatus");

    void setUp() {
        setUpWithServerConnectionCLI();
    }

    void testServerStatus() {

        onExecutions { iteration, thread ->
            def user = getUser( thread );
            ConnectionInterface connection = connectionFactory.getConnection(user);
            org.omg.CORBA.Object obj = connection.getObject();
            IServerStatus iStatus = IServerStatusHelper.narrow(obj);
            if( iStatus == null ) {
                logger.error( "=====>IServerStatus interface not supported by this server" );
                return;
            }

            logger.info( IOMHelper.IServerStatusToString( iStatus ) );

            removeUser( user );
            connection.close();
            connection = null;
        }
    }
}
