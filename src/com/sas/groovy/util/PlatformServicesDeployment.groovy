package com.sas.groovy.util;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import com.sas.services.security.login.OMILoginModule;
import com.sas.services.security.login.TrustedLoginModule;

import com.sas.services.deployment.Environment;
import com.sas.services.deployment.MetadataSourceInterface;
import com.sas.services.deployment.URLMetadataSource;
import com.sas.services.deployment.ServiceLoader;

import com.sas.services.discovery.DiscoveryService;
import com.sas.services.discovery.DiscoveryServiceInterface;
import com.sas.services.discovery.ServiceTemplate;

import com.sas.services.user.UserServiceInterface;

import com.sas.services.session.SessionServiceInterface;

import com.sas.services.information.InformationServiceInterface;


/* The PlatformServicesDeployment class takes a set of properties and attempts to
 * initialize a platform services deployment.  This includes setting up the JAAS
 * authentication as well as finding and deploying all the services.  The caller
 * can provide a MetadataSourceInterface that describes a deployment and a
 * DiscoveryServiceInterface if they want to control those aspects.  If those
 * arguments are null then a default deployment will be used and the local
 * discovery service.  If the user has specified the system property
 * java.security.auth.login.config then no additional authentication configuration
 * will be attempted.  If, however, that property was not set, then a default
 * Metadata Server authentication will be configured with SSPI and (possibly)
 * trusted peer connections.  The options that can be passed in the Properties
 * argument include the following:
 *     authhost        : The Metadata Server host name to use when configuring
 *                           an OMILoginModule (user/pass authentication or IWA).
 *     authport        : The Metadata Server port to use when configuring
 *                           an OMILoginModule (user/pass authentication or IWA).
 *     trustedPeer     : If true then a trusted peer connection is attempted.
 *                           Otherwise an SSPI connection is attempted.
 *     trustedUserName : If present, causes a TrustedLoginModule to be used and
 *                           enables trusted user authentication.
 *     trustedPassword : Required for trused user authentication.
 *     domain          : The domain used for authentication (trusted and regular)
 *                           Default = DefaultAuth
 *     repository      : The repository used for authentication (trusted and regular)
 *                           Default = Foundation
 *
 * NOTE: If both trustedPeer and trustedUserName are specified then a trusted user
 *       connection will be used.  Trusted peer will NOT be used.
 *
 * NOTE: When making a trusted user connection a regular user name that exists in
 *       the Metadata Server must still be specified but neither SSPI nor trustedPeer
 *       connections require a user name.
 */

class PlatformServicesDeployment {

    String                      repositoryName = "";
    DiscoveryServiceInterface   discoveryService = null;
    InformationServiceInterface informationService = null;
    SessionServiceInterface     sessionService = null;
    UserServiceInterface        userService = null;

    PlatformServicesDeployment( Properties p ) {
        this( p, null, null );
    }
    PlatformServicesDeployment( Properties p, MetadataSourceInterface customDeploymentDescription, DiscoveryServiceInterface customDiscoveryService ) {
        this( p.getProperty("authhost"),
              p.getProperty("authport"),
              p.containsKey("trustedPeer"),
              p.getProperty("trustedUserName"),
              p.getProperty("trustedPassword"),
              p.getProperty("domain", "DefaultAuth"),
              p.getProperty("repository", "Foundation"),
              customDeploymentDescription,
              customDiscoveryService );
    }

