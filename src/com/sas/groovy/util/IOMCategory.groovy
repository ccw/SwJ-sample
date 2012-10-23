package com.sas.groovy.util;

import com.sas.iom.SASIOMCommon.IServerInformation;
import com.sas.iom.SASIOMCommon.IServerInformationHelper;
import com.sas.iom.SASIOMCommon.IFilteredList;
import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.AnySeqHelper;
import com.sas.iom.SASIOMDefs.StringSeqHelper;
import com.sas.iom.SASIOMDefs.BooleanSeqHelper;
import org.omg.CORBA.Any;


public class IOMCategory {
    String name;
    IFilteredList filteredList;
    
    String toString() {
        name;
    }

    public getAttributes() {
        def attrs = [];
        AnySeqHolder attributes = new AnySeqHolder();
        String[] names = [];   // The first element of the returned attributes array
        String[] descs = [];   // The second element of the returned attributes array
        Boolean[] updates = [];// The third element of the returned attributes array
        String[] types = [];   // The fourth element of the returned attributes array
        Any[] values = [];     // The fifth element of the returned attributes array
        filteredList.GetAttributes( "", attributes );
        if( !attributes.value ) {
            return attrs;
        }
        names = StringSeqHelper.extract( attributes.value[0] );
        descs = StringSeqHelper.extract( attributes.value[1] );
        updates = BooleanSeqHelper.extract( attributes.value[2] );
        types = StringSeqHelper.extract( attributes.value[3] );
        values = AnySeqHelper.extract( attributes.value[4] );
        for( i in 0..names.length-1 ) {
            attrs.add( new IOMAttribute( filteredList:filteredList,
                categoryName:name,
                name:names[i],
                description:descs[i],
                updatable:updates[i],
                type:types[i],
                value:values[i] ) );
        }
        return attrs.sort{it.name};
    }
    
    def getAttribute( String property ) {
        if( property == "Root" ) {
            property = "";
        }
        AnySeqHolder attributes = new AnySeqHolder();
        try {
            filteredList.GetAttribute( property, "", attributes );
        } catch( Exception e ){}  // Hide this so we can keep chaining
        if( !attributes.value ) {
            return new IOMAttribute(filteredList:filteredList, categoryName:name, name:property, updatable:false);
        }
        return new IOMAttribute( filteredList:filteredList,
            categoryName:name,
            name:attributes.value[0].extract_string(),
            description:attributes.value[1].extract_string(),
            updatable:attributes.value[2].extract_boolean(),
            type:attributes.value[3].extract_string(),
            value:attributes.value[4] );
    }
    
    def propertyMissing( String name ) {
        return getAttribute( name );
    }
}
