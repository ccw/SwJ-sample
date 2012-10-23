import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sas.services.connection.ConnectionInterface;

import com.sas.iom.SASIOMCommon.IServerAdministration;
import com.sas.iom.SASIOMCommon.IServerAdministrationHelper;

import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.iom.SASIOMDefs.DateTimeSeqHolder;
import com.sas.net.util.DateConverter;
import java.text.SimpleDateFormat;

/* The PlatformServerClients class lists the clients of an IOM server
 * using the IServerAdministration interface.  This test expects no
 * properties other than those supported by the PlatformTestCase Groovy
 * Class, ThreadedTestCase Groovy Class, and TestCase Groovy Class classes. 
 *
 * List clients in a server over a User connection (prompted for a password and for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 platformServerAttributes.groovy
 *
 * List clients in a server over an SSPI connection to the Stored Process Server named "SASApp - Logical Stored Process Server"
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerAttributes.groovy
 *
 * List clients in a server over a Trusted Peer connection (prompted for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedpeer platformServerAttributes.groovy
 *
 * List clients in a server over a Trusted User connection (prompted for trusted password and user name but not user password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trusteduser -Doma.person.trustusr.login.userid=sastrust@saspw -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerAttributes.groovy
 */

class PlatformServerClients extends PlatformTestCase {

    void setUp() {
        setUpWithServerConnectionCLI();
    }

    void testServerStop() {
        def user = getUser();
        ConnectionInterface connection = connectionFactory.getConnection(user);
        org.omg.CORBA.Object obj = connection.getObject();
        IServerAdministration iAdmin = IServerAdministrationHelper.narrow(obj);
        
        StringSeqHolder clientsHolder = new StringSeqHolder();
        StringSeqHolder clientHostsHolder = new StringSeqHolder();
        DateTimeSeqHolder enterTimesHolder = new DateTimeSeqHolder();
        
        iAdmin.ListClients( clientsHolder, clientHostsHolder, enterTimesHolder );
        
        String[] clients = clientsHolder.value;
        String[] clientHosts = clientHostsHolder.value;
        long[] enterTimes = enterTimesHolder.value;
        
        Date d = null;
        SimpleDateFormat s   = new SimpleDateFormat("ddMMMyyyy hh:mm:ss,SSS a z");
        s.setTimeZone(TimeZone.getDefault());//TimeZone("GMT"));
        for( int i = 0; i < clients.length; ++i ) {
            d = DateConverter.corbaToJavaGMT( enterTimes[i] );
            println clients[i];
            println "\tHost       : " + clientHosts[i];
            println "\tEnter time : " + s.format(d);
        }

        removeUser(user);
    }

}
