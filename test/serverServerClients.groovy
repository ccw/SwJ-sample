import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sas.iom.SASIOMCommon.IServerAdministration;
import com.sas.iom.SASIOMCommon.IServerAdministrationHelper;

import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.iom.SASIOMDefs.DateTimeSeqHolder;
import com.sas.net.util.DateConverter;
import java.text.SimpleDateFormat;

/* The ServerServerClients class lists the clients of an IOM server using
 * the IServerAdministration interface.  This test expects no properties
 * other than those supported by the ServerTestCase Groovy Class,
 * ThreadedTestCase Groovy Class, and TestCase Groovy Class classes. 
 *
 * Example command lines
 *
 * List clients in a Metadata Server over a User connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 serverServerStatus.groovy
 *
 * List clients in a Stored Process Server over an SSPI connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP serverServerStatus.groovy
 *
 * List clients in a Metadata Server over a trusted connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,TRUSTEDSAS serverServerStatus.groovy
 *
 */

class ServerServerClients extends ServerTestCase {

    void testServerStop() {
        org.omg.CORBA.Object obj = getTopLevelObject();
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
    }
}
