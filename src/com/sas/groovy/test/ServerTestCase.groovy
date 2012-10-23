package com.sas.groovy.test;

import com.sas.iom.orb.SASURI;
import com.sas.services.connection.ConnectionFactoryManager;
import com.sas.services.connection.ConnectionFactoryConfiguration;
import com.sas.services.connection.ManualConnectionFactoryConfiguration;
import com.sas.services.connection.ConnectionFactoryInterface;
import com.sas.services.connection.ConnectionInterface;
import com.sas.services.connection.Server;
import com.sas.services.connection.Credential;
import com.sas.services.connection.PasswordCredential;
import com.sas.services.connection.SecurityPackageCredential;
import com.sas.services.connection.TrustedPeerCredential;

/* The ServerTestCase class provides a harness for writing tests that connect
* directly to an IOM server.  It takes a set of properties and initializes
* a connection factory that will provide access to the top level object exposed
* by the server.  Tests that extend this class can make use of the "getTopLevelObject"
* method to access that top level object.  Tests that extend this class can also
* override the setUp method if they want a different connection factory or they
* can override the ThreadedTestCase:getProperties method if they want to explicitly
* specify the connection options. The options used by this clase include the following:
*
*     -Diomsrv.uri
*         The IOM URI that identifies the remote server as well as the optional
*         authentication information.  See
*         http://java.na.sas.com/reference/api/sas/bip/v920/com/sas/iom/orb/SASURI.html
*         For CLSIDs see
*         http://java.na.sas.com/reference/api/sas/FoundationServices/v920/com/sas/services/connection/Server.html
*         Note: iomsrv.uri overrides all the other properties supported by this class except
*         for iomsrv.authdomain.
*
*     -Diomsrv.authdomain
*         The authentication domain to use.
*
*     -Diomsrv.host
*         The host to contact.
*
*     -Diomsrv.port
*         The port the server is listening on.
*
*     -Diomsrv.protocol
*         The protocol to use when connecting to the server.
*
*     -Diomsrv.classfactory or
*     -Diomsrv.clsid
*         The value for the option CLASSFACTORY specifies the GUID of the object for
*         which to acquire an instance.
*
*     -Diomsrv.netencralg.name
*         The value for ENCR option specifies the encryption algorithm to request
*         when connecting.  This is a SAS 9.2 Configuration Framework property.
*
*     -Doma.propertytype.sasencryptionlevel.name
*         The value for ENCRLVL option specifies the level of encryption to request
*         when connecting.  This is a SAS 9.2 Configuration Framework property.
*
*     -Diomsrv.interfaceiid or
*     -Diomsrv.iid
*         The value for INTERFACEIID option specifies the GUID of the desired
*         interface within the object acquired.
*
*     -Diomsrv.locale
*         Added in SAS 9.2. The value for the LOCALE option specifies the locale of
*         the connecting peer.
*
*     -Diomsrv.major
*         Added in SAS 9.2. The value for the MAJOR option specifies the major portion
*         of the bridge protocol version to use.
*
*     -Diomsrv.minor
*         Added in SAS 9.2. The value for the MINOR option specifies the minor portion
*         of the bridge protocol version to use.
*
*     -Diomsrv.securitypackagelist
*         Added in SAS 9.2. The value for the SECURITYPACKAGELIST option is a comma
*         separated string containing the list of packages to be negotiated with the
*         server. This value should only be used when the SECURITYPACKAGE option has
*         a value of Negotiate.
*
*     -Diomsrv.password
*         The value for PASS option specifies the password for the identity to use when
*         connecting to the peer identified in this IOM URI.
*
*     -Diomsrv.securitypackage
*         Added in SAS 9.2. The value for the SECURITYPACKAGE option specifies the name
*         of the security package that this client is prepared to use with the server.
*         Possible values of this option are Negotiate, NTLM, or Kerberos.
*
*     -Diomsrv.servername
*         Added in SAS 9.2. The value for the SERVERNAME option specifies the name of
*         the server as it is known in the metadata.
*
*     -Diomsrv.spn
*         Added in SAS 9.2. The value for the SPN option specifies the Service Principal
*         Name that the client wishes to use with this IOM Server instance.
*
*     -Diomsrv.timeout
*         Added in SAS 9.2. The value for the TIMEOUT option specifies the timeout, in
*         milliseconds, of all outcall activity.
*
*     -Diomsrv.trustedsas
*         Has no value. Indicates that the owner of the current IOM Server is to be used
*         as the identity when connecting to the peer identified in this IOM URI.
*
*     -Diomsrv.username
*         The value for USER option specifies the identity to use when connecting to the
*         peer identified in this IOM URI.
*
*     -Diomsrv.rduserperthread
*         When present, a different rduser will be used for each thread making a connection
*         to the server.
*
*     -Diomsrv.rduserformat
*       The format used to generate the userids used by iomsrv.rduserperthread.  The default
*       is "rd\rdtest%04d".
*
*     -Diomsrv.rduserpassword
*       The password used for all the rdusers.  The default is the password for rd\rdtestXXXX.
*/

