package com.sas.groovy.test;

import com.sas.groovy.util.*;
import com.sas.services.information.RepositoryInterface;
import com.sas.services.information.metadata.OMRLogicalServer
import com.sas.services.information.Filter;

import com.sas.services.connection.platform.PlatformConnectionFactoryInterface;

import com.sas.services.user.UserContextInterface;
import com.sas.services.user.UserServiceInterface;

import com.sas.services.information.FilterComponent;
import com.sas.services.information.metadata.LogicalServerFilter;

import com.sas.services.connection.platform.PlatformConnectionFactoryConfiguration;
import com.sas.services.connection.platform.PlatformConnectionFactoryManager;

import com.sas.services.deployment.CorePlatformServices
import com.sas.apps.session.server.CoreServices;


/* The PlatformTestCase class provides a harness for writing tests that use
 * the various Platform Services.  It takes a set of properties and initializes
 * a PlatformServicesDeployment object (com.sas.groovy.util.PlatformServicesDeployment).
 * This includes setting up the JAAS authentication as well as finding and deploying all
 * the services.  Tests that extend this class can make use of the "deployment" member
 * variable to access the various services in the deployment.  Tests that extend this
 * class can either override the setUp method if they want a completely different deployment
 * configuration or they can override the ThreadedTestCase:getProperties method if they
 * want to explicitly specify the connection options.  One thing that can be configured without
 * overriding either of those methods is the JAAS configuration.  If the user has specified
 * the system property java.security.auth.login.config then no additional authentication
 * configuration will be attempted.  The options used by this class include the following:
 *
 *   -Diomsrv.metadatasrv.host
 *       The Metadata Server host name to use when configuring an OMILoginModule
 *       (user/pass authentication or IWA).  This is a SAS 9.2 Configuration
 *       Framework property.
 *
 *   -Diomsrv.metadatasrv.port
 *       The Metadata Server port to use when configuring an OMILoginModule
 *       (user/pass authentication or IWA).  This is a SAS 9.2 Configuration
 *       Framework property.
 *
 *   -Diomsrv.metadatasrv.authdomain
 *       The domain used for authentication (trusted and regular).  This is a SAS 9.2
 *       Configuration Framework property.  The default is DefaultAuth.
 *
 *   -Diomsrv.metadatasrv.trustedpeer
 *       If present (no value required) then a trusted peer connection is attempted.
 *       Otherwise an SSPI connection is attempted.
 *
 *   -Diomsrv.metadatasrv.trusteduser
 *       If present (no value required) then a trusted user connection is attempted.
 *       oma.person.trustusr.login.userid is required if this property is present.
 *
 *   -Doma.repository.foundation.name
 *       The repository used for authentication (trusted and regular).  This is a SAS
 *       9.2 Configuration Framework property.
 *
 *   -Doma.person.trustusr.login.userid
 *       If present, causes a TrustedLoginModule to be used and enables trusted
 *       user authentication.  This is a SAS 9.2 Configuration Framework property.
 *
 *   -Doma.person.trustusr.login.password
 *       Required for trusted user authentication.  This is a SAS 9.2 Configuration
 *       Framework property.
 *
 *   -Diomsrv.logical.name
 *       The name of the server to contact.  If not present then the the user will be
 *       prompted with a list of servers defined in metadata.
 *
 *   -Diomsrv.username
 *       The user name to use when logging into the Metadata Server and the target
 *       server (if not present then an sspi connection is attempted)
 *
 *   -Diomsrv.password
 *       The password to use when logging in.  If there is a userName and no password
 *       then the user will be prompted.
 *
 *   -Diomsrv.rduserperthread
 *       When present, a different rduser will be used for each thread making a connection
 *       to the server.  Note that when setUpWithServerConnection() is called that the initial
 *       connection to the metadata server will be made using the values for iomsrv.username,
 *       iomsrv.password, iomsrv.trustedpeer, and iomsrv.trusteduser.  This is because that
 *       initial connection is not made in one of the test threads.
 *
 *   -Diomsrv.rduserformat
 *       The format used to generate the userids used by iomsrv.rduserperthread.  The default
 *       is "rd\rdtest%04d".
 *
 *   -Diomsrv.rduserpassword
 *       The password used for all the rdusers.  The default is the password for rd\rdtestXXXX.
 *
 * NOTE: If both iomsrv.metadatasrv.trustedpeer and iomsrv.metadatasrv.trusteduser
 *       are specified then a trusted user connection will be used.  Trusted peer
 *       will NOT be used.
 *
 * NOTE: When making a trusted user connection a regular user name that exists in
 *       the Metadata Server must still be specified but neither SSPI nor trustedPeer
 *       connections require a user name.
 */

