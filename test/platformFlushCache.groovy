import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.services.connection.ConnectionInterface;

import com.sas.iom.SASIOMCommon.IServerAdministration;
import com.sas.iom.SASIOMCommon.IServerAdministrationHelper;

/* The PlatformFlushCache class flushes the authorization cache of an
 * IOM server using the IServerAdministration interface.  This
 * test expects no options other than those supported by the
 * PlatformTestCase, ThreadedTestCase and TestCase classes.
 *
 * Flush the cache of a server over a User connection (prompted for a password and for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 platformFlushCache.groovy
 *
 * Flush the cache of a server over an SSPI connection to the Stored Process Server named "SASApp - Logical Stored Process Server"
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformFlushCache.groovy
 *
 * Flush the cache of a server over a Trusted Peer connection (prompted for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedpeer platformFlushCache.groovy
 *
 * Flush the cache of a server over a Trusted User connection (prompted for trusted password and user name but not user password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trusteduser -Doma.person.trustusr.login.userid=sastrust@saspw -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformFlushCache.groovy
 */

class PlatformFlushCache extends PlatformTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformFlushCache");

    void setUp() {
        setUpWithServerConnectionCLI();
    }

    void testCacheFlush() {
        onExecutions { iteration, thread ->
            def user = getUser( thread );
            ConnectionInterface connection = connectionFactory.getConnection(user);
            org.omg.CORBA.Object obj = connection.getObject();
            IServerAdministration iAdmin = IServerAdministrationHelper.narrow(obj);
            if( !iAdmin ) {
                logger.error( "ERROR: This server does not support the IServerAdministration interface" );
                return;
            }
            iAdmin.CacheFlush();
            removeUser(user);
        }
    }
}
