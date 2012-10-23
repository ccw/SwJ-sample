package com.sas.groovy.util;

import com.sas.metadata.remote.MdFactory;
import com.sas.metadata.remote.MdFactoryImpl;
import com.sas.metadata.remote.MdOMRConnection;
import com.sas.metadata.remote.MdObjectStore;
import com.sas.metadata.remote.MdOMIUtil;
import com.sas.metadata.remote.CMetadata;
import com.sas.metadata.remote.AssociationList;

class JomaBuilder {
    MdFactoryImpl factory;
    MdOMRConnection connection;
    MdObjectStore store;
    MdOMIUtil util;
    CMetadata repository;
    String repositoryId;

    Node storedProcessServerTemplate = null;
    Node workspaceServerTemplate = null;
    Node pooledWorkspaceServerTemplate = null;
    Node sasAppServerTemplate = null;
    Node objectSpawnerTemplate = null;

    Stack template = new Stack();
    def userTypeNames = [:];

    Properties nameResolver = null;

    JomaBuilder( String host, String port ) {
        loadProperties();
        initializeJoma( host, port, null );
    }

    JomaBuilder( String host, String port, String SPN ) {
        loadProperties();
        initalizeJoma( host, port, SPN );
    }

    JomaBuilder( String host, String port, String userName, String password ) {
        loadProperties();
        initializeJoma( host, port, userName, password );
    }

    void loadProperties() {
        ClassLoader loader = ClassLoader.getSystemClassLoader()
        URL url = loader.getSystemResource( "com/sas/workspace/visuals/res/PropertyBundle.properties" );
        nameResolver = new Properties();
        nameResolver.load( url.openStream() );
    }

    void initializeJoma( String host, String port, String SPN ){
        factory = new MdFactoryImpl( false );
        factory.setLoggingEnabled( true );

        connection = factory.getConnection();
        connection.makeOMRConnectionUsingSecurityPackage( host, port, null, SPN );

        store = factory.createObjectStore();

        util = factory.getOMIUtil();

        repository = util.getRepositories()[0];

        repositoryId = repository.getFQID();
        repositoryId = repositoryId.substring(repositoryId.indexOf(".") + 1);
    }

    void initializeJoma( String host, String port, String userName, String password ){
        factory = new MdFactoryImpl( false );

        connection = factory.getConnection();
        connection.makeOMRConnection( host, port, userName, password );

        store = factory.createObjectStore();

        util = factory.getOMIUtil();

        repository = util.getRepositories()[0];

        repositoryId = repository.getFQID();
        repositoryId = repositoryId.substring(repositoryId.indexOf(".") + 1);
    }

    void dispose(){
        store.updatedMetadataAll();
        factory.dispose();
    }

    JomaBuilder commit() {
        store.updatedMetadataAll();
        return this;
    }

    Object createInternalUserAndCommit( Map attributes ) {
        String name = attributes["Name"];
        JomaBuilderObject user = createJomaBuilderObject( "Person", [Name:name] );
        if( !user ) {
            return null;
        }
        /* Must commit this user to the metadata server so that
         * it can be found by SetInternalPassword */
        commit();
        def sec = connection.MakeISecurityConnection();
        sec.SetInternalPassword( name, attributes["Password"] );

        boolean disabled                   = false;
        boolean bypassStrength             = true;
        boolean bypassHistory              = true;
        boolean stdPasswordExpirationDays  = false;
        int passwordExpirationDays         = 0;
        boolean bypassLockout              = true;
        boolean bypassInactivitySuspension = true;
        boolean expireAccount              = false;
        long expirationDate                = 0;


        if( attributes.containsKey( "Disabled" ) ) {
            disabled = attributes["Disabled"];
        }
        if( attributes.containsKey( "BypassStrength" ) ) {
            bypassStrength = attributes["BypassStrength"];
        }
        if( attributes.containsKey( "BypassHistory" ) ) {
            bypassHistory = attributes["BypassHistory"];
        }
        if( attributes.containsKey( "StdPasswordExpirationDays" ) ) {
            stdPasswordExpirationDays = attributes["StdPasswordExpirationDays"];
        }
        if( attributes.containsKey( "PasswordExpirationDays" ) ) {
            passwordExpirationDays = attributes["PasswordExpirationDays"];
        }
        if( attributes.containsKey( "BypassLockout" ) ) {
            bypassLockout = attributes["BypassLockout"];
        }
        if( attributes.containsKey( "BypassInactivitySuspension" ) ) {
            bypassInactivitySuspension = attributes["BypassInactivitySuspension"];
        }
        if( attributes.containsKey( "ExpireAccount" ) ) {
            expireAccount = attributes["ExpireAccount"];
        }
        if( attributes.containsKey( "ExpirationDate" ) ) {
            expirationDate = attributes["ExpirationDate"];
        }

        sec.SetInternalLoginUserOptions( name,
                                         disabled,
                                         bypassStrength,
                                         bypassHistory,
                                         stdPasswordExpirationDays,
                                         passwordExpirationDays,
                                         bypassLockout,
                                         bypassInactivitySuspension,
                                         expireAccount,
                                         expirationDate );
        return user;
    }

