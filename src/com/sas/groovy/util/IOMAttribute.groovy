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
import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.AnySeqHelper;
import com.sas.iom.SASIOMDefs.StringSeqHelper;
import com.sas.iom.SASIOMDefs.BooleanSeqHelper;

public class IOMAttribute {
    def filteredList;
    String categoryName;
    String name;
    String description;
    Boolean updatable;
    String type;
    Any value;
    Boolean persistChanges;

    static {
        Any.metaClass.toString = { -> IOMServer.AnyToString( delegate ) }
    }
    
    def IOMAttribute( args ) {
        // Start with this object updatable and not persisting changes
        // because we can't skip the setter even when calling from the constructor.
        persistChanges = false;
        updatable = true;
        args.each { k, v ->
            switch( k ) {
                case "filteredList": filteredList = v; break;
                case "categoryName": categoryName = v; break;
                case "name":
                    if( v == "" ) {
                        name = "Root"
                    } else {
                        name = v;
                    }
                    break;
                case "description": description = v; break;
                case "type": type = v; break;
                case "value": value = v; break;
            }
        }
        // Handle updatable last so that the change to value can
        // go through in the constructor.  This is because we can't
        // skip the setter even when calling from the constructor.
        if( args.containsKey("updatable") ) {
            updatable = args.get("updatable");
        }
        persistChanges = true;
    }
    
    String toString() {
        "$name = " + IOMServer.AnyToString( value );
    }
    
    def Any getValue(){ value }

    def void setValueAs( Any newValue ){
        if( !updatable ) {
            if( value == null ) { // This is an intermediate or nonexistent property
                throw new Exception( "The attribute $name does not exist." );
            }
            throw new Exception( "The attribute $name is not updatable." );
        }
        if( !persistChanges ) {
            value = newValue;
            return;
        }
        def actualName = name;
        if( actualName == "Root" ) {
            actualName = "";
        }
        filteredList.SetValue( actualName, newValue as Any );
        value = newValue;        
    }
    
    def void setValueAs( String newValue ){
        setValueAs( IOMServer.StringToAny( filteredList._orb(), type, newValue ) );
    }
    
    def void setValueAs( unsupported ) {
        throw new Exception("IOMAttribute::setValueAs unsupported type " + unsupported.class);
    }
    
    def void setValue( newValue ) {
        // Need special secondary dispatch b/c setters can't be
        // overloaded based on argument type.
        setValueAs( newValue );
    }
        
    def propertyMissing( String property ) {
        property = "$name.$property";
        AnySeqHolder attributes = new AnySeqHolder();
        try {
            filteredList.GetAttribute( property, "", attributes );
        } catch( Exception e ){} // Hide this so we can keep chaining
        if( !attributes.value ) {
            return new IOMAttribute(filteredList:filteredList, categoryName:categoryName, name:property, updatable:false);
        }
        return new IOMAttribute( filteredList:filteredList,
            categoryName:categoryName,
            name:attributes.value[0].extract_string(),
            description:attributes.value[1].extract_string(),
            updatable:attributes.value[2].extract_boolean(),
            type:attributes.value[3].extract_string(),
            value:attributes.value[4] );
    }
}
