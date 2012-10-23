package com.sas.groovy.util;

import com.sas.metadata.remote.AssociationList;

import java.lang.reflect.Method;

class JomaBuilderObject {
    Object              jomaObject          = null;
    String              typeName            = null;
    String              templateTypeName    = null;
    JomaBuilder         builder             = null;
    JomaBuilderTemplate template            = null;

    JomaBuilderObject( Object obj, String n, JomaBuilder b ) {
        jomaObject = obj;
        typeName = n;
        builder = b;
    }

    JomaBuilderObject commit() {
        builder.commit();
        return this;
    }

    JomaBuilderObject delete() {
        jomaObject.delete();
        jomaObject = null;
        return this;
    }

    void setTemplateInfo( JomaBuilderTemplate t, String ttn ) {
        template = t;
        templateTypeName = ttn;
    }

    def methodMissing(String name, args) {
        if( name.startsWith( "with" ) ) {
            return createAssociation( name.substring(4), args );
        } else if( name.startsWith( "add" ) ) {
            return createAssociation( name.substring(3), args );
        } else {
            throw new MissingMethodException(name, this.class, args);
        }
    }

    void setAttribute( String name, String value ) {
        value = builder.resolveLabel( value );
        try {
            Method setter = jomaObject.class.getMethod( "set$name", [String] as Class[] );
            setter.invoke( jomaObject, value );
        } catch( NoSuchMethodException e ) {
            throw new Exception( "The ${jomaObject.class.name} type does not have a ${name} attribute.", e );
        }
    }

    JomaBuilderObject createAssociation( String name, args ) {
        switch( name ) {
            case "ServerComponents":
            case "Servers":
            case "Spawners":
                /* ServerComponents, Servers, and Spawners go in the
                 * UsingComponents association.  This just makes it read easier. */
                return createDefaultAssociation( "UsingComponents", args );
                break;
            case "SpawnedServers":
                /* SpawnedServers go in the UsedByComponents association.
                 * This just makes it read easier. */
                 return createDefaultAssociation( "UsedByComponents", args );
                 break;
            case "Machines":
                /* Machines go in both the AssociatedMachine association and
                 * the SoftwareTrees association */
                AssociationList assoc = jomaObject.getAssociatedObjects( "AssociatedMachine" );
                if( assoc == null ){
                    assoc = new AssociationList( "AssociatedMachine" );
                }
                args.each {
                    assoc.add( it.jomaObject );
                }
                jomaObject.setMdObjectAssociation( assoc );

                /* Create a Tree object */
                def tree = builder.createJomaBuilderObject( "Tree", [Name:"MachineGroup", TreeType:"MachineGroup"] );
                if( !tree ) {
                    return this;
                }

                /* Add the machine to the Members association of the Tree object */
                assoc = tree.jomaObject.getAssociatedObjects( "Members" );
                if( assoc == null ) {
                    assoc = new AssociationList( "Members" );
                }
                args.each {
                    assoc.add( it.jomaObject );
                }
                tree.jomaObject.setMdObjectAssociation( assoc );

                /* Add the Tree object to the SoftwareTrees association of the parent object */
                assoc = jomaObject.getAssociatedObjects( "SoftwareTrees" );
                if( assoc == null ) {
                    assoc = new AssociationList( "SoftwareTrees" );
                }
                assoc.add( tree.jomaObject );
                jomaObject.setMdObjectAssociation( assoc );
                return this;
                break;
            case "Properties":
                /* Properties association is special */
                /* If a property by this name already exists
                 * then we are just updating it.  If not
                 * then we need to look it up in the template
                 * to see what it's default attributes are and
                 * use them. */
                args.each{ newProperty ->
                    boolean found = false;
                    /* Look in our curret property list */
                    def currentProperties = jomaObject.getAssociatedObjects( "Properties" );
                    if( currentProperties ) {
                        currentProperties.each{ currentProperty ->
                            /* If we found it then update the default value and delete
                             * the new property we were trying to add */
                            if( !found && currentProperty.getPropertyName() == newProperty.jomaObject.getPropertyName() ) {
                                currentProperty.setDefaultValue( newProperty.jomaObject.getDefaultValue() );
                                newProperty.delete();
                                found = true;
                            }
                        }
                    }
                    /* If didn't find the property in the current list then look for
                     * a template that tells us what it should look like */
                    if( !found && template ) {
                        String propertyName = newProperty.jomaObject.getPropertyName();
                        Node propertyDef = template.getPropertyDef( templateTypeName, propertyName );
                        /* If we found a definition for a property with this name update
                         * it's default value and add it to this object */
                        if( propertyDef ) {
                            def newValue = newProperty.jomaObject.getDefaultValue();
                            if( newValue && newValue != "" ) {
                                propertyDef.@DefaultValue = newValue;
                            }
                            template.addProperty( this, propertyDef );
                            newProperty.delete();
                            found = true;
                        }
                    }
                    /* If we didn't find this property in our current list
                     * of properties and we didn't find a template for it
                     * then just create a regular Properties association and
                     * expect that the user added all the attributes they wanted */
                    if( !found ) {
                        println "JomaBuilderObject: WARN: Adding ${newProperty.jomaObject.getName()} property without a template or default";
                        createDefaultAssociation( name, [newProperty] );
                    }
                }
                return this;
                break;
        }
        /* If it wasn't a special association then just add it as a regular association */
        return createDefaultAssociation( name, args );
    }

    JomaBuilderObject createDefaultAssociation( String name, args ) {
        def assoc = jomaObject.getAssociatedObjects(name);
        if( assoc == null ){
            assoc = new AssociationList( name );
        }
        /* args must be an array of JomaBuilderObjects */
        args.each {
            println "JomaBuilderObject: Adding ${it.jomaObject.getName()} to ${name} association of ${jomaObject.getName()}";
            assoc.add( it.jomaObject );
        }
        jomaObject.setMdObjectAssociation( assoc );
        return this;
    }
}