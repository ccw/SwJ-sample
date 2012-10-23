import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.services.connection.ConnectionInterface;

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


/* The PlatformServerAttributes class dumps all the attributes from the
 * IServerInformation interface of a server defined in metadata.  In addition to
 * the PlatformTestCase, ThreadedTestCase, and TestCase properties this class expects the following:
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
 * All attributes over a User connection (prompted for a password and for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.username=carynt\sasiom1 platformServerAttributes.groovy
 *
 * Information attributes over an SSPI connection to the Stored Process Server named "SASApp - Logical Stored Process Server"
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Dcategory=Information -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerAttributes.groovy
 *
 * Counters attributes over a Trusted Peer connection (prompted for the server name)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trustedppeer -Dcategory=Counters platformServerAttributes.groovy
 *
 * All attributes over a Trusted User connection (prompted for trusted password and user name but not user password)
 * runvjrscript -Diomsrv.metadatasrv.host=localhost -Diomsrv.metadatasrv.port=15976 -Diomsrv.trusteduser -Doma.person.trusturs.login.userid=sastrust@saspw -Diomsrv.logicalname="SASApp - Logical Stored Process Server" platformServerAttributes.groovy
 */


class PlatformServerAttributes extends PlatformTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformServerAttributes");
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
The attribute to dump.  If not specifed then all attributes are dumped. If category is not specified then all categories are searched for the given attribute.

value
If this option is present then the attribute will be set to this value. category and attribute must also be specified.""";
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

        setUpWithServerConnectionCLI();
    }

    void testServerAttributes() {

        onExecutions { iteration, thread ->
            def user = getUser( thread );
            ConnectionInterface connection = connectionFactory.getConnection(user);
            org.omg.CORBA.Object obj = connection.getObject();
            IServerInformation iInfo = IServerInformationHelper.narrow(obj);
            if( iInfo == null ) {
                logger.error( "=====>IServerInformation interface not supported by this server" );
                connection.close();
                connection = null;
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
            removeUser(user);
            connection.close();
            connection = null;
        }
    }

/*
    void testServerAttributeDescriptions() {

        ConnectionInterface connection = connectionFactory.getConnection(userContext);
        org.omg.CORBA.Object obj = connection.getObject();
        IServerInformation iInfo = IServerInformationHelper.narrow(obj);
        iInfo.ListCategories().each{ category ->
            def attrs = [];
            AnySeqHolder attributes = new AnySeqHolder();
            String[] names = [];   // The first element of the returned attributes array
            String[] descs = [];   // The second element of the returned attributes array
            Boolean[] updates = [];// The third element of the returned attributes array
            String[] types = [];   // The fourth element of the returned attributes array
            Any[] values = [];     // The fifth element of the returned attributes array
            IFilteredList filteredList = iInfo.UseCategory( category, "" );
            filteredList.GetAttributes( "", attributes );
            if( !attributes.value ) {
                return;
            }
            names = StringSeqHelper.extract( attributes.value[0] );
            descs = StringSeqHelper.extract( attributes.value[1] );
            updates = BooleanSeqHelper.extract( attributes.value[2] );
            types = StringSeqHelper.extract( attributes.value[3] );
            values = AnySeqHelper.extract( attributes.value[4] );
            for( i in 0..names.length-1 ) {
                println category + "." + names[i] + " = " + descs[i];
            }
        }
        connection.close();
        connection = null;
    }
*/

}
