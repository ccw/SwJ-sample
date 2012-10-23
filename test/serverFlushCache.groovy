import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.iom.SASIOMCommon.IServerAdministration;
import com.sas.iom.SASIOMCommon.IServerAdministrationHelper;


/* The ServerFlushCache flushes the authorization cache of an
 * IOM server using the IServerAdministration interface.  This
 * test expects no properties other than those supported by the
 * ServerTestCase, ThreadedTestCase, and TestCase classes.
 *
 * Example command lines
 *
 * Flush the cache of a Metadata Server over a User connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 serverFlushCache.groovy
 *
 * Flush the cache of a Stored Process Server over an SSPI connection.
 * runvjrscript -iomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP serverFlushCache.groovy
 *
 * Flush the cache of a Metadata Server over a trusted connection.
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,TRUSTEDSAS serverFlushCache.groovy
 *
 */

class ServerFlushCache extends ServerTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".ServerFlushCache");

    void testFlushCache() {
        onExecutions { iteration, thread ->
            org.omg.CORBA.Object obj = getTopLevelObject( thread );
            IServerAdministration iAdmin = IServerAdministrationHelper.narrow(obj);
            if( !iAdmin ) {
                logger.error( "ERROR: This server does not support the IServerAdministration interface" );
                return;
            }
            iAdmin.CacheFlush();
        }
    }
}