class PlatformTestCase extends ThreadedTestCase {
    PlatformConnectionFactoryInterface  connectionFactory   = null;
    String                              repositoryName      = "Foundation";
    def                                 users               = [];

    void printHelp() {
        super.printHelp();
        println """
=============Platform Test Case Properties=============

iomsrv.metadatasrv.host
The Metadata Server host name to use when configuring an OMILoginModule (user/pass authentication or IWA).  This is a SAS 9.2 Configuration Framework property.

iomsrv.metadatasrv.port
The Metadata Server port to use when configuring an OMILoginModule (user/pass authentication or IWA).  This is a SAS 9.2 Configuration Framework property.

iomsrv.metadatasrv.authdomain
The domain used for authentication (trusted and regular).  This is a SAS 9.2 Configuration Framework property.  The default is DefaultAuth.

iomsrv.metadatasrv.trustedpeer
If present (no value required) then a trusted peer connection is attempted. Otherwise an SSPI connection is attempted.

iomsrv.metadatasrv.trusteduser
If present (no value required) then a trusted user connection is attempted.  oma.person.trustusr.login.userid is required if this property is present.

oma.repository.foundation.name
The repository used for authentication (trusted and regular).  This is a SAS 9.2 Configuration Framework property.

oma.person.trustusr.login.userid
If present, causes a TrustedLoginModule to be used and enables trusted user authentication.  This is a SAS 9.2 Configuration Framework property.

oma.person.trustusr.login.password
Required for trusted user authentication.  This is a SAS 9.2 Configuration Framework property.

iomsrv.logical.name
The name of the server to contact.  If not present then the the user will be prompted with a list of servers defined in metadata.

iomsrv.username
The user name to use when logging into the Metadata Server and the target server (if not present then an sspi connection is attempted)

iomsrv.password
The password to use when logging in.  If there is a userName and no password then the user will be prompted.

iomsrv.rduserperthread
When present, a different rduser will be used for each thread making a connection to the server.  Note that when setUpWithServerConnection() is called that the initial connection to the metadata server will be made using the values for iomsrv.username, iomsrv.password, iomsrv.trustedpeer, and iomsrv.trusteduser.  This is because that initial connection is not made in one of the test threads.

iomsrv.rduserformat
The format used to generate the userids used by iomsrv.rduserperthread.  The default is \"rd\\rdtest%04d\".

iomsrv.rduserpassword
The password used for all the rdusers.  The default is the password for rd\rdtestXXXX.""";
    }

    void setUp() {
        internalSetUp();
        super.setUp();
    }

    void setUpWithAuthenticationCLI() {
        Properties p = getProperties();
        promptForAuthenticationCLI( p );
        internalSetUp()
        super.setUp();
    }

    void setUpWithServerConnectionCLI() {
        Properties p = getProperties();
        promptForAuthenticationCLI( p );

        String serverName = p.getProperty("iomsrv.logical.name");

        /* Set up our local deployment.
         */
        internalSetUp();
        super.setUp();

        /* Now find the server def and setup the connection objects.
         * If this portion throws an exception then tearDown should
         * be called to clean up.
         */
        try {
            UserContextInterface userContext = getStartupUser();

            RepositoryInterface repository = userContext.authServer.getRepository( repositoryName );
            println "=====>Connected to $repository";

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

            println "=====>Connected to ${serverDef.getName()}";

            /* Create a connection factory to be used by all the
             * threads for creating new connections
             */
            PlatformConnectionFactoryConfiguration cfg = new PlatformConnectionFactoryConfiguration(serverDef);
            PlatformConnectionFactoryManager mgr = new PlatformConnectionFactoryManager();
            connectionFactory = mgr.getFactory(cfg);
        } catch( Throwable t ) {
            tearDown();
            throw t;
        }
    }

    void internalSetUp() {
        Properties p = getProperties();
        repositoryName = p.getProperty("oma.repository.foundation.name", "Foundation");
        PlatformServicesUtil.StartCorePlatformServices(
            p.'iomsrv.metadatasrv.host',
            p.'iomsrv.metadatasrv.port',
            p.getProperty("iomsrv.metadatasrv.authdomain", "DefaultAuth"),
            p.containsKey("iomsrv.metadatasrv.trustedpeer"),
            p.'oma.person.trustusr.login.userid',
            p.'oma.person.trustusr.login.password' );
    }

