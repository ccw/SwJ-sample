import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.iom.SASIOMCommon.IServerInformation;
import com.sas.iom.SASIOMCommon.IServerInformationHelper;
import com.sas.iom.SASIOMCommon.IFilteredList;

import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.AnySeqHelper;
import com.sas.iom.SASIOMDefs.StringSeqHelper;
import com.sas.iom.SASIOMDefs.BooleanSeqHelper;
import com.sas.iom.SASIOMDefs.UUID;
import com.sas.iom.SASIOMDefs.UUIDHelper;
import com.sas.iom.SASIOMDefs.DateTimeHelper;
import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;


/* The ServerServerAttributes class dumps all the attributes from the
 * IServerInformation interface of a a remote server.  In addition to
 * the ServerTestCase, ThreadedTestCase, and TestCase properties, this class
 * expects the following:
 *
 * -Dcategory
 *     The category to dump attributes for.  If not specified then all categories
 *     are dumped.
 *
 * -Dattribute
 *     The attribute to dump.  If not specifed then all attributes are dumped.
 *     If -Dcategory is not specified then all categories are searched for the
 *     given attribute.
 *
 * -Dvalue
 *     If this option is present then the attribute will be set to this value.
 *     category and attribute must also be specified.
 *
 * Example command lines
 *
 * All attributes over a User connection to a Metadata Server
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,USER=carynt\sasiom1,PASS=123456 serverServerAttributes.groovy
 *
 * Information attributes over an SSPI connection to a Stored Process Server
 * runvjrscript -Diomsrv.uri=iom://localhost:8601;Bridge;CLSID=CLSID_SASSTP -Dcategory=Information serverServerAttributes.groovy
 *
 * Counters attributes over a trusted connection to a Metadata Server
 * runvjrscript -Diomsrv.uri=iom://localhost:15976;Bridge;CLSID=CLSID_SASOMI,TRUSTEDSAS -Dcategory=Counters serverServerAttributes.groovy
 *
 */

class ServerServerAttributes extends ServerTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".ServerServerAttributes");
    String category = null;
    String attribute = null;
    String value = null;

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

category
The category to dump attributes for.  If not specified then all categories are dumped.

attribute
The attribute to dump.  If not specifed then all attributes are dumped.  If category is not specified then all categories are searched for the given attribute.

value
If this option is present then the attribute will be set to this value.  category and attribute must also be specified.""";
    }

    void setUp() {
        Properties p = getProperties();
        category = p.getProperty("category");
        attribute = p.getProperty("attribute");
        value = p.getProperty("value");
        /* If a value was given then both attribute and category must be specified.
         * Batch updating is not supported.
         */
        if( value && ( !attribute || !category ) ) {
            throw new IllegalArgumentException( "The category and attribute options must be set if value is set" );
        }
        super.setUp();
    }

    void testServerAttributes() {

        onExecutions { iteration, thread ->
            org.omg.CORBA.Object obj = getTopLevelObject( thread );
            IServerInformation iInfo = IServerInformationHelper.narrow(obj);
            if( iInfo == null ) {
                logger.info( "=====>IServerInformation interface not supported by this server" );
                return;
            }

            String[] categories = [];
            if( category ) {
                categories = [category];
            } else {
                categories = iInfo.ListCategories();
            }
            categories.each { currentCategory ->
                AnySeqHolder attributes = new AnySeqHolder();
                String[] names = [];   // The first element of the returned attributes array
                String[] descs = [];   // The second element of the returned attributes array
                Boolean[] updates = [];// The third element of the returned attributes array
                String[] types = [];   // The fourth element of the returned attributes array
                Any[] values = [];     // The fifth element of the returned attributes array
                IFilteredList filteredList = iInfo.UseCategory( currentCategory, "" );
                if( attribute ) {
                    filteredList.GetAttribute( attribute, "", attributes );
                    names   = [attributes.value[0].extract_string()];
                    descs   = [attributes.value[1].extract_string()];
                    updates = [attributes.value[2].extract_boolean()];
                    types   = [attributes.value[3].extract_string()];
                    values  = [attributes.value[4]];
                } else {
                    filteredList.GetAttributes( "", attributes );
                    names   = StringSeqHelper.extract( attributes.value[0] );
                    descs   = StringSeqHelper.extract( attributes.value[1] );
                    updates = BooleanSeqHelper.extract( attributes.value[2] );
                    types   = StringSeqHelper.extract( attributes.value[3] );
                    values  = AnySeqHelper.extract( attributes.value[4] );
                }

                if( value ) {
                    filteredList.SetValue( attribute, IOMHelper.StringToAny( filteredList._orb(), types[0], value ) );
                } else {
                    for( i in 0..names.length-1 ) {
                        logger.info( currentCategory + ":" + names[i] + " = " + IOMHelper.AnyToString(values[i]) );
                    }
                }
            }
        }
    }
}
