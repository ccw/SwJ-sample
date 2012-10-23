import com.sas.groovy.test.PlatformTestCase;

import org.apache.log4j.Logger;

import com.sas.services.user.UserContextInterface;

import com.sas.services.session.SessionServiceInterface;

/* The PlatformConnection class tests creating a new userContext in a
 * given platform deployment.  This test expects no options other than
 * those supported by the PlatformTestCase, ThreadedTestCase and TestCase
 * classes.
 *
 * Regular user authentication (prompted for password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 platformConnection.groovy
 *
 * SSPI authentication
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 platformConnection.groovy
 *
 * Trusted Peer authentication
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedpeer platformConnection.groovy
 *
 * Trusted User authentication (prompted for trusted password and user name but not user password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trusteduser -Doma.person.trustusr.login.userid=sastrust@saspw platformConnection.groovy
 *
 */

class PlatformConnection extends PlatformTestCase {
    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformConnection");

    void setUp() {
        setUpWithAuthenticationCLI();
    }

    void testConnection() {
        onExecutions { iteration, thread ->
            UserContextInterface userContext = getUser( thread );
            logger.info( "Connected to " + userContext.authServer.getRepository( repositoryName ) );
            removeUser(userContext);
        }
    }
}