    PlatformServicesDeployment( String AuthenticationHost,
                                String AuthenticationPort,
                                Boolean TrustedPeer,
                                String TrustedUserName,
                                String TrustedPassword,
                                String Domain,
                                String Repository,
                                MetadataSourceInterface customDeploymentDescription,
                                DiscoveryServiceInterface customDiscoveryService ) {

        /* Save the name of the repository we are connecting to.
         */
        repositoryName = Repository;

        /* If the user did not specify a deployment description then
         * create a default deployment from the XML description in this package.
         */
        Boolean destroyMetadataSource = false;
        MetadataSourceInterface metadataSource = customDeploymentDescription;
        if( metadataSource == null ) {
            destroyMetadataSource = true;
            metadataSource = new URLMetadataSource(this.getClass().getResource("DefaultServicesDeployment.xml"), "Default Services");
        }

        /* If the user did not specify a discovery service then
         * use the default discovery service.
         */
        discoveryService = customDiscoveryService;
        if( discoveryService == null ) {
            discoveryService = DiscoveryService.defaultInstance();
        }

        /* If the system property java.security.auth.login.config is set then
         * assume the authentication environment is already set up.  Otherwise
         * setup a basic environment for the user.
         */
        if( System.getProperties().containsKey("java.security.auth.login.config") ) {
            ServiceLoader.deployServices(metadataSource, discoveryService);
        } else {
            /* Create an environment that defines the JAAS plugin options to use for
             * authentication.  In this case we are setting up the options to use
             * our metadata server as the athentication "service" via OMILoginModule
             * and adding support for sspi.  We are also adding a trusted user
             * authentication service.
             */
            Map optionsMap = new HashMap(4);
            optionsMap.put("host", AuthenticationHost);
            optionsMap.put("port", AuthenticationPort);
            optionsMap.put("domain", Domain);
            optionsMap.put("repository", Repository);
            if( TrustedPeer ) {
                optionsMap.put("idpropagation", "trustedpeer");
            } else {
                optionsMap.put("idpropagation", "sspi");
            }
            AppConfigurationEntry loginJAASConfig = new AppConfigurationEntry( OMILoginModule.class.getName(), LoginModuleControlFlag.OPTIONAL, optionsMap );

            optionsMap = new HashMap(4);
            optionsMap.put("host", AuthenticationHost);
            optionsMap.put("port", AuthenticationPort);
            optionsMap.put("domain", Domain);
            optionsMap.put("repository", Repository);
            optionsMap.put("trusteduser", TrustedUserName);
            optionsMap.put("trustedpw", TrustedPassword);
            AppConfigurationEntry trustedJAASConfig = new AppConfigurationEntry( TrustedLoginModule.class.getName(), LoginModuleControlFlag.OPTIONAL, optionsMap );
            /* Only add the trusted authentication if there is a TrustedUserName.
             * It changes the log output from the metadata server so I'm not sure
             * if it's overriding the sspi connection I'm trying.
             */
            AppConfigurationEntry[] JAASConfigs = [];
            if( TrustedUserName != null ) {
                JAASConfigs = [trustedJAASConfig];
            } else {
                JAASConfigs = [loginJAASConfig];
            }
            Environment environment = new Environment();
            environment.setAppConfigurationEntry( JAASConfigs );

            /* Deploy the services
             */
            ServiceLoader.deployServices(metadataSource, environment, discoveryService);
        }

        /* If we created a metadata soure then destroy it now.
         */
        if( destroyMetadataSource ) {
            metadataSource.destroy();
        }
    }

    synchronized InformationServiceInterface getInformationService() {
        if( informationService == null ) {
            informationService = (InformationServiceInterface) discoveryService.findService(new ServiceTemplate((Class[])[InformationServiceInterface.class] ));
        }
        return informationService;
    }

    synchronized UserServiceInterface getUserService() {
        if( userService == null ) {
            userService = (UserServiceInterface) discoveryService.findService(new ServiceTemplate((Class[])[UserServiceInterface.class] ));
        }
        return userService;
    }

    synchronized SessionServiceInterface getSessionService() {
        if( sessionService == null ) {
            sessionService = (SessionServiceInterface) discoveryService.findService(new ServiceTemplate((Class[])[SessionServiceInterface.class]));
        }
        return sessionService;
    }

    synchronized void destroy() {
        if( discoveryService ) {
            discoveryService.destroy();
            discoveryService = null;
        }
    }

}