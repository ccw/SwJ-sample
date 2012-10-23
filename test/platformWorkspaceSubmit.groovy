import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;

import com.sas.services.user.UserServiceInterface;
import com.sas.services.user.UserContextInterface;

import com.sas.services.information.RepositoryInterface;
import com.sas.services.information.FilterComponent;
import com.sas.services.information.metadata.OMRLogicalServer
import com.sas.services.information.metadata.LogicalServerFilter;

import com.sas.services.session.SessionServiceInterface;
import com.sas.services.session.SessionContextInterface;

import com.sas.services.connection.platform.PlatformConnectionFactoryConfiguration;
import com.sas.services.connection.platform.PlatformConnectionFactoryManager;
import com.sas.services.connection.platform.PlatformConnectionFactoryInterface;
import com.sas.services.connection.ConnectionInterface;

import com.sas.services.deployment.CorePlatformServices;

import com.sas.iom.SAS.IWorkspace;
import com.sas.iom.SAS.IWorkspaceHelper;
import com.sas.iom.SAS.ILanguageService;

import org.apache.log4j.Logger;

/*
 * The PlatformWorkspaceSubmit class extends the PlatformTestCase to
 * test all the different kinds of pooling that Workspace Servers support.
 * If given a Server name that is defined in Metadata then it connects and submits the
 * contents of the file specified in the infile argument.  If no Server
 * name is given then the user will be prompted to select from a list
 * of the servers available in the metadata server.  In addition to the
 * PlatformTestCase and ThreadedTestCase options, this class expects the
 * following:
 *
 *     -Dpooling           : "client" for client side pooling, "server" for server side
 *                           pooling, and leave the argument off for no pooling.
 *     -Dpooladminname     : If using client side pooling then this is the name of the
 *                           pooling administrator.
 *     -Dpooladminpassword : If using client side pooling then this is the password of the
 *                           pooling administrator.
 *     -Dinfile            : The path to a .sas file to submit to the Workspace Server
 *     -Dprintlog          : If true (present on the command line) then print the execution
 *                           log.
 *     -Dprintlist         : If true (present on the command line) then print the execution
 *                           listing.
 *     -Dasync             : If true (present on the command line) then the Language Service is put into
 *                           async mode before the submit is called.
 *
 * Example command lines
 *
 * Regular user authentication (prompted for password and server name) no pooling
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Dinfile=test.sas -Diomsrv.username=carynt\sasiom1 platformWorkspaceSubmit.groovy
 *
 * SSPI authentication client-side pooling
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname=LogicalServerName -Dinfile=test.sas -Dpooling=client platformWorkspaceSubmit.groovy
 *
 * Trusted Peer authentication server-side pooling
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname=LogicalServerName -Dinfile=test.sas -Diomsrv.trustedpeer -Dpooling=server platformWorkspaceSubmit.groovy
 *
 * Trusted User authentication (prompted for trusted password but not user password) no pooling
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.logicalname=LogicalServerName -Dinfile=test.sas -Diomsrv.username=carynt\sasiom1 -Diomsrv.trusteduser -Doma.person.trustusr.login.userid=sastrust@saspw platformWorkspaceSubmit.groovy
 *
 */

class PlatformWorkspaceSubmit extends PlatformTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformWorkspaceSubmit");
    RepositoryInterface repository = null;
    PlatformConnectionFactoryInterface connectionFactory = null;
    UserContextInterface userContext = null;
    UserContextInterface poolAdminContext = null;
    SessionContextInterface sessionContext = null;
    String source = null;
    Boolean printlog = false;
    Boolean printlist = false;
    Boolean async = false;

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

pooling
\"client\" for client side pooling, \"server\" for server side pooling, and leave the argument off for no pooling.

pooladminname
If using client side pooling then this is the name of the pooling administrator.

pooladminpassword
If using client side pooling then this is the password of the pooling administrator.

infile
The path to a .sas file to submit to the Workspace Server

printlog
If true (present on the command line) then print the execution log.

printlist
If true (present on the command line) then print the execution listing.