class ServerTestCase extends ThreadedTestCase {
    SASURI                      serverURI           = null;
    Server                      serverDef           = null;
    ConnectionFactoryInterface  connectionFactory   = null;

    void printHelp() {
        super.printHelp();
        println """
=============Server Test Case Properties=============

iomsrv.uri
The IOM URI that identifies the remote server as well as the optional authentication information.  See
http://java.na.sas.com/reference/api/sas/bip/v920/com/sas/iom/orb/SASURI.html
For CLSIDs see
http://java.na.sas.com/reference/api/sas/FoundationServices/v920/com/sas/services/connection/Server.html
Note: iomsrv.uri overrides all the other properties supported by this class except for iomsrv.authdomain.

iomsrv.authdomain
The authentication domain to use.

iomsrv.host
The host to contact.

iomsrv.port
The port the server is listening on.

iomsrv.protocol
The protocol to use when connecting to the server.

iomsrv.classfactory or iomsrv.clsid
The value for the option CLASSFACTORY specifies the GUID of the object for which to acquire an instance.

iomsrv.netencralg.name
The value for ENCR option specifies the encryption algorithm to request when connecting.  This is a SAS 9.2 Configuration Framework property.

oma.propertytype.sasencryptionlevel.name
The value for ENCRLVL option specifies the level of encryption to request when connecting.  This is a SAS 9.2 Configuration Framework property.

iomsrv.interfaceiid or iomsrv.iid
The value for INTERFACEIID option specifies the GUID of the desired interface within the object acquired.

iomsrv.locale
Added in SAS 9.2. The value for the LOCALE option specifies the locale of the connecting peer.

iomsrv.major
Added in SAS 9.2. The value for the MAJOR option specifies the major portion of the bridge protocol version to use.

iomsrv.minor
Added in SAS 9.2. The value for the MINOR option specifies the minor portion of the bridge protocol version to use.

iomsrv.securitypackagelist
Added in SAS 9.2. The value for the SECURITYPACKAGELIST option is a comma separated string containing the list of packages to be negotiated with the server. This value should only be used when the SECURITYPACKAGE option has a value of Negotiate.

iomsrv.password
The value for PASS option specifies the password for the identity to use when connecting to the peer identified in this IOM URI.

iomsrv.securitypackage
Added in SAS 9.2. The value for the SECURITYPACKAGE option specifies the name of the security package that this client is prepared to use with the server. Possible values of this option are Negotiate, NTLM, or Kerberos.

iomsrv.servername
Added in SAS 9.2. The value for the SERVERNAME option specifies the name of the server as it is known in the metadata.

iomsrv.spn
Added in SAS 9.2. The value for the SPN option specifies the Service Principal Name that the client wishes to use with this IOM Server instance.

iomsrv.timeout
Added in SAS 9.2. The value for the TIMEOUT option specifies the timeout, in milliseconds, of all outcall activity.

iomsrv.trustedsas
Has no value. Indicates that the owner of the current IOM Server is to be used as the identity when connecting to the peer identified in this IOM URI.

iomsrv.username
The value for USER option specifies the identity to use when connecting to the peer identified in this IOM URI.

iomsrv.rduserperthread
When present, a different rduser will be used for each thread making a connection to the server.  iomsrv.username and iomsrv.password will be ignored.

iomsrv.rduserformat
The format used to generate the userids used by iomsrv.rduserperthread.  The default is \"rd\\rdtest%04d\".

iomsrv.rduserpassword
The password used for all the rdusers.  The default is the password for rd\rdtestXXXX.""";
    }

