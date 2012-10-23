package com.sas.groovy.util;

import com.sas.services.connection.*;
import org.omg.CORBA.Any
import org.omg.CORBA.TCKind
import org.omg.CORBA.ORB;
import com.sas.iom.orb.SASURI;
import com.sas.iom.SASIOMDefs.UUID;
import com.sas.iom.SASIOMDefs.UUIDHelper;
import com.sas.iom.SASIOMDefs.DateTimeHelper;
import com.sas.iom.SASIOMCommon.IServerInformation;
import com.sas.iom.SASIOMCommon.IServerInformationHelper;


/* The IOMServer class takes a set of properties and attempts to connect
 * to the server defined by the following (case sensitive):
 *     classID [see http://java.na.sas.com/reference/api/sas/FoundationServices/v920/com/sas/services/connection/Server.html]
 *     uri
 *     host
 *     port
 *     domain
 *     encryptionPolicy
 *     encryptionContent
 *     encryptionAlgorithms
 *     authService
 *     securityPackage
 *     securityPackageList
 *     SPN
 *     trustedPeerCred
 *     securityPackageCred
 *     userName
 *     password
 *     locale
 * After a connection is established the open connection is held in the
 * topLevelObject field.  The server definition used to create that
 * connection is held in the serverDef field.  And the credential
 * used to authenticate that connection is held in the credential field.
 */
public class IOMServer {
    org.omg.CORBA.Object topLevelObject = null;
    BridgeServer serverDef = null;
    Credential credential = null;
    
    static {
        IServerInformation.metaClass.getCategories = { ->
            return delegate.ListCategories().collect{ name ->
                new IOMCategory(filteredList:delegate.UseCategory(name, ""), name:name) };
        }
        IServerInformation.metaClass.getCategory = { name ->
            return new IOMCategory(filteredList:delegate.UseCategory(name, ""), name:name);
        }
        IServerInformation.metaClass.propertyMissing = { name ->
            return delegate.getCategory( name );
        }
    }
    /*
     * Construct an IOMServer object and connect to a server based
     * on the given properties.
     */
    public IOMServer( Properties p ) {
        this( p, false );
    }
    
    public IOMServer( Properties p, Boolean withAttributeHelpers ) {
        // Get a configuration that identifies the desired server
        serverDef = createServerDef( p );
        
        // Get some factory management classes
        ConnectionFactoryManager cxfManager = new ConnectionFactoryManager();
        ConnectionFactoryConfiguration cxfConfig = new ManualConnectionFactoryConfiguration(serverDef);

        // get a connection factory that matches the configuration
        ConnectionFactoryInterface cxf = cxfManager.getFactory(cxfConfig);

        // get a connection
        credential = createCredential( p );
        ConnectionInterface connection = null;
        if( credential ) {
            connection = cxf.getConnection( credential );
        } else {
            connection = cxf.getConnection( p.getProperty("domain", "DefaultAuth") );
        }
        topLevelObject = connection.getObject();

        // Check to make sure that if this object supports IServerInformation
        // that the client script set ExpandoMetaClass.enableGlobally()
        if( withAttributeHelpers ) {
            IServerInformation iInfo = IServerInformationHelper.narrow(topLevelObject);
            if( iInfo ) {
                if(!iInfo.metaClass.respondsTo(iInfo, "getCategories")) {
                    throw new Exception("You must call ExpandoMetaClass.enableGlobally() before your app starts to use the attribute helper methods.  Such as in the main method or servlet bootstrap.");
                }
            }
        }        
    }

