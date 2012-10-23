import com.sas.groovy.test.ThreadedTestCase;
//import org.apache.tools.ant.taskdefs.optional.net.RExecTask;

/* The ThreadedRexec class uses the ant RExecTask to submit one command
 * to a remot host.  In addition to the ThreadedTestCase and TestCase
 * properties this class expects the following:
 *
 * -Dhost      :  The host name of the server to submit the command to.
 *
 * -Dcommand   :  The command to submit.
 *
 * -Dusername  :  The user name to use when logging into the remote host.
 *
 * -Dpassword  :  The password for the username.  If not present the user
 *                will be prompted.
 *
 * Example command lines
 *
 * Submitting the "pwd" command to blueman.
 * runvjrscript -Dhost=blueman -Dcommand=pwd -Dusername=sasiom1 -Dpassword=bogus -Dthread.count=4 threadedRexec.groovy
 *
 * Submitting the "pwd" command to blueman on 4 threads.  Propted for the password.
 * runvjrscript -Dhost=blueman -Dcommand=pwd -Dusername=sasiom1 -Dthread.count=4 threadedRexec.groovy
 */

class ThreadedRexec extends ThreadedTestCase {

    String serverName = "";
    String command    = "";
    String username   = "";
    String password   = "";

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

host
The host name of the server to submit the command to.

command
The command to submit.

username
The user name to use when logging into the remote host.

password
The password for the username.  If not present the user will be prompted.""";
    }

    void setUp() {
        Properties p = getProperties();
        serverName = p.getProperty("host");
        command = p.getProperty("command");
        username = p.getProperty("username");
        password = p.getProperty("password");
        if( username && !password ) {
            print "Password for $username: ";
            password = new jline.ConsoleReader().readLine( "*".charAt(0) );
            p.put( "password", password);
        }
        super.setUp();
    }

    void testThreadedRexec() {
        onExecutions { iteration, thread ->
//            RExecTask t = new RExecTask();
//            t.setCommand( command );
//            t.setUserid( username );
//            t.setPassword( password );
//            t.setServer( serverName );
//            t.execute();
        }
    }
}
