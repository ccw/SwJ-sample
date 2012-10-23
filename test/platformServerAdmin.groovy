import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;
import com.sas.groovy.util.CLIIOMAdmin;

import com.sas.services.connection.ConnectionInterface;


/* The PlatformServerAdmin class connects to the admin interfaces of
 * any IOM server defined in metadata and gives the user a command
 * line shell for interacting with those interfaces.  In addition to
 * the PlatformTestCase, ThreadedTestCase, and TestCase options, this
 * class expects the following:
 *
 * -Dinitstmt :  A single command to execute before returning control to
 *               the user.  Executed before any -Dinfile argument.
 *
 * -Dinfile   :  A file which contains commands to execute before returning
 *               control to the user.
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

class PlatformServerAdmin extends PlatformTestCase {

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

initstmt
A single command to execute before returning control to the user.  Executed before any infile argument.

infile
A file which contains commands to execute before returning control to the user.""";
    }

    void setUp() {
        Properties p = getProperties();
        setUpWithServerConnectionCLI();
    }

    void testServerAdmin() {
        def user = getUser();
        ConnectionInterface connection = connectionFactory.getConnection(user);
        org.omg.CORBA.Object obj = connection.getObject();

        CLIIOMAdmin admin = new CLIIOMAdmin( obj, null, null );

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
        removeUser(user);
    }

}
