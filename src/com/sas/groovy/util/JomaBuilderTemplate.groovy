package com.sas.groovy.util;

import com.sas.metadata.remote.AssociationList;

class JomaBuilderTemplate {
    JomaBuilder builder  = null;
    Node        template = null;

    JomaBuilderTemplate( JomaBuilder b, String templatePath ) {
        builder = b;
        switch( templatePath ) {
            case "StoredProcessServerTemplate":
                templatePath = "com/sas/workspace/visuals/res/Stored_Process_Server_Template_gx.xml";
                break;
            case "WorkspaceServerTemplate":
                templatePath = "com/sas/workspace/visuals/res/Workspace_Server_Template_gx.xml";
                break;
            case "PooledWorkspaceServerTemplate":
                templatePath = "com/sas/workspace/visuals/res/Pooled_Workspace_Server_Template_gx.xml";
                break;
            case "SASApplicationServerTemplate":
                templatePath = "com/sas/workspace/visuals/res/SAS_Application_Server_Template_gx.xml";
                break;
            case "ObjectSpawnerTemplate":
                templatePath = "com/sas/workspace/visuals/res/Object_Spawner_Template_gx.xml";
                break;
        }
        ClassLoader loader = ClassLoader.getSystemClassLoader()
        URL url = loader.getSystemResource( templatePath );
        BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream() ) );
        template = new XmlParser().parseText( br.getText() );
    }

    def methodMissing(String name, args) {
        return createJomaBuilderObject( name as String, args[0] as Map );
    }

    Object createJomaBuilderObject( String name, Map attributes ) {
        println "JomaBuilderTemplate: Creating $name object ${attributes['Name']}";
        /* Set up some friendly names for servers we know about */
        switch( name ) {
            case "LogicalStoredProcessServer":
            case "LogicalWorkspaceServer":
            case "LogicalPooledWorkspaceServer":
                name = "LogicalIOMServer";
                break;
            case "LogicalStoredProcessServerReference":
            case "LogicalWorkspaceServerReference":
            case "LogicalPooledWorkspaceServerReference":
                name = "LogicalIOMServerReference";
                break;
            case "StoredProcessServer":
            case "WorkspaceServer":
            case "PooledWorkspaceServer":
                name = "IOMServer";
                break;
            case "StoredProcessServerReference":
            case "WorkspaceServerReference":
            case "PooledWorkspaceServerReference":
                name = "IOMServerReference";
                break;
            case "ObjectSpawner":
                name = "IOMSpawner";
                break;
            case "ObjectSpawnerReference":
                name = "IOMSpawnerReference";
                break;
        }
        String typeName = resolveTypeName( name );

        /* If this is a reference type then just try to create the reference */
        if( name.endsWith("Reference") ) {
            attributes["PublicType"] = resolvePublicType( name );
            return builder.createJomaBuilderObject( typeName, attributes );
        }

        /* Otherwise this is a new object */
        /* Get the default attributes and then augment them with
         * user specified attributes */
        def defaultAttributes = getDefaultAttributes( name );
        attributes.each{
            defaultAttributes[(it.key)] = it.value;
        }

        /* Create joma builder object */
        def obj = builder.createJomaBuilderObject( typeName, defaultAttributes );
        if( !obj )
            return obj;
        obj.setTemplateInfo( this, name );

        /* Add the default properties */
        addDefaultProperties( name, obj );

        /* Add default service type if there is one */
        addDefaultServiceType( name, obj );

        return obj;
    }

    def getDefaultAttributes( String name ) {
        def attributes = [:];

        def prototype = template.Metadata.Prototype.find{ it.@Id == "\$" + name };
        if( !prototype ) {
            return attributes;
        }

        def groupedProperties = prototype.depthFirst().findAll{ it.name() == "GroupedProperties" };

        groupedProperties.AttributeProperty.each{
            if( it.@IsRequired == "1" ) {
                attributes[(it.@PropertyName)] = it.@DefaultValue;
            } else {
                println "JomaBuilderTemplate: WARN: Skipping optional attribute ${it.@PropertyName}";
            }
        }
        return attributes;
    }

    void addDefaultProperties( String name, JomaBuilderObject obj ) {
        def prototype = template.Metadata.Prototype.find{ it.@Id == "\$" + name };
        if( !prototype ) {
            return;
        }

        def groupedProperties = prototype.depthFirst().findAll{ it.name() == "GroupedProperties" };

        /* Set the default properties */
        groupedProperties.Property.each{
            if( it.@IsRequired == "1" ) {
                addProperty( obj, it );
            } else {
                println "JomaBuilderTemplate: WARN: Skipping optional property ${it.@PropertyName}";
            }
        }
    }

    void addDefaultServiceType( String name, JomaBuilderObject obj ) {
        /* Add any AssociationProperty with a StoredConfiguration */
        def prototype = template.Metadata.Prototype.find{ it.@Id == "\$" + name };
        if( !prototype ) {
            return;
        }

        def groupedProperties = prototype.depthFirst().findAll{ it.name() == "GroupedProperties" };

        groupedProperties.AssociationProperty.each{
            if( it.@AssociationName == "ServiceTypes" &&
                it.StoredConfiguration &&
                it.StoredConfiguration.TextStore &&
                it.StoredConfiguration.TextStore.@StoredText ) {
                Node config = new XmlParser().parseText( it.StoredConfiguration.TextStore.@StoredText );
                def serviceType = createJomaBuilderObject( it.@MetadataType, [Name:config.DefaultValues.Value.@name[0]] );
                obj.createAssociation( it.@AssociationName, [serviceType] );
            }
        }
    }

    Node getPropertyDef( String owner, String propertyName ) {
        def prototype = template.Metadata.Prototype.find{ it.@Id == "\$" + owner };
        if( !prototype ) {
            return;
        }

        def groupedProperties = prototype.depthFirst().findAll{ it.name() == "GroupedProperties" };

        /* Set the default properties */
        Node found = null;
        groupedProperties.Property.each{
            if( !found && it.@PropertyName == propertyName ) {
                found = it;
            }
        }
        return found;
    }

    void addProperty( JomaBuilderObject obj, Node propertyDef ) {
        String propertyType = "";
        if( propertyDef.OwningType.PropertyType.@ObjRef[0] ){
            propertyType = propertyDef.OwningType.PropertyType.@ObjRef[0];
        } else if( propertyDef.OwningType.PropertyType.@Objref[0] ){
            propertyType = propertyDef.OwningType.PropertyType.@Objref[0];
        }
        Map attributes = propertyDef.attributes();
        propertyType = propertyType.substring(1);

        /* Create the property object and add it */
        def prop = builder.createJomaBuilderObject( "Property", attributes ).jomaObject;

        def assoc = obj.jomaObject.getAssociatedObjects("Properties");
        if( assoc == null ){
            assoc = new AssociationList( "Properties" );
        }
        assoc.add( prop );
        obj.jomaObject.setMdObjectAssociation( assoc );

        /* Try to add a reference to an existing type object.  Create one
           if we couldn't find one */
        def typeObj = builder.createJomaBuilderObject( "PropertyTypeReference", [Name:propertyType] );
        if( typeObj == null ){
            println "JomaBuilderTemplate: WARN: Creating type definition for $propertyType";
            def typeDef = template.Metadata.PropertyType.find{ it.@Id == "\$" + propertyType };
            String sqlType = "";
            if( typeDef.attributes().containsKey("SQLType") ){
                sqlType = typeDef.@SQLType[0];
            } else if( typeDef.attributes().containsKey("SqlType") ) {
                sqlType = typeDef.@SqlType[0];
            }
            typeObj = builder.createJomaBuilderObject( "PropertyType", [Name:typeDef.@Name, SQLType:sqlType] );

            /* Now see if there is a stored configuration */
            typeDef.StoredConfiguration.each {
                it.TextStore.each {
                    def textStore = builder.createJomaBuilderObject( "TextStore", it.attributes() ).jomaObject;
                    assoc = typeObj.jomaObject.getAssociatedObjects( "StoredConfiguration" );
                    if( assoc == null ) {
                        assoc = new AssociationList( "StoredConfiguration" );
                    }
                    assoc.add( textStore );
                    typeObj.jomaObject.setMdObjectAssociation( assoc );
                }
            }
        }

        /* Set up the type association for this property */
        assoc = prop.getAssociatedObjects( "OwningType" );
        if( assoc == null ) {
            assoc = new AssociationList( "OwningType" );
        }
        assoc.add( typeObj.jomaObject );
        prop.setMdObjectAssociation( assoc );
    }

    String resolveTypeName( String name ) {
        if( !template ) {
            return name;
        }
        String lookupName = name;
        if( name.endsWith("Reference") ) {
            lookupName = name.substring( 0, name.length() - "Reference".length() );
        }
        def prototype = template.Metadata.Prototype.find{ it.@Id == "\$" + lookupName };
        if( !prototype ) {
            return name;
        }
        if( name.endsWith("Reference") )
            return prototype.@MetadataType + "Reference";
        return prototype.@MetadataType;
    }

    String resolvePublicType( String name ) {
        String publicType = "";
        String lookupName = name;
        if( name.endsWith("Reference") ) {
            lookupName = name.substring( 0, name.length() - "Reference".length() );
        }
        def prototype = template.Metadata.Prototype.find{ it.@Id == "\$" + lookupName };
        if( !prototype ) {
            return publicType;
        }

        def groupedProperties = prototype.depthFirst().findAll{ it.name() == "GroupedProperties" };

        def publicTypeAttribute = groupedProperties.AttributeProperty.find{ it.@PropertyName == "PublicType" };
        if( !publicTypeAttribute ) {
            return publicType;
        }

        publicType = publicTypeAttribute.@DefaultValue;
        return publicType;
    }
}