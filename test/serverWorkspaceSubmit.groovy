import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.iom.SAS.IWorkspace;
import com.sas.iom.SAS.IWorkspaceHelper;
import com.sas.iom.SAS.ILanguageService;

/* The ServerWorkspaceSubmit class tests submitting a local file to a workspace
 * server using a direct manual connection.  In addition to the ServerTestCase,
 * ThreadedTestCase, and TestCase properties this class expects the following:
 *
 * -Dinfile
 *     The path to a local file.  The contents of this file will be submitted
 *     the the workspace server.
 *
 * -Dprintlog
 *     If true (present on the command line) then print the execution log.
 *
 * -Dprintlist
 *     If true (present on the command line) then print the execution listing.
 *
 * -Dasync
 *     If true (present on the command line) then the Language Service is put into
 *     async mode before the submit is called.
 *
 * See com.sas.groovy.test.ServerTestCase for connection options.
 *
 * Example command lines
 *
 * Submit the contents of test.sas over an SSPI connection, one iteration of one thread
 * runvjrscript -Diomsrv.uri=iom://localhost:8591;Bridge -Dinfile=test.sas serverWorkspaceSubmit.groovy
 *
 * Submit the contents of test.sas over an SSPI connection, two iterations of four threads
 * runvjrscript -Diomsrv.uri=iom://localhost:8591;Bridge -Dinfile=test.sas -Diteration.count=2 -Dthread.count=4 serverWorkspaceSubmit.groovy
 *
 * Submit the contents of test.sas over a User connection, one iteration of one thread
 * runvjrscript -Diomsrv.uri=iom://localhost:8591;Bridge;USER=carynt\sasiom1,PASS=123456 -Dinfile=test.sas serverWorkspaceSubmit.groovy
 *
 */

class ServerWorkspaceSubmit extends ServerTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".ServerWorkspaceSubmit");
    String source = "";
    Boolean printlog = false;
    Boolean printlist = false;
    Boolean async = false;

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

infile
The path to a local file.  The contents of this file will be submitted the the workspace server.

printlog
If true (present on the command line) then print the execution log.

printlist
If true (present on the command line) then print the execution listing.

async
If true (present on the command line) then the Language Service is put into async mode before the submit is called.""";
    }

    void setUp() {
        Properties p = getProperties();
        if( !p.containsKey("infile") ) {
            throw new IllegalArgumentException( "infile argument is required" );
        } else {
            StringBuilder buffer = new StringBuilder();
            new File(p.getProperty("infile")).eachLine { line -> buffer.append(line); }
            source = buffer.toString();
        }
        printlog = p.containsKey( "printlog" );
        printlist = p.containsKey( "printlist" );
        async = p.containsKey( "async" );
        super.setUp();
    }

    void testWorkspaceSubmit() {
        onExecutions { iteration, thread ->
            IWorkspace iWorkspace = IWorkspaceHelper.narrow( getTopLevelObject( thread ) );
            ILanguageService iLanguage = iWorkspace.LanguageService();
            if( async ) {
                iLanguage.Async( true );
            }
            iLanguage.Submit( source );
            if( printlog ) {
                logger.info( "=====>Thread$thread: " + IOMHelper.ILanguageServiceLogToString( iLanguage ) );
            }
            if( printlist ) {
                logger.info( "=====>Thread$thread: " + IOMHelper.ILanguageServiceListToString( iLanguage ) );
            }
        }
    }
}
