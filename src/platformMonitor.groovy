import com.sas.groovy.test.PlatformTestCase;
import com.sas.groovy.util.IOMHelper;
import com.sas.groovy.util.PlatformServicesUtil;

import org.apache.log4j.Logger;

import com.sas.services.user.UserContextInterface;
import com.sas.services.user.UserServiceInterface;
import com.sas.services.connection.Server;
import com.sas.services.connection.BridgeServer;
import com.sas.services.connection.ConnectionFactoryConfiguration;
import com.sas.services.connection.ConnectionInterface;
import com.sas.services.connection.ManualConnectionFactoryConfiguration;
import com.sas.services.connection.platform.PlatformConnectionFactoryInterface;
import com.sas.services.connection.platform.PlatformConnectionFactoryManager;
import com.sas.services.deployment.CorePlatformServices;

import com.sas.services.information.RepositoryInterface;
import com.sas.services.information.FilterComponent;
import com.sas.services.information.metadata.OMRLogicalServer
import com.sas.services.information.metadata.LogicalServerFilter;
import com.sas.services.information.Filter;

import com.sas.iom.SASIOMCommon.IServerInformation;
import com.sas.iom.SASIOMCommon.IServerInformationHelper;
import com.sas.iom.SASIOMCommon.IFilteredList;
import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.AnySeqHelper;
import com.sas.iom.SASIOMDefs.StringSeqHelper;
import com.sas.iom.SASIOMDefs.BooleanSeqHelper;

import org.omg.CORBA.Any;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.swing.SwingBuilder;

import java.awt.BorderLayout as BL
import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.event.*;
import javax.swing.JFrame;

/* The PlatformMonitor class connects to a metadata server and at a user specified interval
 * does the following:
 *
 *  1) Connect to each spawner and fetch it's Counter values
 *  2) Call a user specified script and pass those values to the script.
 *
 * The script can rely on a number of global variables and functions provided to it.
 *     user:            The authenticated UserContextInterface that was used to connect to
 *                      the Metadata server.
 *     textArea:        The JTextComponent that is used for displaying log messages.
 *     newLine:         A convinience variable that contains the value of System.properties.'line.separator'.
 *     append(str):     A convinience method that will call textArea.append() and then scroll to the
 *                      bottom of the text area.
 *     appendln(str):   A convinience method that will call textArea.append() for the given text and a new
 *                      line and then scroll to the bottom of the text area.
 *     valuesBySpawner: A map.  The key is the name of the spawner that provided the counters.  The value
 *                      is a map of counter name, counter value pairs.
 *     valuesByCounter: A map.  The key is the counter name.  The value is a map of spawner name, counter value pairs.
 *     spawnerInfo:     A map.  The key is the name of the spawner.  The value is a map of name, value pairs where the
 *                      name is "Host", "Port", "Protocol", "CLSID", or "Servers".  The value associated with
 *                      the "Servers" key is itself a map that describes each server that can be launched by this
 *                      spawner.
 *
 * In addition to these global variables and functions each discovered hostname and counter is also it's own
 * script property.  This means that hostnames and counter names in bare code become functions that take a
 * closure that recieves the associated map as it's only argument (see examples)
 *
 * In addition to the PlatformTestCase, ThreadedTestCase, and TestCase properties this class expects
 * the following:
 *
 * -Dtriggers
 *     The file that contains the Groovy code to be executed on every polling.frequency interval.
 *
 * -Dpolling.frequency
 *     The amount of time to wait between server queries.  Default: 5s.  This is a string of
 *     the form "Number[h|m|s]" where Number is a number and is followed by either "h" for
 *     hours, "m" for minutes, "s" for seconds, or nothing in which case seconds are assumed.
 *
 *
 * Execute the given triggers.groovy script on an interval of 5 seconds over a user connection to a metadata server
 * runvjrscript -Diomsrv.metadatasrv.host=ecmsrv01.na.sas.com -Diomsrv.metadatasrv.port=8561 -Diomsrv.username=sasadm@saspw -Diomsrv.password=**** -Dtriggers=triggers.groovy platformMonitor.groovy
 *
 */
public class PlatformMonitor extends PlatformTestCase
{
    Logger logger = Logger.getLogger(appLogger.getName() + ".PlatformMonitor");
    String triggers;
    Long   pollingFrequency;
    /*
    String user;
    String pass;
    String domain;
    String rpos;
    */

    void printHelp() {
        super.printHelp();
        println """
=============Test Properties=============

triggers
The file that contains the Groovy code to be executed on every polling.frequency interval.

polling.frequency
The amount of time to wait between server queries.  Default: 5s.  This is a string of the form \"Number[h|m|s]\" where Number is a number and is followed by either \"h\" for hours, \"m\" for minutes, \"s\" for seconds, or nothing in which case seconds are assumed.""";
    }

