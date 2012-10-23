import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import org.apache.log4j.Logger;

import com.sas.iom.SAS.IWorkspace;
import com.sas.iom.SAS.IWorkspaceHelper;
import com.sas.iom.SAS.IDataService;
import com.sas.iom.SAS.ILibref;
import com.sas.iom.SAS.IDataSet;
import com.sas.iom.SAS.IDataSetPackage.BindKeys;

import com.sas.iom.SASIOMDefs.DateTimeHolder;
import org.omg.CORBA.StringHolder;
import org.omg.CORBA.IntHolder;

import com.sas.iom.SASIOMDefs.DateTimeHolder;
import com.sas.iom.SASIOMDefs.VariableArray2dOfStringHolder;
import com.sas.iom.SASIOMDefs.VariableArray2dOfDoubleHolder;
import com.sas.iom.SASIOMDefs.VariableArray2dOfOctetHolder;
import com.sas.iom.SASIOMDefs.LongSeqHolder;
import com.sas.iom.SASIOMDefs.ShortSeqHolder;
import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.iom.SASIOMDefs.OctetSeqHolder;

/* The ServerWorkspaceFetch class tests reading the contents of a dataset
 * from a workspace server using a direct manual connection.  In addition
 * to the ServerTestCase, ThreadedTestCase, and TestCase properties this
 * class expects the following:
 *
 * -Ddataset
 *     The name of the dataset to fetch.
 *
 * -Dprint
 *     If true (present on the command line) then print the contents of
 *     the dataset.  Note that even if false, the dataset is still
 *     pulled over the wire to test throughput.
 *
 * See com.sas.groovy.test.ServerTestCase for connection options.
 *
 * Example command lines
 *
 * Submit the contents of sashelp.class over an SSPI connection, one iteration of one thread
 * runvjrscript -Diomsrv.uri=iom://localhost:8591;Bridge -Ddataset=sashelp.class serverWorkspaceFetch.groovy
 *
 * Submit the contents of sashelp.class over an SSPI connection, two iterations of four threads
 * runvjrscript -Diomsrv.uri=iom://localhost:8591;Bridge -Ddataset=sashelp.class -Diteration.count=2 -Dthread.count=4 serverWorkspaceFetch.groovy
 *
 * Submit the contents of sashelp.class over a User connection, one iteration of one thread
 * runvjrscript -Diomsrv.uri=iom://localhost:8591;Bridge;USER=carynt\sasiom1,PASS=123456 -Ddataset=sashelp.class serverWorkspaceFetch.groovy
 *
 */

class ServerWorkspaceFetch extends ServerTestCase {