    /*
     * The createServerDef method returns a new server definition based
     * on the given properties.  Currently supported properties include
     * (case sensitive):
     *     classID [see http://java.na.sas.com/reference/api/sas/FoundationServices/v920/com/sas/services/connection/Server.html]
     *     host
     *     port
     *     domain
     *     encryptionPolicy
     *     encryptionContent
     *     encryptionAlgorithms
     *     authService
     *     securityPackage
     *     securityPackageList
     *     SPN
     *     trustedPeerCred
     *     securityPackageCred
     */
    public static BridgeServer createServerDef( Properties p ) {
        BridgeServer server = null;
        
        String classID = "";
        if( p.containsKey("classID") ) {
            classID = p.getProperty("classID");
        } else if( p.containsKey("CLSID") ) {
            classID = p.getProperty("CLSID");
        } else if( p.containsKey("clsid") ) {
            classID = p.getProperty("clsid");
        }
        if (classID.startsWith("CLSID"))
        {
            classID = Server.clsidNameToValue(classID);
        }

        if( p.containsKey("uri" ) ) {
            SASURI uri = SASURI.create(p.getProperty("uri"));
            server = Server.fromURI(uri);
            if( classID != "" ) {
                server.classID = classID;
            }
        } else {
            if( classID == "" ) {
                classID = Server.clsidNameToValue( "CLSID_SAS" );
            }
            String host = p.getProperty("host","localhost");
            int port = Integer.parseInt(p.getProperty("port","5310"));
            server = new BridgeServer(classID,host,port);
        }
        
        String domain = p.getProperty("domain","DefaultAuth");
        server.setDomain(domain);

        if( p.containsKey("encryptionPolicy") ) {
            server.setEncryptionPolicy(p.getProperty("encryptionPolicy"));
        }

        if( p.containsKey("encryptionContent") ) {
            server.setEncryptionContent(p.getProperty("encryptionContent"));
        }

        if( p.containsKey("encryptionAlgorithms") ) {
            server.setEncryptionAlgorithms(p.getProperty("encryptionAlgorithms"));
        }

        if( p.containsKey("authService") ) {
            server.setAuthService(p.getProperty("authService"));
        }

        if( p.containsKey("securityPackage") ) {
            server.setSecurityPackage(p.getProperty("securityPackage"));
        }

        if( p.containsKey("securityPackageList") ) {
            server.setSecurityPackageList(p.getProperty("securityPackageList"));
        }

        if( p.containsKey("SPN") ) {
            server.setSPN(p.getProperty("SPN"));
        }
        return server;
    }

    /*
     * The createCredential method returns a new credential based on the
     * given properties.  Currently supported properties include
     * (case sensitive):
     *     userName
     *     password
     *     locale
     *     domain
     */
    
    public static Credential createCredential( Properties p ) {
        Credential cred = null;
        if (p.containsKey("trustedPeerCred")) {
            cred = TrustedPeerCredential.getInstance();
        }
        else if (p.containsKey("securityPackageCred")) {
            cred = SecurityPackageCredential.getInstance();
        }
        else if( p.containsKey("userName") ) {
            String userName = p.getProperty("userName","");
            String password = p.getProperty("password","");
            String domain = p.getProperty("domain","DefaultAuth");
            cred = new PasswordCredential(userName,password,domain);
        }
        if( cred ) {
            String localeS = p.getProperty("locale");
            if( p.containsKey("locale") ){
                Locale locale = SASURI.parseLocaleName(p.getProperty("locale"));
                cred.setLocale(locale);
            }
        }
        return cred;
    }
    
    /*
     * The AnyToString method takes an Any and retuns a String
     * representation of that Any.
     */
    static String AnyToString( Any a ) {
        switch( a.type().kind().value() ) {
            case TCKind._tk_long:
                return a.extract_long().toString();
                break;
            case TCKind._tk_longlong:
                return a.extract_longlong().toString();
                break;
            case TCKind._tk_double:
                return a.extract_double().toString();
                break;
            case TCKind._tk_boolean:
                return a.extract_boolean().toString();
                break;
            case TCKind._tk_string:
                return a.extract_string().toString();
                break;
            case UUIDHelper.type().kind().value():
                UUID u = UUIDHelper.extract( a );
                return String.format( "%08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X", u.Data1, u.Data2, u.Data3, 
                    u.Data4[0], u.Data4[1], u.Data4[2], u.Data4[3], u.Data4[4], u.Data4[5], u.Data4[6], u.Data4[7] );
                break;
            case DateTimeHelper.type().kind().value():
                return DateTimeHelper.extract( a ).toString();
                break;
            default:
                return "unknown type: " + a.type() + "(" + a.type().kind().value() + ")";
                break;
        }
    }

    /*
     * The StringToAny method takes a type (as returned from IFilteredList)
     * and a string value and returns an Any of the appropriate type with
     * the given value.
     */
    static Any StringToAny( ORB orb, String type, String value ) {
        Any any = orb.create_any();
        switch( type ) {
            case "Int32":
                any.insert_long( Integer.parseInt( value ) );
                break;
            case "Double":
                any.insert_double( Double.parseDouble( value ) );
                break;
            case "Boolean":
                any.insert_boolean( Boolean.parseBoolean( value ) );
                break;
            case "UUID":
                break;
            case "DateTime":
                break;
            case "String":
            case "Level":
                any.insert_string( value );
                break;
        }
        return any;
    }

}