import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.CLIIOMAdmin;

/* The ServerServerAdmin class connects to the admin interfaces of
 * any IOM server and gives the user a command line shell for interacting
 * with those interfaces.  In addition to the properties supported by the
 * ServerTestCase, ThreadedTestCase, and TestCase this test expects the following:
 *
 * -Dinitstmt :  A single command to execute before returning control to
 *               the user.  Executed before any -Dinfile argument.
 *
 * -Dinfile   :  A file which contains commands to execute before returning
 *               control to the user.
 *
 * Example command lines
 *
 * Connect to a Metadata Server and execute the contents of cmds.txt then return user to a shell
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 -Dinfile=cmds.txt serverServerAdmin.groovy
 *
 * Connect over an SSPI connection to a Stored Process Server and return user to a shell
 * runvjrscript -Diomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP serverServerAdmin.groovy
 *
 * Connect over a trusted connection to a Metadata Server and return user to a shell
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,TRUSTEDSAS serverServerAdmin.groovy
 *
 */

class ServerServerAdmin extends ServerTestCase {

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

initstmt
A single command to execute before returning control to the user.  Executed before any infile argument.

infile
A file which contains commands to execute before returning control to the user.""";
    }

    void testServerAdmin() {
        /* Must call getProperties outside of the closures or it doesn't get
         * the right properties object back.  I can only assume that closures
         * get their own properties object and getProperties methods which is
         * masking the one from ThreadedTestCase... But that's just a guess.
         */
        Properties p = getProperties();
        def disconnect = {
            tearDown();
        }
        def connect = { argument ->
            tearDown();
            String uri = argument;
            if( uri == "" ) {
                println "The Connect command requires a uri argument";
                return null;
            }
            p.setProperty( "ServerTestCase.server.uri", uri );
            setUp();
            return getTopLevelObject();
        }

        CLIIOMAdmin admin = new CLIIOMAdmin( getTopLevelObject(), connect, disconnect );

        Boolean cont = true;

        /* If we have an initstmt then run it first.
         */
        String initstmt = getProperties().getProperty( "initstmt" );
        if( initstmt ) {
            cont = admin.execute( initstmt );
            if( !cont ) {
                return;
            }
        }

        /* If running from a script just process each line and return.
         * Any failures are reported and execution continues.
         * Leave the "session" open for any additional commands
         * from the user.
         */
        String inputFile = getProperties().getProperty( "infile" );
        if( inputFile ) {
            cont = admin.runFromFile( inputFile );
            if( !cont ) {
                return;
            }
        }

        /* If we made it here then we are in interactive mode so tell
         * the CLIIOMAdmin to run.  If we didn't have an input file
         * or an initstmt then go ahead and print the help.
         */
        if( !inputFile && !initstmt ) {
            println "";
            admin.execute( "help" );
        }
        admin.run();
    }

}