    Logger logger = Logger.getLogger(appLogger.getName() + ".ServerWorkspaceFetch");
    String libref = "sashelp";
    String dataset = "";
    Boolean printit = false;

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

dataset
The name of the dataset to fetch.

printlog
If true (present on the command line) then print the contents of the dataset.  Note that even if false, the dataset is still pulled over the wire to test throughput.""";
    }

    void setUp() {
        Properties p = getProperties();
        if( !p.containsKey("dataset") ) {
            throw new IllegalArgumentException( "dataset argument is required" );
        } else {
            dataset = p.getProperty("dataset");
            if( dataset.contains('.') )
                (libref,dataset) = dataset.split("\\.");
        }
        printit = p.containsKey( "print" );
        super.setUp();
    }

    void testWorkspaceFetch() {
        onExecutions { iteration, thread ->

            IWorkspace iWorkspace = IWorkspaceHelper.narrow( getTopLevelObject( thread ) );
            IDataService iDataService = iWorkspace.DataService();
            ILibref iLibref = iDataService.UseLibref( libref );

            int flags = ILibref.ReadOnlyAccess;
            String options = "";
            String[] passwords = [];
            StringHolder labelHolder = new StringHolder();
            StringHolder typeHolder = new StringHolder();
            DateTimeHolder createdHolder = new DateTimeHolder();
            DateTimeHolder modifiedHolder = new DateTimeHolder();
            IntHolder lengthHolder = new IntHolder();
            StringHolder compressHolder = new StringHolder();
            IntHolder bmkLengthHolder = new IntHolder();
            IntHolder logicalHolder = new IntHolder();
            IntHolder physicalHolder = new IntHolder();
            IntHolder attrsHolder = new IntHolder();

            IDataSet iDataSet = iLibref.OpenDataSet(
                    flags,
                    dataset,
                    options,
                    passwords,
                    labelHolder,
                    typeHolder,
                    createdHolder,
                    modifiedHolder,
                    lengthHolder,
                    compressHolder,
                    bmkLengthHolder,
                    logicalHolder,
                    physicalHolder,
                    attrsHolder );

            IntHolder countHolder = new IntHolder();
            StringSeqHolder namesHolder = new StringSeqHolder();
            LongSeqHolder typesHolder = new LongSeqHolder();
            LongSeqHolder lengthsHolder = new LongSeqHolder();
            StringSeqHolder labelsHolder = new StringSeqHolder();
            StringSeqHolder formatNamesHolder = new StringSeqHolder();
            ShortSeqHolder formatWidthsHolder = new ShortSeqHolder();
            ShortSeqHolder formatDecimalsHolder = new ShortSeqHolder();
            ShortSeqHolder formatLengthsHolder = new ShortSeqHolder();
            StringSeqHolder informatNamesHolder = new StringSeqHolder();
            ShortSeqHolder informatWidthsHolder = new ShortSeqHolder();
            ShortSeqHolder informatDecimalsHolder = new ShortSeqHolder();
            OctetSeqHolder sortedByHolder = new OctetSeqHolder();

            iDataSet.GetColumnDefs(
                new boolean[0],
                countHolder,
                namesHolder,
                typesHolder,
                lengthsHolder,
                labelsHolder,
                formatNamesHolder,
                formatWidthsHolder,
                formatDecimalsHolder,
                formatLengthsHolder,
                informatNamesHolder,
                informatWidthsHolder,
                informatDecimalsHolder,
                sortedByHolder);

            int col     = 0;
            def names   = namesHolder.value;
            def lengths = lengthsHolder.value;
            def types   = typesHolder.value;
            if( printit )
            {

                for( col = 0; col < names.size(); ++col )
                {
                    if( names[col].length() > lengths[col] )
                        lengths[col] = names[col].length();
                    if( types[col] == 0 /* num */ )
                        lengths[col] = 14;
                    print String.format("%${lengths[col]}s, ", names[col]);
                }
                println '';
            }

            int bindKey = BindKeys._BindKeysUnformatted;
            byte[] positionBookmark = [];
            int numberRowsToRead = 500;
            int rowsOffset = 0;
            VariableArray2dOfStringHolder characterValues = new VariableArray2dOfStringHolder()
            VariableArray2dOfDoubleHolder numericValues = new VariableArray2dOfDoubleHolder()
            VariableArray2dOfOctetHolder missingNumericValues = new VariableArray2dOfOctetHolder()
            OctetSeqHolder bookmarksHolder = new OctetSeqHolder();
            IntHolder statusHolder = new IntHolder();
            boolean done = false;

            while( !done )
            {
                iDataSet.ReadRecords(
                    flags,
                    bindKey,
                    positionBookmark,
                    numberRowsToRead,
                    rowsOffset,
                    characterValues,
                    numericValues,
                    missingNumericValues,
                    bookmarksHolder,
                    statusHolder );
                if( characterValues.value.size() < numberRowsToRead )
                    done = true;

                if( printit )
                {

                    int row = 0;
                    int numCol = 0;
                    int chrCol = 0;
                    def numVals = numericValues.value;
                    def chrVals = characterValues.value;
                    def misVals = missingNumericValues.value;

                    for( row = 0; row < numVals.size(); ++row )
                    {
                        numCol = 0;
                        chrCol = 0;
                        for( col = 0; col < types.size(); ++col )
                        {
                            switch( types[col] )
                            {
                                case 0: /* num */
                                    if( misVals &&
                                        misVals.size() >= row &&
                                        misVals[row].size >= numCol &&
                                        misVals[row][numCol] )
                                    {
                                        print String.format("%${lengths[col]}.${lengths[col]}s, ", ".");
                                        numCol++;
                                    }
                                    else
                                        print String.format("%14.8g, ", numVals[row][numCol++]);
                                    break;
                                case 1: /* chr */
                                    print String.format("%${lengths[col]}.${lengths[col]}s, ", chrVals[row][chrCol++]);
                                    break;
                            }
                        }
                        println '';
                    }
                }
            }
        }
    }
}
