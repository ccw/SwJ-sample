import com.sas.groovy.test.PlatformTestCase;

import com.sas.services.user.UserServiceInterface;
import com.sas.services.user.UserContextInterface;

import com.sas.services.information.RepositoryInterface;
import com.sas.services.information.FilterComponent;
import com.sas.services.information.metadata.OMRLogicalServer
import com.sas.services.information.metadata.LogicalServerFilter;
import com.sas.services.information.Filter;

import com.sas.services.session.SessionServiceInterface;
import com.sas.services.session.SessionContextInterface;

import com.sas.services.connection.platform.PlatformConnectionFactoryConfiguration;
import com.sas.services.connection.platform.PlatformConnectionFactoryManager;
import com.sas.services.connection.platform.PlatformConnectionFactoryInterface;
import com.sas.services.connection.ConnectionInterface;
import com.sas.services.connection.Server;

import org.apache.log4j.Logger;

/*
 * The PlatformListServers class extends the PlatformTestCase to
 * print out a list of all the servers defined in the metadata server.
 * In addition to the PlatformTestCase, ThreadedTestCase, and TestCase options,
 * this class expects the following:
 *
 *     -Dspawnersonly      : Show the spawner definitions and the servers they spawn.
 *                           Default false.
 *     -Dserversonly       : Show the logical server definitions and the servers they
 *                           contain.  Default false.
 *     -Dverbose           : If true (present on the command line) then print additional
 *                           information about the servers found.
 *
 * Example command lines
 *
 * Regular user authentication (prompted for password and server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 platformListServers.groovy
 *
 * SSPI authentication
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 platformListServers.groovy
 *
 * Trusted Peer authentication
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedpeer platformListServers.groovy
 *
 * Trusted User authentication (prompted for trusted password but not user password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 -Diomsrv.trusteduser -Doma.person.trustusr.login.userid=sastrust@saspw platformListServers.groovy
 *
 */

class PlatformListServers extends PlatformTestCase {
    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformListServers");
    Boolean verbose      = false;
    Boolean spawnersonly = false;
    Boolean serversonly  = false;

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

spawnersonly
Show the spawner definitions and the servers they spawn.  The default is false.

serversonly
Show the logical server definitions and the servers they contain.  The default is false.

verbose
If true (present on the command line) then print additional information about the servers found.""";
    }

    void setUp() {
        setUpWithAuthenticationCLI();
        Properties p    = getProperties();
        verbose         = p.containsKey("verbose");
        spawnersonly    = p.containsKey("spawnersonly");
        serversonly     = p.containsKey("serversonly");

        /* spawnersonly and serversonly are mutually exclusive */
        if( spawnersonly && serversonly ) {
            throw new IllegalArgumentException( "The spawnersonly and serversonly options are mutually exclusive" );
        }
    }

    void testListServers() {
        onExecutions { iteration, thread ->
            RepositoryInterface repository = null;
            UserContextInterface userContext = getUser( thread )

            repository = userContext.authServer.getRepository( repositoryName );
            logger.info( "Connected to " + repository );

            /* Find the server def requested by the user.
             */
            Filter spwnFilter = null;
            LogicalServerFilter lsFilter = null;

            if( !serversonly ) {
                spwnFilter = new Filter( "PublicType", FilterComponent.EQUALS, "Spawner.IOM" );
            }

            if( !spawnersonly ) {
                lsFilter = new LogicalServerFilter();
            }

            if( spwnFilter ) {
                List serverDefs = repository.search( spwnFilter );
                serverDefs.each{
                    logger.info( "" );
                    logger.info( "Spawner" );
                    logger.info( "\tName: " + it.getName() );
                    it.getSoftwareTrees().each{
                        if( it.name == "MachineGroup" ) {
                            it.getItemsByType( "Machine" ).each {
                                logger.info( "\tMachine: " + it.getName() );
                            }
                        }
                    }
                    it.getSourceConnections().each{
                        logger.info( "\tConnection: " + it.getName() + " - " + it.getPort() );
                    }
                    it.getServed().each {
                        //logger.info( "\tServer: " + it.getName() + " (" + it.getHost() + ":" + it.getTcpPort() + ")" );
                        logger.info( "\tServer" );
                        logger.info( "\t\tName: " + it.getName() );// + " (" + it.getHost() + ":" + it.getTcpPort() + ")";
                        logger.info( "\t\tURI: iom://" + it.getHost() + ":" + it.getTcpPort() + ";" +
                                it.getApplicationProtocol() + ";CLSID=" + it.getClassIdentifier() );
                        if( verbose ) {
                            it.getProperties().each{ logger.info( "\t\t" + it.getName() + ": " + it.getValue() );}
                        }
                    }
                }
            }

            if( lsFilter ) {
                List serverDefs = repository.search( lsFilter );
                serverDefs.each{
                    String type = it.getType();
                    logger.info( "" );
                    if( type != "" ) {
                        logger.info( "$type" );
                    } else {
                        logger.info( "LogicalServer" );
                    }
                    logger.info( "\tName: " + it.getName() );
                    it.getServers().each {
                        logger.info( "\tServer" );
                        logger.info( "\t\tName: " + it.getName() );// + " (" + it.getHost() + ":" + it.getTcpPort() + ")";
                        logger.info( "\t\tURI: iom://" + it.getHost() + ":" + it.getTcpPort() + ";" +
                                it.getApplicationProtocol() + ";CLSID=" + it.getClassIdentifier() );
                        if( verbose ) {
                            it.getProperties().each{ logger.info( "\t\t" + it.getName() + ": " + it.getValue() ); }
                        }
                    }
                }
            }

            logger.info( "" );
            removeUser( userContext );
            if( repository ) {
                repository.close();
            }
        }
    }
}