    def methodMissing(String name, args) {
        return createJomaBuilderObject( name as String, args[0] as Map );
    }

    Object createJomaBuilderObject( String name, Map attributes ) {
        println "JomaBuilder: Creating $name object ${attributes['Name']}";
        def node;
        JomaBuilderObject obj = null;
        /* Machines, Domains, and Persons are unique in that there should only
         * ever be one representation of each.  So first try to find
         * a matching node before creating a new one. */
        switch( name ) {
            case "Machine":
                obj = createJomaBuilderObject( "MachineReference", attributes );
                if( obj ) {
                    return obj;
                }
                // If we couldn't find the Machine then create one
                break;
            case "AuthenticationDomain":
                obj = createJomaBuilderObject( "AuthenticationDomainReference", attributes );
                if( obj ) {
                    return obj;
                }
                // If we couldn't find the AuthenticationDomain then create one
                break;
            case "Person":
                obj = createJomaBuilderObject( "PersonReference", attributes );
                if( obj ) {
                    return obj;
                }
                // If we couldn't find the AuthenticationDomain then create one
                break;
        }

        if( name.endsWith( "Reference" ) ) {
            name = name.substring( 0, name.length() - "Reference".length() );
            StringBuilder xmlSelect = new StringBuilder();
            xmlSelect.append( "<XMLSelect search=\"$name[" );
            Boolean first = true;
            attributes.each{
                if( first ) {
                    first = false;
                } else {
                    xmlSelect.append( " and " );
                }
                xmlSelect.append( "@${it.key}='${it.value}'" );
            }
            xmlSelect.append( "]\" />" );
            println "JomaBuilder: Searching for object with " + xmlSelect.toString();
            def nodes = util.getMetadataObjectsSubset( store, repository.getFQID(), name,
                                                            MdOMIUtil.OMI_XMLSELECT |
                                                            MdOMIUtil.OMI_ALL_SIMPLE |
                                                            MdOMIUtil.OMI_TEMPLATE |
                                                            MdOMIUtil.OMI_GET_METADATA,
                                                            xmlSelect.toString() );
            if( nodes.size() > 1 ) {
                println "JomaBuilder: WARN: More than one $name object found.";
            } else if( nodes.size() < 1 ) {
                println "JomaBuilder: WARN: No $name object found.";
            }
            node = nodes[0];
            if( !node )
                return null;
            obj = new JomaBuilderObject( node, name, this );
        } else {
            /* Add any known attributes for this type */
            switch( name ) {
                case "Person":
                    if( !attributes.containsKey("UsageVersion") ){ attributes["UsageVersion"] = "1000000"; }
                    if( !attributes.containsKey("PublicType"  ) ){attributes["PublicType"] = "User"; }
                    break;
            }
            node = factory.createComplexMetadataObject( store, "", name, repositoryId );
            if( !node )
                return null;
            obj = new JomaBuilderObject( node, name, this );
            attributes.each{
                obj.setAttribute( it.key, it.value );
            }
        }
        return obj;
    }

    String resolveLabel( String name ) {
        if( nameResolver.containsKey( name ) ) {
            name = nameResolver.getProperty( name ).replaceAll("&", "");
        }
        return name;
    }

}