    void promptForAuthenticationCLI( Properties p ) {
        String domain = p.getProperty("iomsrv.metadatasrv.authdomain", "DefaultAuth");
        String userName = p.getProperty("iomsrv.username");
        String password = p.getProperty("iomsrv.password");

        /* If there was a user name but no password then prompt
         * but only if we are not doing trusted user connections
         */
        if( userName != null && password == null && !p.containsKey("iomsrv.metadatasrv.trusteduser") ) {
            print "Password for $userName: ";
            password = new jline.ConsoleReader().readLine( "*".charAt(0) );
            p.put( "iomsrv.password", password);
        }

        /* If a trusted user connection is requested but no trusted userid then prompt
         */
        if( p.containsKey("iomsrv.metadatasrv.trusteduser") && !p.containsKey("oma.person.trustusr.login.userid") ) {
            print "Username (trusted user): ";
            String trustedUsername = new jline.ConsoleReader().readLine();
            p.put( "oma.person.trustusr.login.userid", trustedUsername );
        }

        /* If a trusted user connection is requested but no trusted password then prompt
         */
        if( p.containsKey("iomsrv.metadatasrv.trusteduser") && !p.containsKey("oma.person.trustusr.login.password") ) {
            print "Password for " + p.getProperty("oma.person.trustusr.login.userid") + "(trusted user): ";
            String trustedPassword = new jline.ConsoleReader().readLine( "*".charAt(0) );
            p.put( "oma.person.trustusr.login.password", trustedPassword );
        }

        /* If there is a trusted username but no username then prompt for the user
         * name.  Trusted User connections require a user name to look up in metadata
         * even though they don't require a password for that user.
         */
        if( p.containsKey("oma.person.trustusr.login.userid") && userName == null ) {
            print "User name to authenticate over trusted user connection: ";
            userName = new jline.ConsoleReader().readLine();
            p.put("iomsrv.username", userName)
        }
    }

    OMRLogicalServer promptForServerCLI( RepositoryInterface repository, Filter filter ) {
        OMRLogicalServer serverDef = null;
        List serverDefs = repository.search( filter );
        if( serverDefs.size() == 0 ) {
            return null;
        } else if( serverDefs.size() > 1 ) {
            def i = 0;
            println "Select a server:";
            serverDefs.each{
                println "    $i.  " + it.getName();
                ++i;
            }
            print "Server: ";
            i = Integer.parseInt( new jline.ConsoleReader().readLine().trim() );
            serverDef = serverDefs[i];
        } else {
            serverDef = serverDefs[0];
        }
        return serverDef;
    }

    UserContextInterface getUser() {
        return getUser( 0 );
    }

    synchronized UserContextInterface getUser( threadid ) {
        Properties p = getProperties();
        if( p.containsKey( "iomsrv.rduserperthread" ) ) {
            return getRdUser( threadid + 1 );
        } else {
            return getStartupUser();
        }
    }

    synchronized UserContextInterface getStartupUser() {
        UserContextInterface userContext = null;
        UserServiceInterface userService = CorePlatformServices.userService;
        Properties p = getProperties();
        String domain = p.getProperty("iomsrv.metadatasrv.authdomain", "DefaultAuth");
        String userName = p.getProperty("iomsrv.username");
        String password = p.getProperty("iomsrv.password");

        if( userName == null ) {
            userContext = userService.newUser( null, null );
        } else {
            userContext = userService.newUser( userName, password, domain );
        }

        /* Add this user context to the contexts to be freed */
        users << userContext;
        return userContext;
    }

    synchronized UserContextInterface getRdUser( int i ) {
        UserContextInterface userContext = null;
        UserServiceInterface userService = CorePlatformServices.userService;
        Properties p = getProperties();
        String domain = p.getProperty("iomsrv.metadatasrv.authdomain", "DefaultAuth");
        String format = p.getProperty("iomsrv.rduserformat", "rd\\rdtest%04d" );
        String userName = String.format(format, i);
        String password = p.getProperty("iomsrv.rduserpassword", "good2go" );
        userContext = userService.newUser( userName, password, domain );
        /* Add this user context to the contexts to be freed */
        users << userContext;
        return userContext;
    }

    synchronized void removeUser( userContext ) {
        if( users.contains( userContext ) ) {
            users.remove( userContext );
            CorePlatformServices.userService.removeUser( userContext );
            userContext.destroy();
            userContext = null;
        }
    }

    void tearDown() {
        users.each {
            CorePlatformServices.userService.removeUser(it);
            it.destroy();
            it = null;
        }
        users = [];
        if( connectionFactory ) {
            connectionFactory.getAdminInterface().destroy();
            connectionFactory = null;
        }
        PlatformServicesUtil.StopCorePlatformServices();
        super.tearDown();
    }
}