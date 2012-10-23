import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.sas.iom.SASIOMCommon.IServerAdministration;
import com.sas.iom.SASIOMCommon.IServerAdministrationHelper;


/* The ServerServerStop class stops an IOM server using the IServerAdministration
 * interface.  In addition to the properties supported by the
 * ServerTestCase, ThreadedTestCase, and TestCase classes this test expects the following:
 *
 * -Ddefer : When present perform a deferred stop.  Otherwise perform an immediate stop.
 *
 * Example command lines
 *
 * Stop a Metadata Server over a User connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 serverServerStatus.groovy
 *
 * Stop a Stored Process Server over an SSPI connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP serverServerStatus.groovy
 *
 * Stop a Metadata Server over a trusted connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,TRUSTEDSAS serverServerStatus.groovy
 *
 */

class ServerServerStop extends ServerTestCase {

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

defer
When present perform a deferred stop.  Otherwise perform an immediate stop.""";
    }
    void testServerStop() {
        org.omg.CORBA.Object obj = getTopLevelObject();
        IServerAdministration iAdmin = IServerAdministrationHelper.narrow(obj);
        if( getProperties().containsKey("defer") ) {
            iAdmin.DeferredStopServer();
        } else {
            iAdmin.StopServer();
        }
    }
}