    static SASURI createURI( Properties p ) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("iom://${p.'iomsrv.host'}:${p.'iomsrv.port'};${p.'iomsrv.protocol'};");
        Boolean needComma = false;
        if( p.containsKey( "iomsrv.classfactory" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            String value = p.'iomsrv.classfactory';
            if( value.startsWith( "CLSID" ) ) {
                value = Server.clsidNameToValue( value );
            }
            buffer.append( "CLASSFACTORY=${value}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.clsid" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            String value = p.'iomsrv.clsid';
            if( value.startsWith( "CLSID" ) ) {
                value = Server.clsidNameToValue( value );
            }
            buffer.append( "CLSID=${value}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.netencralg.name" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "ENCR=${p.'iomsrv.netencralg.name'}" );
            needComma = true;
        }
        if( p.containsKey( "oma.propertytype.sasencryptionlevel.name" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "ENCRLVL=${p.'oma.propertytype.sasencryptionlevel.name'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.interfaceiid" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "INTERFACEIID=${p.'iomsrv.interfaceiid'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.iid" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "IID=${p.'iomsrv.iid'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.locale" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "LOCALE=${p.'iomsrv.locale'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.major" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "MAJOR=${p.'iomsrv.major'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.minor" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "MINOR=${p.'iomsrv.minor'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.securitypackagelist" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "SECURITYPACKAGELIST=${p.'iomsrv.securitypackagelist'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.password" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "PASS=${p.'iomsrv.password'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.securitypackage" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "SECURITYPACKAGE=${p.'iomsrv.securitypackage'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.servername" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "SERVERNAME=${p.'iomsrv.servername'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.spn" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "SPN=${p.'iomsrv.spn'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.timeout" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "TIMEOUT=${p.'iomsrv.timeout'}" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.trustedsas" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "TRUSTEDSAS" );
            needComma = true;
        }
        if( p.containsKey( "iomsrv.username" ) ) {
            if( needComma ) {
                buffer.append( "," );
            }
            buffer.append( "USER=${p.'iomsrv.username'}" );
            needComma = true;
        }
        return SASURI.create( buffer.toString() );
    }

    void setUp() {
        /* Parse the URI and create a Server Def from it.  Note that we need to update
         * a couple of fields in the server def that aren't handled by the fromURI method.
         * For URI formats see
         * http://java.na.sas.com/reference/api/sas/bip/v920/com/sas/iom/orb/SASURI.html
         */
        serverURI = null;
        Properties p = getProperties();
        if( p.containsKey( "iomsrv.uri" ) ) {
            serverURI = SASURI.create( p.'iomsrv.uri' );
        } else {
            serverURI = createURI( p );
        }

        serverDef = Server.fromURI(serverURI);

        /* Update the CLSID so the user can use the symbolic names.
         */
        if( serverURI.clsid && serverURI.clsid.toUpperCase().startsWith("CLSID") ) {
            serverDef.classID = Server.clsidNameToValue(serverURI.clsid.toUpperCase());
        }

        /* The domain doesn't have a spot in the URI so it gets a seperate property.
         */
        String domain = p.getProperty("iomsrv.authdomain","DefaultAuth");
        serverDef.setDomain(domain);

        /* Just assume that we can negotiate the security package if the user didn't
         * specify.
         */
        if( serverDef.securityPackage == null ) {
            serverDef.setSecurityPackage("NEGOTIATE");
        }

        /* Create a connection factory for this server.
         */
        ConnectionFactoryManager cxfManager = new ConnectionFactoryManager();
        ConnectionFactoryConfiguration cxfConfig = new ManualConnectionFactoryConfiguration(serverDef);
        connectionFactory = cxfManager.getFactory(cxfConfig);

        super.setUp();
    }

    Credential getUserCredential() {
        return getUserCredential( 0 );
    }

    synchronized Credential getUserCredential( threadid ) {
        /* When rduserperthread is specified on the command line then return an rduser
         * for the given threadid.  Otherwise return the right kind of credential for
         * the other options in the server URI.
         */
        Credential userCredential = null;
        Properties p = getProperties();
        if( p.containsKey( "iomsrv.rduserperthread" ) ) {
            String format = p.getProperty("iomsrv.rduserformat", "rd\\rdtest%04d" );
            String username = String.format(format, threadid);
            String password = p.getProperty("iomsrv.rduserpassword", "good2go" );
            userCredential = new PasswordCredential(username,password,serverDef.getDomain());
        } else {
            if( serverURI.isTrustedSAS() ) {
                userCredential = TrustedPeerCredential.getInstance();
            } else if( serverURI.getUser() ) {
                userCredential = new PasswordCredential(serverURI.getUser(),serverURI.getPass(),serverDef.getDomain());
            } else {
                userCredential = SecurityPackageCredential.getInstance();
            }
        }
        if( userCredential && serverURI.getLocale() ) {
            Locale locale = SASURI.parseLocaleName(serverURI.getLocale());
            userCredential.setLocale(locale);
        }
        return userCredential;
    }

    org.omg.CORBA.Object getTopLevelObject() {
        return getTopLevelObject( -1 );
    }

    synchronized org.omg.CORBA.Object getTopLevelObject( threadid ) {
        if( !connectionFactory ) {
            return null;
        }
        ConnectionInterface connection = null;
        Credential userCredential = getUserCredential( threadid );
        if( userCredential ) {
            connection = connectionFactory.getConnection( userCredential );
        } else {
            Properties p = getProperties();
            connection = connectionFactory.getConnection( p.getProperty("iomsrv.authdomain","DefaultAuth") );
        }
        return connection.getObject();
    }

    void tearDown() {
        serverURI           = null;
        serverDef           = null;
        if( connectionFactory ) {
            connectionFactory.getAdminInterface().destroy();
            connectionFactory = null;
        }
        super.tearDown();
    }
}