    void setUp() {
        Properties p = getProperties();
        triggers = p.'triggers';
        if( !triggers ) {
            throw new IllegalArgumentException( "'triggers' command line parameter is not optional.  It should be the full path to a file that contains the Groovy code that looks at the counter values and decides what to do." );
        }
        pollingFrequency = timeInMillis( p.getProperty("polling.frequency", "5s" ) );
        setUpWithAuthenticationCLI();
        /*
        Properties p = System.properties;
        user         = p.'iomsrv.username'
        pass         = p.'iomsrv.password'
        domain       = p.getProperty("iomsrv.metadatasrv.authdomain", "DefaultAuth")
        rpos         = p.getProperty("oma.repository.foundation.name", "Foundation")
        String host  = p.'iomsrv.metadatasrv.host'
        String port  = p.'iomsrv.metadatasrv.port'

        PlatformServicesUtil.StartCorePlatformServices( host, port, domain );
        */
    }

    void tearDown() {
        /*Wait for the LiveGraph to go away before doing teardown work */
    }

    void doTearDown(){
        super.tearDown();
    }

    public void testGetConnectionToObjectSpawner() throws Exception
    {
        /* Set up our worker thread variables
         */
         AtomicBoolean keepRunning = new AtomicBoolean( true );
         AtomicBoolean paused = new AtomicBoolean( false );
         Thread worker = null;

        /* Set up our GUI
         */
        def self = this;
        def window;
        def output;
        /* Define some helper closures
         */
        def closing = {
            synchronized( keepRunning ) {
                keepRunning.set( false );
                keepRunning.notifyAll();
            }
            if( worker )
                worker.join();
            self.doTearDown();
        }
        def append = { str ->
            output.append( str.toString() );
            output.caretPosition = output.text.length();
        }
        def appendln = { str ->
            output.append( str.toString() + System.properties.'line.separator' );
            output.caretPosition = output.text.length();
        }

        /* Build the GUI
         */
        window = new SwingBuilder().frame( title:'Platform Monitor - Log',
                                           size:[640,480],
                                           defaultCloseOperation:JFrame.EXIT_ON_CLOSE,
                                           windowClosing: closing ) {
            panel( border:emptyBorder(1,1,1,0) ) {
                borderLayout()
                label( constraints: BL.NORTH,
                       text:"Trigger file: " + new File(triggers).absolutePath )
                scrollPane{
                    output = textArea( constraints: BL.CENTER,
                                       text:"",
                                       editable: false )
                }
                panel( constraints:BL.SOUTH,
                       border:emptyBorder(0,0,0,1) ) {
                    borderLayout()
                    button( text:'Clear',
                            actionPerformed: {output.text = ""},
                            constraints:BL.WEST )
                    panel( constraints:BL.CENTER )
                    button( text:'Pause',
                            constraints:BL.EAST,
                            actionPerformed: {e->
                                if( e.actionCommand=="Pause" ) {
                                    paused.set(true);
                                    e.source.text = "Resume";
                                    appendln( "Paused" );
                                } else {
                                    paused.set(false);
                                    e.source.text = "Pause";
                                    appendln( "Resumed" );
                                }
                            } )
                }
            }
        }
        window.show();

        /* Create and authenticate the user
         */
        /*
          UserServiceInterface userService = CorePlatformServices.getUserService();
          UserContextInterface iomUser = userService.newUser(user,pass,domain);
        */
        UserContextInterface iomUser = getUser();

        /* Set up our Groovy Shell
         */
        GroovyShell shell = new GroovyShell();
        Binding binding = new Binding();
        /* Go ahead and set the current user and the textArea in the binding
         */
        //http://sww.sas.com/JavaXRef/JXRDataExtractor?requesttype=details&object=class&class=ServerInterface&pkg=com.sas.services.information&jar=sas.svc.core.jar&jxrenv=day%2Fvert-v930&showquery=off
        binding.setVariable("user", iomUser);
        binding.setVariable("textArea", output);
        binding.setVariable("newLine", System.properties.'line.separator');
        binding.setVariable("append", append);
        binding.setVariable("appendln", appendln);

        /* Get the repository to search
         */
        RepositoryInterface repository = iomUser.authServer.getRepository( repositoryName );
        output.text += "Connected to ${repository} as ${iomUser}" + System.properties.'line.separator';

        /* Create a spawner filter
         */
        Filter spwnFilter = new Filter( "PublicType", FilterComponent.EQUALS, "Spawner.IOM" );

        worker = Thread.start {
            ConnectionInterface connection = null;
            PlatformConnectionFactoryInterface factory = null;
            try {
                /* Find the spawner server defs
                 */
                synchronized( keepRunning ) {
                    while( keepRunning.get() ) {
                        if( paused.get() ) {
                            keepRunning.wait( pollingFrequency );
                            continue;
                        }
                        Script script;
                        try {
                            script = shell.parse( new File(triggers) );
                        } catch( Throwable t ) {
                            StringWriter sw = new StringWriter();
                            t.printStackTrace(new PrintWriter(sw));
                            String stacktrace = sw.toString();
                            output.append( System.properties.'line.separator' +
                                            "ERROR:" + System.properties.'line.separator' +
                                            stacktrace + System.properties.'line.separator' );
                            output.caretPosition = output.text.length()
                            keepRunning.wait( pollingFrequency );
                            continue;
                        }

                        script.setBinding( binding );

                        List serverDefs = repository.search( spwnFilter );
                        output.text += "Found Spawners ${serverDefs.collect{ it.getName()}}" + System.properties.'line.separator';

                        def spawnerInfo = [:];
                        def valuesBySpawner = [:];
                        def valuesByCounter = [:];

                        serverDefs.each { serverDef ->
                            /* First fill in the spawnerInfo */
                            def spawnerName = serverDef.getName();
                            def host = serverDef.getHost();
                            def port = serverDef.getTcpPort();
                            def domain = serverDef.getDomains()[0];
                            def servers = [:];
                            serverDef.getServed().each {
                                servers[it.getName()] = ["Host": it.getHost(),
                                                     "Port": it.getTcpPort(),
                                                     "Protocol": it.getApplicationProtocol(),
                                                     "CLSID": it.getClassIdentifier()
                                                    ];
                            }
                            spawnerInfo[spawnerName] = ["Host": host, "Port": port, "Domain": domain, "Servers": servers];




                            Server bridgeServer = new BridgeServer( "00000000-0000-0000-0000-000000000000", host, port );
                            bridgeServer.setDomain( domain );
                            ConnectionFactoryConfiguration factoryConfig = new ManualConnectionFactoryConfiguration( bridgeServer );
                            factory = PlatformConnectionFactoryManager.getPlatformConnectionFactory( factoryConfig );
                            connection = factory.getConnection( iomUser );
                            IServerInformation iInfo = IServerInformationHelper.narrow(connection.getObject());
                            if( iInfo == null ) {
                                output.append( "ERROR: IServerInformation interface not supported on ${host}" + System.properties.'line.separator' );
                                connection.close();
                                connection = null;
                                factory.getAdminInterface().destroy();
                                return;
                            }
                            AnySeqHolder attributes = new AnySeqHolder();
                            String[] names = [];   // The first element of the returned attributes array
                            String[] descs = [];   // The second element of the returned attributes array
                            Boolean[] updates = [];// The third element of the returned attributes array
                            String[] types = [];   // The fourth element of the returned attributes array
                            Any[] values = [];     // The fifth element of the returned attributes array
                            IFilteredList filteredList = iInfo.UseCategory( "Counters", "" );
                            filteredList.GetAttributes( "", attributes );
                            names   = StringSeqHelper.extract( attributes.value[0] );
                            descs   = StringSeqHelper.extract( attributes.value[1] );
                            updates = BooleanSeqHelper.extract( attributes.value[2] );
                            types   = StringSeqHelper.extract( attributes.value[3] );
                            values  = AnySeqHelper.extract( attributes.value[4] );
                            def valuesForSpawner = [:];
                            for( def j in 0..names.length-1 ) {
                                def name = names[j];
                                valuesForSpawner.put( name, IOMHelper.ExtractAny(values[j]) )
                                try {
                                    valuesByCounter[name].put(spawnerName,IOMHelper.ExtractAny(values[j]));
                                } catch( Throwable t ) {
                                    valuesByCounter[name] = [(spawnerName): IOMHelper.ExtractAny(values[j])];
                                }
                                script.setProperty( name, {c -> c(valuesByCounter[name])} )
                                //logger.info( names[j] + " = " + IOMHelper.AnyToString(values[j]) );
                            }
                            valuesBySpawner[spawnerName] = valuesForSpawner;
                            script.setProperty( spawnerName, {c -> c.maximumNumberOfParameters>1?c(valuesForSpawner, spawnerInfo[spawnerName]):c(valuesForSpawner)} )
                            connection.close();
                            connection = null;
                            factory.getAdminInterface().destroy();
                            factory = null;
                        }
                        binding.setVariable("spawnerInfo", spawnerInfo);
                        binding.setVariable("valuesBySpawner", valuesBySpawner);
                        binding.setVariable("valuesByCounter", valuesByCounter);
                        try {
                            script.run();
                        } catch( Throwable t ) {
                            StringWriter sw = new StringWriter();
                            t.printStackTrace(new PrintWriter(sw));
                            String stacktrace = sw.toString();
                            output.append( System.properties.'line.separator' +
                                            "ERROR:" + System.properties.'line.separator' +
                                            stacktrace + System.properties.'line.separator' );
                        }
                        output.caretPosition = output.text.length()
                        shell.resetLoadedClasses();
                        keepRunning.wait( pollingFrequency );
                    }
                }
            }
            finally
            {
                if( connection ) {
                    connection.close();
                }
                if( factory ) {
                    factory.getAdminInterface().destroy();
                }
            }
        }
    }
}

