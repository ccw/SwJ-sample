import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.iom.SASIOMCommon.IServerStatus;
import com.sas.iom.SASIOMCommon.IServerStatusHelper;


/* The ServerServerStatus class dumps all the information from the
 * IServerStatus interface of a remote server.  This test expects no
 * properties other than those supported by the ServerTestCase, ThreadedTestCase
 * and TestCase classes.
 *
 * Example command lines
 *
 * The status over a User connection to a Metadata Server
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 serverServerStatus.groovy
 *
 * The status over an SSPI connection to a Stored Process Server
 * runvjrscript -Diomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP serverServerStatus.groovy
 *
 * The status over a trusted connection to a Metadata Server
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,TRUSTEDSAS serverServerStatus.groovy
 *
 */

class ServerServerStatus extends ServerTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".ServerServerStatus");

    void testServerStatus() {
        onExecutions { iteration, thread ->
            org.omg.CORBA.Object obj = getTopLevelObject( thread );
            IServerStatus iStatus = IServerStatusHelper.narrow(obj);
            if( iStatus == null ) {
                logger.error( "=====>IServerStatus interface not supported by this server" );
                return;
            }
            logger.info( IOMHelper.IServerStatusToString( iStatus ) );
        }
    }
}
