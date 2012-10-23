import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sas.services.connection.ConnectionInterface;

import com.sas.iom.SASIOMCommon.IServerAdministration;
import com.sas.iom.SASIOMCommon.IServerAdministrationHelper;

/* The PlatformServerStop class stops an IOM server using the IServerAdministration
 * interface.  In addition to the options supported by the
 * PlatformTestCase and ThreadedTestCase classes, this test expects the following:
 *
 * -Ddefer
 *     When present, perform a deferred stop.  Otherwise perform an immediate stop.
 *
 * Stop a server over a User connection (prompted for a password and for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 platformServerAttributes.groovy
 *
 * Stop a server over an SSPI connection to the Stored Process Server named "SASApp - Logical Stored Process Server"
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerAttributes.groovy
 *
 * Stop a server over a Trusted Peer connection (prompted for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedpeer platformServerAttributes.groovy
 *
 * Stop a server over a Trusted User connection (prompted for trusted password and user name but not user password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trusteduser -Doma.person.trustusr.login.userid=sastrust@saspw -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerAttributes.groovy
 */

class PlatformServerStop extends PlatformTestCase {

    Boolean defer = false;

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

defer
When present, perform a deferred stop.  Otherwise perform an immediate stop.""";
    }

    void setUp() {
        Properties p = getProperties();
        defer = p.containsKey("defer");
        setUpWithServerConnectionCLI();
    }

    void testServerStop() {
        def user = getUser();
        ConnectionInterface connection = connectionFactory.getConnection(user);
        org.omg.CORBA.Object obj = connection.getObject();

        IServerAdministration iAdmin = IServerAdministrationHelper.narrow(obj);
        if( getProperties().containsKey("defer") ) {
            iAdmin.DeferredStopServer();
        } else {
            iAdmin.StopServer();
        }
        removeUser(user);
    }

}
