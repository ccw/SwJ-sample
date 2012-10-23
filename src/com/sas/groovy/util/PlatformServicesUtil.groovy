package com.sas.groovy.util;

import com.sas.groovy.util.*;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import com.sas.services.deployment.CorePlatformServices;
import com.sas.services.deployment.Environment;
import com.sas.services.deployment.MetadataSourceInterface;
import com.sas.services.deployment.PlatformServicesConfiguration;
import com.sas.services.deployment.URLMetadataSource;
import com.sas.services.security.login.LoginPropertyConstants;
import com.sas.services.security.login.OMILoginModule;
import com.sas.services.security.login.TrustedLoginModule;


class PlatformServicesUtil {

    static void StartCorePlatformServices( String AuthenticationHost,
                                           String AuthenticationPort,
                                           String Domain ) {
        StartCorePlatformServices( AuthenticationHost, AuthenticationPort, Domain, null, null, null );
    }

    static void StartCorePlatformServices( String AuthenticationHost,
                                           String AuthenticationPort,
                                           String Domain,
                                           Boolean TrustedPeer,
                                           String TrustedUserName,
                                           String TrustedPassword ) {
        String loginModuleName = OMILoginModule.class.getName();
        Map optionMap = new HashMap(7);
        optionMap.put(LoginPropertyConstants.PROPERTYNAME_HOST,   AuthenticationHost );
        optionMap.put(LoginPropertyConstants.PROPERTYNAME_PORT,   AuthenticationPort );
        optionMap.put(LoginPropertyConstants.PROPERTYNAME_DOMAIN, Domain );
        optionMap.put(LoginPropertyConstants.PROPERTYNAME_DEBUG,"FALSE");
        if( TrustedPeer ) {
            optionMap.put(LoginPropertyConstants.PROPERTYNAME_ID_PROPAGATION, LoginPropertyConstants.PROPERTYVALUE_ID_PROPAGATION_TRUSTED_PEER);
        } else {
            optionMap.put(LoginPropertyConstants.PROPERTYNAME_ID_PROPAGATION, LoginPropertyConstants.PROPERTYVALUE_ID_PROPAGATION_SSPI);
        }
        if( TrustedUserName ) {
            optionMap.put(LoginPropertyConstants.PROPERTYNAME_TRUSTEDUSER, TrustedUserName);
            optionMap.put(LoginPropertyConstants.PROPERTYNAME_TRUSTEDPW, TrustedPassword);
            loginModuleName = TrustedLoginModule.class.getName()
        }

        AppConfigurationEntry entry = new AppConfigurationEntry(loginModuleName,LoginModuleControlFlag.OPTIONAL,optionMap);
        AppConfigurationEntry[] entryArr = [entry] as AppConfigurationEntry[];

        Environment env = new Environment();
        env.setAppConfigurationEntry(entryArr);

        URL url = new PlatformServicesUtil().getClass().getResource("DefaultServicesDeployment.xml")
        MetadataSourceInterface metadataSource = new URLMetadataSource(url as URL, "Default Services", "Default Services Group");

        PlatformServicesConfiguration servicesConfig = new PlatformServicesConfiguration([metadataSource] as MetadataSourceInterface[],null);

        CorePlatformServices.setServicesConfiguration(servicesConfig);
        CorePlatformServices.setEnvironment(env);
        CorePlatformServices.startServices();

        metadataSource.destroy();
    }

    static void StopCorePlatformServices() {
        CorePlatformServices.terminateServices();
        /* We need to manually launch the garbage collector
         * to avoid a 60 second time out at the end of tests.
         */
        Runtime.getRuntime().gc();
    }
}