async
If true (present on the command line) then the Language Service is put into async mode before the submit is called.""";
    }

    void setUp() {
        Properties p                = getProperties();
        promptForAuthenticationCLI( p );

        String serverName           = p.getProperty("iomsrv.logical.name");
        String pooling              = p.getProperty("pooling");
        String pooladminname        = p.getProperty("pooladminname");
        String pooladminpassword    = p.getProperty("pooladminpassword");
        String infile               = p.getProperty("infile");
        String userName             = p.getProperty("iomsrv.username");
        String password             = p.getProperty("iomsrv.password");
        String domain               = p.getProperty("iomsrv.metadatasrv.authdomain", "DefaultAuth");
        printlog                    = p.containsKey( "printlog" );
        printlist                   = p.containsKey( "printlist" );
        async                       = p.containsKey( "async" );

        if( infile == null ) {
            throw new IllegalArgumentException( "infile argument is required" );
        } else {
            StringBuilder buffer = new StringBuilder();
            new File(p.getProperty("infile")).eachLine { line -> buffer.append(line); }
            source = buffer.toString();
        }

        /* If using client-side pooling and there was a pooladminname but no pooladminpassword
         * then prompt.
         */
        if( pooling == "client" && pooladminname != null && pooladminpassword == null ) {
            print "Password for $pooladminname: ";
            pooladminpassword = new jline.ConsoleReader().readLine( "*".charAt(0) );
        }

        /* Let the PlatformTestCase handle setting up our local deployment.
         */
        super.setUp();

        /* Now proceed with this test's unique platform deployment setup.
         */
        UserServiceInterface userService = CorePlatformServices.getUserService();
        SessionServiceInterface sessionService = CorePlatformServices.getSessionService();

        if( userName == null ) {
            userContext = userService.newUser( null, null );
        } else {
            userContext = userService.newUser( userName, password, domain );
        }

        repository = userContext.authServer.getRepository( repositoryName );
        logger.info( "=====>Connected to " + repository );

        /* Find the server def requested by the user.  If the user did not specify a
         * logical server to run against then list the available ones and prompt.
         */
        OMRLogicalServer serverDef = null;
        LogicalServerFilter filter = null;
        if( serverName == null ) {
            filter = new LogicalServerFilter();
        } else {
            filter = new LogicalServerFilter( "Name", FilterComponent.EQUALS, serverName );
        }

        serverDef = promptForServerCLI( repository, filter );
        if( serverDef == null ) {
            throw new IllegalArgumentException( "Unknown serverName $serverName" );
        }

        logger.info( "=====>Stressing " + serverDef.getName() );

        /* Create a session context for passing our pooling preference
         * down to the connection code.
         */
        sessionContext = sessionService.newSessionContext(userContext);
        PlatformConnectionFactoryConfiguration cfg = null;
        if( pooling == "client" ) {
            poolAdminContext = userService.newUser( pooladminname, pooladminpassword, domain );
            poolAdminContext.setKey("PLATFORMWORKSPACESUBMIT.GROOVY.POOLADMIN");
            userService.setUser(poolAdminContext);
            sessionContext.setAttribute( PlatformConnectionFactoryConfiguration.PROPERTYNAME_FACTORY_REQ_CLIENT_POOLING,"TRUE" );
            cfg = new PlatformConnectionFactoryConfiguration(serverDef, "PLATFORMWORKSPACESUBMIT.GROOVY.POOLADMIN");
        } else if( pooling == "server" ) {
            sessionContext.setAttribute( PlatformConnectionFactoryConfiguration.PROPERTYNAME_FACTORY_REQ_SERVER_POOLING,"TRUE" );
            cfg = new PlatformConnectionFactoryConfiguration(serverDef);
        } else {
            sessionContext.setAttribute( PlatformConnectionFactoryConfiguration.PROPERTYNAME_FACTORY_REQ_STANDARD,"TRUE" );
            cfg = new PlatformConnectionFactoryConfiguration(serverDef);
        }

        /* Create a connection factory to be used by all the
         * threads for creating new connections
         */
        PlatformConnectionFactoryManager mgr = new PlatformConnectionFactoryManager();
        connectionFactory = mgr.getFactory(cfg);
    }

    void testWorkspaceSubmit() {
        /* Lock the session to eliminate warnings
         */
        try {
            def sessionLock = sessionContext.lock("platformWorkspaceSubmit.groovy");
            try {
                onExecutions { iteration, thread ->
                    def user = getUser( thread );
                    ConnectionInterface connection = null;
                    synchronized( connectionFactory ) {
                        connection = connectionFactory.getConnection(user);
                    }
                    org.omg.CORBA.Object obj = connection.getObject();
                    IWorkspace workspace = IWorkspaceHelper.narrow(obj);
                    ILanguageService languageService = workspace.LanguageService();
                    if( async ) {
                        languageService.Async( true );
                    }
                    languageService.Submit(source);
                    if( printlog ) {
                        logger.info( "=====>Thread$thread: " + IOMHelper.ILanguageServiceLogToString( languageService ) );
                    }
                    if( printlist ) {
                        logger.info( "=====>Thread$thread: " + IOMHelper.ILanguageServiceListToString( languageService ) );
                    }
                    connection.close();
                    removeUser(user);
                }
            } finally {
                sessionContext.unlock(sessionLock);
            }
        } catch ( Throwable t ) {
            /* When an exception happens tearDown() is skipped which causes hangs */
            tearDown();
            throw t;
        }
    }

    void tearDown() {
        CorePlatformServices.getUserService().removeUser(userContext);
        userContext.destroy();
        userContext = null;
        if( poolAdminContext!=null ) {
            CorePlatformServices.getUserService().removeUser(poolAdminContext);
            poolAdminContext.destroy();
            poolAdminContext = null;
        }
        connectionFactory = null;
        sessionContext.destroy();
        repository.close();
        super.tearDown();
    }

}
