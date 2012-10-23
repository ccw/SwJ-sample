package com.sas.groovy.util;

import com.sas.groovy.test.ServerTestCase;
import com.sas.groovy.util.IOMHelper;

import com.sas.iom.SASIOMCommon.IServerAdmin;
import com.sas.iom.SASIOMCommon.IServerAdminHelper;
import com.sas.iom.SASIOMCommon.IServerAdministration;
import com.sas.iom.SASIOMCommon.IServerAdministrationHelper;
import com.sas.iom.SASIOMCommon.IServerInformation;
import com.sas.iom.SASIOMCommon.IServerInformationHelper;
import com.sas.iom.SASIOMCommon.IServerStatus;
import com.sas.iom.SASIOMCommon.IServerStatusHelper;
import com.sas.iom.SASIOMCommon.IServerSessions;
import com.sas.iom.SASIOMCommon.IServerSessionsHelper;
import com.sas.iom.SASIOMCommon.ISessionAdmin;
import com.sas.iom.SASIOMCommon.ISessionAdminHelper;
import com.sas.iom.SASIOMCommon.ISessionAdministration;
import com.sas.iom.SASIOMCommon.ISessionAdministrationHelper;
import com.sas.iom.SASIOMCommon.ServerState;

import com.sas.iom.ObjectSpawner.ISpawnerInformation;
import com.sas.iom.ObjectSpawner.ISpawnerInformationHelper;
import com.sas.iom.ObjectSpawner.ISpawnerAdministration;
import com.sas.iom.ObjectSpawner.ISpawnerAdministrationHelper;

import com.sas.iom.SASIOMDefs.UUID;
import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.iom.SASIOMDefs.StringSeqHelper;
import com.sas.iom.SASIOMDefs.DateTimeSeqHolder;
import com.sas.iom.SASIOMDefs.DateTimeHolder;
import com.sas.iom.SASIOMDefs.UUIDSeqHolder;
import com.sas.iom.SASIOMDefs.DoubleSeqHolder;
import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.LongSeqHolder;
import com.sas.iom.SASIOMDefs.AnySeqHelper;
import com.sas.iom.SASIOMDefs.BooleanSeqHelper;
import com.sas.iom.SASIOMDefs.VariableArray2dOfStringHolder;

import com.sas.iom.orb.UUIDConverter;

import com.sas.services.util.DateUtil;
import com.sas.net.util.DateConverter;

import org.omg.CORBA.BooleanHolder;
import org.omg.CORBA.Any;

import java.text.SimpleDateFormat;

import jline.ConsoleReader;
import jline.SimpleCompletor;

class CLIIOMAdmin {
    Object                 topLevel              = null;
    IServerAdmin           serverAdmin           = null;
    IServerAdministration  serverAdministration  = null;
    IServerStatus          serverStatus          = null;
    ISpawnerInformation    spawnerInformation    = null;
    ISpawnerAdministration spawnerAdministration = null;
    IServerInformation     serverInformation     = null;
    IServerSessions        serverSessions        = null;
    ISessionAdmin          sessionAdmin          = null;

    def connect = null;
    def disconnect = null;

    CLIIOMAdmin( Object top, conClosure, disconClosure ) {
        topLevel              = top;
        connect               = conClosure;
        disconnect            = disconClosure;
        serverAdmin           = IServerAdminHelper.narrow(topLevel);
        serverAdministration  = IServerAdministrationHelper.narrow(topLevel);
        serverStatus          = IServerStatusHelper.narrow(topLevel);
        spawnerInformation    = ISpawnerInformationHelper.narrow(topLevel);
        spawnerAdministration = ISpawnerAdministrationHelper.narrow(topLevel);
        serverInformation     = IServerInformationHelper.narrow(topLevel);
        serverSessions        = IServerSessionsHelper.narrow(topLevel);
        sessionAdmin          = ISessionAdminHelper.narrow(topLevel);
    }

    void run() {
        Boolean cont = true;
        print "\nServerAdmin> ";
        ConsoleReader console = new jline.ConsoleReader();
        console.setUseHistory( true );
        def line = console.readLine().trim();
        while( 1 ) {
            try {
                cont = execute( line );
                if( !cont ) {
                    return;
                }
            } catch( Throwable t ) {
                println t.toString();
            }
            print "\nServerAdmin> ";
            line = console.readLine();
        }
    }

    Boolean runFromFile( String path ) {
        Boolean cont = true;
        new File( path ).eachLine{ line ->
            if( !cont ) {
                return;
            }
            try {
                cont = execute( line );
            } catch( Throwable t ) {
                println t.toString();
            }
        }
        return cont;
    }

    /*Boolean execute( String line ) {
        Boolean cont = true;
        def cmds = line.split( ";" );
        cmds.each{
            cont = execute1( it );
            if( !cont ) {
                return cont;
            }
        }
        return cont;
    }*/

    Boolean execute( String command ) {
        Boolean cont = true;
        def cmdAndArgs = command.split( "\\s+" );
        if( 0 >= cmdAndArgs.length ) {
            return cont;
        }

        String cmd = cmdAndArgs[0];
        String[] args = cmdAndArgs[1..<cmdAndArgs.length] as String[];

        if( cmd == "" || cmd.startsWith("#") ) {
            /* Empty command or comment line */
        } else {
            cmd = cmd.toLowerCase();
            switch( cmd ) {
                case "quit":
                case "exit":
                    cont = false;
                    break;

                case "disconnect":
                    if( !disconnect ) {
                        println "Disconnect not supported";
                        return cont;
                    }
                    topLevel              = null;
                    serverAdmin           = null;
                    serverAdministration  = null;
                    serverStatus          = null;
                    spawnerInformation    = null;
                    spawnerAdministration = null;
                    serverInformation     = null;
                    disconnect();
                    break;
                case "connect":
                    if( !disconnect || !connect ) {
                        println "Connect not supported";
                        break;
                    }
                    topLevel              = null;
                    serverAdmin           = null;
                    serverAdministration  = null;
                    serverStatus          = null;
                    spawnerInformation    = null;
                    spawnerAdministration = null;
                    serverInformation     = null;
                    disconnect();
                    String uri = "";
                    if( args.length > 0 ) {
                        uri = args[0];
                    }
                    topLevel              = connect( uri );

                    serverAdmin           = IServerAdminHelper.narrow(topLevel);
                    serverAdministration  = IServerAdministrationHelper.narrow(topLevel);
                    serverStatus          = IServerStatusHelper.narrow(topLevel);
                    spawnerInformation    = ISpawnerInformationHelper.narrow(topLevel);
                    spawnerAdministration = ISpawnerAdministrationHelper.narrow(topLevel);
                    serverInformation     = IServerInformationHelper.narrow(topLevel);
                    break;
                case "?":
                case "help":
                case "commands":
                    commands();
                    break;

                case "clients":
                case "adminex.clients":
                    adminExClients( serverAdministration );
                    break;

                case "resetperf":
                case "adminex.resetperf":
                    adminExResetPerformanceCounters( serverAdministration );
                    break;

                case "stop":
                    if( serverAdministration && serverAdmin ) {
                        println "Ambiguous Stop command.  Admin and AdminEx both supported.";
                        println "Explicitly call Admin.Stop or AdminEx.Stop.";
                        break;
                    }
                    if( serverAdmin ) {
                        adminStop( serverAdmin );
                    } else if( serverAdministration ) {
                        adminExStop( serverAdministration );
                    } else {
                        println "The server does not support Stop."
                    }
                    break;
                case "admin.stop":
                    adminStop( serverAdmin );
                    break;
                case "adminex.stop":
                    adminExStop( serverAdministration );
                    break;

                case "deferredstop":
                    if( serverAdministration && serverAdmin ) {
                        println "Ambiguous DeferredStop command.  Admin and AdminEx both support this command.";
                        println "Explicitly call Admin.DeferredStop or AdminEx.DeferredStop.";
                        break;
                    }
                    if( serverAdmin ) {
                        adminDeferredStop( serverAdmin );
                    } else if( serverAdministration ) {
                        adminExDeferredStop( serverAdministration );
                    } else {
                        println "The server does not support DeferredStop."
                    }
                    break;
                case "admin.deferredstop":
                    adminDeferredStop( serverAdmin );
                    break;
                case "adminex.deferredstop":
                    adminExDeferredStop( serverAdministration );
                    break;

                case "pause":
                    if( serverAdministration && serverAdmin ) {
                        println "Ambiguous Pause command.  Admin and AdminEx both support this command.";
                        println "Explicitly call Admin.Pause or AdminEx.Pause.";
                        break;
                    }
                    if( serverAdmin ) {
                        adminPause( serverAdmin );
                    } else if( serverAdministration ) {
                        adminExPause( serverAdministration );
                    } else {
                        println "The server does not support Pause.";
                    }
                    break;
                case "admin.pause":
                    adminPause( serverAdmin );
                    break;
                case "adminex.pause":
                    adminExPause( serverAdministration );
                    break;

                case "continue":
                    if( serverAdministration && serverAdmin ) {
                        println "Ambiguous Continue command.  Admin and AdminEx both support this command.";
                        println "Explicitly call Admin.Continue or AdminEx.Continue.";
                        break;
                    }
                    if( serverAdmin ) {
                        adminContinue( serverAdmin );
                    } else if( serverAdministration ) {
                        adminExContinue( serverAdministration );
                    } else {
                        println "The server does not support Continue.";
                    }
                    break;
                case "admin.continue":
                    adminContinue( serverAdmin );
                    break;
                case "adminex.continue":
                    adminExContinue( serverAdministration );
                    break;

                case "flushcache":
                case "adminex.flushcache":
                    adminExFlushCache( serverAdministration );
                    break;

                case "uniqueid":
                    if( serverStatus && serverAdmin ) {
                        println "Ambiguous Continue command.  Status and Admin both support this command.";
                        println "Explicitly call Status.UniqueID or Admin.UniqueID.";
                        break;
                    }
                    if( serverAdmin ) {
                        adminUniqueID( serverAdmin );
                    } else if( serverStatus ) {
                        statusUniqueID( serverStatus );
                    } else {
                        println "The server does not support UniqueID.";
                    }
                    break;
                case "admin.uniqueid":
                    adminUniqueID( serverAdmin );
                    break;
                case "status.uniqueid":
                    statusUniqueID( serverStatus );
                    break;

                case "clsids":
                case "classids":
                case "status.classids":
                    statusClsids( serverStatus );
                    break;

                case "dnsname":
                case "status.dnsname":
                    statusDnsName( serverStatus );
                    break;

                case "state":
                case "status.state":
                    statusState( serverStatus );
                    break;

                case "info":
                    if( serverStatus && serverAdmin ) {
                        println "Ambiguous Continue command.  Status and Admin both support this command.";
                        println "Explicitly call Status.Info or Admin.Info.";
                        break;
                    }
                    if( serverAdmin ) {
                        adminInfo( serverAdmin );
                    } else if( serverStatus ) {
                        statusInfo( serverStatus );
                    } else {
                        println "The server does not support Info.";
                    }
                    break;
                case "admin.info":
                    adminInfo( serverAdmin );
                    break;
                case "status.info":
                    statusInfo( serverStatus );
                    break;

                case "servertime":
                case "status.servertime":
                    statusServerTime( serverStatus );
                    break;

                case "name":
                case "admin.name":
                    adminName( serverAdmin );
                    break;

                case "start":
                case "admin.start":
                    adminStart( serverAdmin );
                    break;

                case "perf":
                case "admin.perf":
                    adminPerf( serverAdmin );
                    break;

                case "controlinfo":
                case "admin.controlinfo":
                    adminControlInfo( serverAdmin );
                    break;

                case "categories":
                case "info.categories":
                    infoCategories( serverInformation );
                    break;

                case "attrs":
                case "info.attrs":
                    infoAttrs( serverInformation, args );
                    break;

                case "attr":
                case "info.attr":
                    infoAttr( serverInformation, args );
                    break;

                case "sessions":
                    if( serverSessions && sessionAdmin ) {
                        println "Ambiguous Sessions command.  Sessions and SesAdmin both support this command.";
                        println "Explicitly call Sessions.Sessions or SesAdmin.Sessions.";
                        break;
                    }
                    if( serverSessions ) {
                        sessionsSessions( serverSessions, args );
                    } else if( sessionAdmin ) {
                        sesadminSessions( sessionAdmin, args );
                    } else {
                        println "The server does not support Sessions.";
                    }
                    break;

                case "sessions.sessions":
                    sessionsSessions( serverSessions, args );
                    break;

                case "sesadmin.sessions":
                    sesadminSessions( sessionAdmin, args );
                    break;

                case "stopsession":
                    if( serverSessions && sessionAdmin ) {
                        println "Ambiguous StopSession command.  Sessions and SesAdmin both support this command.";
                        println "Explicitly call Sessions.StopSession or SesAdmin.StopSession.";
                        break;
                    }
                    if( serverSessions ) {
                        sessionsStopSession( serverSessions, args );
                    } else if( sessionAdmin ) {
                        sesadminStopSession( sessionAdmin, args );
                    } else {
                        println "The server does not support StopSession.";
                    }
                    break;

                case "sessions.stopsession":
                    sessionsStopSession( serverSessions, args );
                    break;

                case "sesadmin.stopsession":
                    sesadminStopSession( sessionAdmin, args );
                    break;

                case "defined":
                case "spawner.defined":
                    spawnerDefined( spawnerInformation );
                    break;

                case "spawned":
                case "spawner.spawned":
                    spawnerSpawned( spawnerInformation );
                    break;

                case "abandoned":
                case "spawner.abandoned":
                    spawnerAbandoned( spawnerInformation );
                    break;

                case "refresh":
                case "spawner.refresh":
                    spawnerRefresh( spawnerAdministration );
                    break;

                case "stopspawned":
                case "spawner.stopspawned":
                    spawnerStopSpawned( spawnerAdministration, args );
                    break;

                default:
                    println "Unknown command $cmd.";
            }
        }
        return cont;
    }

    void commands() {
        println "Commands             | Description";
        println "------------------------------------------------------------";
        println "Help                 | Print the list of available commands.";
        if( connect && disconnect ) {
            println "Connect              | Connect to an IOM server.";
            println "   [URI]             |    The URI of the server.";
        }
        if( disconnect ) {
            println "Disconnect           | Disconnect from the server.";
        }
        if( serverAdmin ) {
            println "Admin.UniqueID       | Print the per-execution instance identifier.";
            println "Admin.Name           | Print the name of the server.";
            println "Admin.Start          | Start the server.";
            println "Admin.Stop           | Stop the server.";
            println "Admin.DeferredStop   | Stop the server when all current client work is complete.";
            println "Admin.Pause          | Pause the server so that it does not accept any new work.";
            println "Admin.Continue       | Resume server operation.";
            //println "Admin.ResetServerPerformance      This operation is not implemented.";
            println "Admin.Perf           | Print information about server status and performance.";
            println "Admin.Info           | Print information about the server.";
            println "Admin.ControlInfo    | Print information about what functionality is supported. ";
        }
        if( serverAdministration ) {
            println "AdminEx.Clients      | Print a list of the clients currently connected to this server.";
            println "AdminEx.ResetPerf    | Reset the accumulating counters to their initial values.";
            println "AdminEx.Stop         | Stop the server.";
            println "AdminEx.DeferredStop | Stop the server when all current client work is complete.";
            println "AdminEx.Pause        | Pause the server so that it does not accept any new work.";
            println "AdminEx.Continue     | Resume server operation.";
            println "AdminEx.FlushCache   | Flush the authorization cache.";
        }
        if( serverStatus ) {
            println "Status.UniqueID      | Print the per-execution instance identifier.";
            println "Status.ClassIDs      | Print the classID's supported by the server.";
            println "Status.DNSName       | Print the host on which the server is running.";
            println "Status.State         | Print the current state of the server.";
            println "Status.Info          | Print information about the server.";
            println "Status.ServerTime    | Print the GMT and local times of the server.";
        }
        if( serverSessions ) {
            println "Sessions.Sessions    | Print a list of active sessions on the server.";
            println "   [user filter]     |    Only print sessions owned by this user.";
            println "Sessions.StopSession | End the given session.";
            println "   SessionID         |    The UUID of the session to stop.";
        }
        if( sessionAdmin ) {
            println "SesAdmin.Sessions    | Print a list of active sessions on the server.";
            println "SesAdmin.StopSession | Stop the given session.";
            println "   SessionID         |    The UUID of the session to stop.";
        }
        if( serverInformation ) {
            println "Info.Categories      | Print a list of the categories available to the currently connected client.";
            println "Info.Attrs           | Print a list of attributes and their values.";
            println "   [/verbose]        |    Print extended information about the attributes.";
            println "   [CategoryName]    |    Print only attributes in the given category.";
            println "Info.Attr            | Print or set one attribute and it's value.";
            println "   [/verbose]        |    Print extended information about the attribute.";
            println "   [/set]            |    Set the value for the given attribute.  Requires [Value] argument.";
            println "   Category.Attribute|    Print the value for the given attribute in the given category.";
            println "   [Value]           |    Required value if /set used.";
        }
        if( spawnerInformation ) {
            println "Spawner.Defined      | Print a list of defined servers that can be spawned.";
            println "Spawner.Spawned      | Print a list of servers spanwed.";
            println "Spawner.Abandoned    | Print a list of servers that have been abandoned.";
        }
        if( spawnerAdministration ) {
            println "Spawner.Refresh      | Refresh the spawner.";
            println "Spawner.StopSpawned  | Stop a spanwed server.";
            println "   [UUID]            |    The UUID of the spawned server to stop.";
        }
    }

    /******************************************************************
    * IServerAdmin methods                                            *
    ******************************************************************/
    static void adminStop( IServerAdmin serverAdmin ) {
        serverAdmin.ServerAdminStop();
        println "OK.";
    }

    static void adminDeferredStop( IServerAdmin serverAdmin ) {
        serverAdmin.ServerAdminDeferredStop();
        println "OK.";
    }

    static void adminPause( IServerAdmin serverAdmin ) {
        serverAdmin.ServerAdminPause();
        println "OK.";
    }

    static void adminContinue( IServerAdmin serverAdmin ) {
        serverAdmin.ServerAdminContinue();
        println "OK.";
    }

    static void adminUniqueID( IServerAdmin serverAdmin ) {
        println serverAdmin.ServerAdminUniqueID().toUpperCase();
    }

    static void adminInfo( IServerAdmin serverAdmin ) {
        StringSeqHolder softwareInfoHolder = new StringSeqHolder();
        VariableArray2dOfStringHolder hardwareInfoHolder = new VariableArray2dOfStringHolder();
        serverAdmin.ServerAdminGetInfo( softwareInfoHolder, hardwareInfoHolder );
        String[] softwareInfo = softwareInfoHolder.value;
        String[][] hardwareInfo = hardwareInfoHolder.value;
        println "SAS version        : " + softwareInfo[0];
        println "SAS version long   : " + softwareInfo[1];
        println "OS name            : " + softwareInfo[2];
        println "OS family          : " + softwareInfo[3];
        println "OS version         : " + softwareInfo[4];
        println "Client user ID     : " + softwareInfo[5];
        for( int i = 0; i < hardwareInfo.length; ++i ) {
            println "CPU" + i + " model name    : " + hardwareInfo[i][0];
            println "CPU" + i + " model number  : " + hardwareInfo[i][1];
            println "CPU" + i + " serial number : " + hardwareInfo[i][2];
        }
    }

    static void adminName( IServerAdmin serverAdmin ) {
        println serverAdmin.ServerAdminName();
    }

    static void adminStart( IServerAdmin serverAdmin ) {
        serverAdmin.ServerAdminStart();
    }

    static void adminPerf( IServerAdmin serverAdmin ) {
        DoubleSeqHolder valuesHolder = new DoubleSeqHolder();
        serverAdmin.GetServerPerformance( valuesHolder );
        double[] values = valuesHolder.value;
        println "Total real execution time : " + values[0];
        println "Total system CPU time     : " + values[1];
        println "Total user CPU time       : " + values[2];
        println "Total calls               : " + values[3];
        println "Elapsed object time       : " + values[4];
        println "Elapsed server time       : " + values[5];
        println "Status                    : " + IOMHelper.ServerStateToString( ServerState.from_int( (int)values[6] ) );
    }

    static void adminControlInfo( IServerAdmin serverAdmin ) {
        BooleanHolder supportsStart = new BooleanHolder();
        BooleanHolder supportsStop = new BooleanHolder();
        BooleanHolder supportsDStop = new BooleanHolder();
        BooleanHolder supportsPause  = new BooleanHolder();
        BooleanHolder supportsContinue  = new BooleanHolder();
        serverAdmin.ServerAdminControlInfo (
            supportsStart, supportsStop, supportsDStop, supportsPause, supportsContinue );

        println "Supports start         : " + supportsStart.value;
        println "Supports stop          : " + supportsStop.value;
        println "Supports deferred stop : " + supportsDStop.value;
        println "Supports pause         : " + supportsPause.value;
        println "Supports continue      : " + supportsContinue.value;
    }

    /******************************************************************
    * IServerAdministration methods                                   *
    ******************************************************************/
    static void adminExClients( IServerAdministration serverAdministration ) {
        StringSeqHolder clientsHolder = new StringSeqHolder();
        StringSeqHolder clientHostsHolder = new StringSeqHolder();
        DateTimeSeqHolder enterTimesHolder = new DateTimeSeqHolder();
        serverAdministration.ListClients( clientsHolder, clientHostsHolder, enterTimesHolder );
        String[] clients = clientsHolder.value;
        String[] clientHosts = clientHostsHolder.value;
        long[] enterTimes = enterTimesHolder.value;
        Date d = null;
        SimpleDateFormat s   = new SimpleDateFormat("ddMMMyyyy hh:mm:ss,SSS a z");
        s.setTimeZone(TimeZone.getDefault());//TimeZone("GMT"));
        for( int i = 0; i < clients.length; ++i ) {
            d = DateConverter.corbaToJavaGMT( enterTimes[i] );
            println clients[i];
            println "\tHost       : " + clientHosts[i];
            println "\tEnter time : " + s.format(d);
        }

    }

    static void adminExResetPerformanceCounters( IServerAdministration serverAdministration ) {
        serverAdministration.ResetPerformanceCounters();
        println "OK.";
    }

    static void adminExStop( IServerAdministration serverAdministration ) {
        serverAdministration.StopServer();
        println "OK.";
    }

    static void adminExDeferredStop( IServerAdministration serverAdministration ) {
        serverAdministration.DeferredStopServer();
        println "OK.";
    }

    static void adminExPause( IServerAdministration serverAdministration ) {
        serverAdministration.PauseServer();
        println "OK.";
    }

    static void adminExContinue( IServerAdministration serverAdministration ) {
        serverAdministration.ContinueServer();
        println "OK.";
    }

    static void adminExFlushCache( IServerAdministration serverAdministration ) {
        serverAdministration.CacheFlush();
        println "OK.";
    }

    /******************************************************************
    * IServerStatus methods                                           *
    ******************************************************************/
    static void statusUniqueID( IServerStatus serverStatus ) {
        UUID u = serverStatus.ServerStatusUniqueID();
        println UUIDConverter.uuidToString( u ).toUpperCase();
    }

    static void statusClsids( IServerStatus serverStatus ) {
        UUIDSeqHolder uuidsHolder = new UUIDSeqHolder();
        serverStatus.ServerStatusServerClassIDs( uuidsHolder );
        UUID[] uuids = uuidsHolder.value;
        uuids.each{
            println UUIDConverter.uuidToString( it ).toUpperCase();
        }
    }

    static void statusDnsName( IServerStatus serverStatus ) {
        println serverStatus.ServerStatusDNSName();
    }

    static void statusState( IServerStatus serverStatus ) {
        println IOMHelper.ServerStateToString( serverStatus.ServerStatusState() );
    }

    static void statusInfo( IServerStatus serverStatus ) {
        StringSeqHolder infoHolder = new StringSeqHolder();
        serverStatus.ServerStatusGetInfo( infoHolder );
        String[] info = infoHolder.value;
        println "SAS version      : " + info[0];
        println "SAS version long : " + info[1];
        println "OS name          : " + info[2];
        println "OS family        : " + info[3];
        println "OS version       : " + info[4];
        println "Client user ID   : " + info[5];
    }

    static void statusServerTime( IServerStatus serverStatus ) {
        DateTimeHolder gmtHolder = new DateTimeHolder();
        DateTimeHolder localHolder = new DateTimeHolder();
        serverStatus.ServerStatusServerTime( gmtHolder, localHolder );
        Date gmt = null;
        Date local = null;

        SimpleDateFormat s = new SimpleDateFormat("ddMMMyyyy HH:mm:ss,SSS z");
        s.setTimeZone(TimeZone.getTimeZone("GMT"));
        gmt = DateConverter.corbaToJavaGMT( gmtHolder.value );
        print s.format(gmt) + " (";

        s = new SimpleDateFormat("ddMMMyyyy hh:mm:ss,SSS a");
        s.setTimeZone(TimeZone.getTimeZone("GMT"));
        local = DateConverter.corbaToJavaGMT( localHolder.value );
        TimeZone defaultTimeZone = TimeZone.getDefault();
        println  s.format(local) + " " + defaultTimeZone.getDisplayName(defaultTimeZone.useDaylightTime(), TimeZone.SHORT) + ")";
    }

    /******************************************************************
    * IServerInformation methods                                      *
    ******************************************************************/
    static void infoCategories( IServerInformation serverInformation ) {
        serverInformation.ListCategories().each{
            println it;
        }
    }

    static void infoAttrs( IServerInformation serverInformation, String[] attrs ) {
        String category = "";
        Boolean verbose = false;
        if( attrs.length > 0 ) {
            category = attrs[0];
        }
        if( category.toLowerCase().startsWith( "/verbose" ) ) {
            verbose = true;
            category = "";
            if( attrs.length > 1 ) {
                category = attrs[1];
            }
        }
        def categories = [];
        if( category != "" ) {
            categories.add( category );
        } else {
            categories = serverInformation.ListCategories();
        }

        categories.each {
            AnySeqHolder attributes = new AnySeqHolder()
            String[] names = [];   // The first element of the returned attributes array
            String[] descs = [];   // The second element of the returned attributes array
            Boolean[] updates = [];// The third element of the returned attributes array
            String[] types = [];   // The fourth element of the returned attributes array
            Any[] values = [];     // The fifth element of the returned attributes array
            def filteredList = serverInformation.UseCategory( it, "" );
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
                String name = names[i];
                if( name == "" ) {
                    name = "Root";
                }
                if( verbose ) {
                    println it + "." + name;
                    println "\tDescription : " + descs[i];
                    println "\tUpdatable   : " + updates[i];
                    println "\tType        : " + types[i];
                    println "\tValue       : " + IOMHelper.AnyToString( values[i] );
                } else {
                    println it + "." + name + " : " + IOMHelper.AnyToString( values[i] );
                }
            }
        }
    }

    static void infoAttr( IServerInformation serverInformation, String[] args ) {
        String category = "";
        String attribute = "";
        String value = null;
        Boolean verbose = false;
        Boolean set = false;
        if( args.length > 0 ) {
            category = args[0];
        }
        if( category.toLowerCase().startsWith( "/verbose" ) ) {
            verbose = true;
        } else if( category.toLowerCase().startsWith( "/set" ) ) {
            set = true;
        }
        if( verbose || set ) {
            category = "";
            if( args.length > 1 ) {
                category = args[1];
            }
        }
        if( category == "" ) {
            println "The Info.Attr command requires a Category.Attribute argument.";
            return;
        }
        if( args.length > 2 ) {
            value = args[2];
        }
        if( set && !value ) {
            println "The Info.Attr /set command requires a value argument.";
            return;
        }
        int idx = category.indexOf( "." );
        if( idx < 0 ) {
            println "The Info.Attr command requres a Category.Attribute argument.";
            return;
        }
        attribute = category.substring( idx+1 );
        if( attribute == "Root" ) {
            attribute = "";
        }
        category = category.substring( 0, idx );

        def filteredList = serverInformation.UseCategory( category, "" );
        AnySeqHolder attributes = new AnySeqHolder()
        filteredList.GetAttribute( attribute, "", attributes );
        if( !attributes.value ) {
            println "No attribute named $category.$attribute found";
            return;
        }

        String name = attributes.value[0].extract_string();
        String description = attributes.value[1].extract_string();
        Boolean updatable = attributes.value[2].extract_boolean();
        String type = attributes.value[3].extract_string();
        Any serverValue = attributes.value[4];

        if( set ) {
            filteredList.SetValue( attribute, IOMHelper.StringToAny( filteredList._orb(), type, value ) );
            filteredList.GetAttribute( attribute, "", attributes );
            name = attributes.value[0].extract_string();
            description = attributes.value[1].extract_string();
            updatable = attributes.value[2].extract_boolean();
            type = attributes.value[3].extract_string();
            serverValue = attributes.value[4];
        }
        if( name == "" ) {
            name = "Root";
        }
        if( verbose ) {
            println category + "." + name;
            println "\tDescription : " + description;
            println "\tUpdatable   : " + updatable;
            println "\tType        : " + type;
            println "\tValue       : " + IOMHelper.AnyToString( serverValue );
        } else {
            println category + "." + name + " : " + IOMHelper.AnyToString( serverValue );
        }
    }

    /******************************************************************
    * IServerSessions methods                                         *
    ******************************************************************/
    static void sessionsSessions( IServerSessions serverSessions, String[] args ) {
        String userFilter = "";
        UUIDSeqHolder sessionIDs = new UUIDSeqHolder();
        StringSeqHolder owners = new StringSeqHolder();
        DoubleSeqHolder inactiveTimes = new DoubleSeqHolder();
        if( args.length > 0 ) {
            userFilter = args[0];
        }
        serverSessions.SessionList( userFilter, sessionIDs, owners, inactiveTimes );
        for( int i = 0; i < sessionIDs.value.length; ++i ) {
            println owners.value[i];
            println "\tSession ID    : " + UUIDConverter.uuidToString( sessionIDs.value[i] ).toUpperCase();
            println "\tInactive time : " + inactiveTimes.value[i];
        }
    }

    static void sessionsStopSession( IServerSessions serverSessions, String[] args ) {
        String sessionid = "";
        if( args.length > 0 ) {
            sessionid = args[0];
        }
        if( sessionid == "" ) {
            println "The Sessions.StopSession command requires a SessionID argument.";
            return;
        }
        UUID uuid = UUIDConverter.stringToUUID( sessionid );
        ISessionAdministration sessionAdministration = ISessionAdministrationHelper.narrow( serverSessions.UseSession( uuid ) );
        sessionAdministration.EndSession();
    }

    /******************************************************************
    * ISessionAdmin methods                                           *
    ******************************************************************/
    static void sesadminSessions( ISessionAdmin sessionAdmin, String[] args ) {
        String userFilter = "";
        UUIDSeqHolder sessionIDs = new UUIDSeqHolder();
        StringSeqHolder owners = new StringSeqHolder();
        LongSeqHolder secsInact = new LongSeqHolder();
        LongSeqHolder nMembers = new LongSeqHolder();
        if( args.length > 0 ) {
            userFilter = args[0];
        }

        sessionAdmin.ListSessions( userFilter, sessionIDs, owners, secsInact, nMembers );
        for( int i = 0; i < sessionIDs.value.length; ++i ) {
            println owners.value[i];
            println "\tSession ID    : " + UUIDConverter.uuidToString( sessionIDs.value[i] ).toUpperCase();
            println "\tInactive time : " + secsInact.value[i];
            println "\tMembers       : " + nMembers.value[i];
        }
    }

    static void sesadminStopSession( ISessionAdmin sessionAdmin, String[] args ) {
        String sessionid = "";
        if( args.length > 0 ) {
            sessionid = args[0];
        }
        if( sessionid == "" ) {
            println "The Sessions.StopSession command requires a SessionID argument.";
            return;
        }
        UUID uuid = UUIDConverter.stringToUUID( sessionid );
        sessionAdmin.CloseSession( uuid );
    }

    /******************************************************************
    * ISpawnerInformation methods                                     *
    ******************************************************************/
    static void spawnerDefined( ISpawnerInformation spawnerInformation ) {
        StringSeqHolder logicalServersHolder = new StringSeqHolder();
        StringSeqHolder serverComponentsHolder = new StringSeqHolder();
        UUIDSeqHolder serverClassesHolder = new UUIDSeqHolder();
        spawnerInformation.ListDefinedServers( "", logicalServersHolder, serverComponentsHolder, serverClassesHolder );
        String[] logicalServers = logicalServersHolder.value;
        String[] serverComponents = serverComponentsHolder.value;
        UUID[] serverClasses = serverClassesHolder.value;
        for( int i = 0; i < logicalServers.length; ++i ) {
            println logicalServers[i];
            println "\tComponent : " + serverComponents[i];
            println "\tClass     : " + UUIDConverter.uuidToString( serverClasses[i] ).toUpperCase();
        }
    }

    static void spawnerSpawned( ISpawnerInformation spawnerInformation ) {
        StringSeqHolder logicalServersHolder = new StringSeqHolder();
        StringSeqHolder serverComponentsHolder = new StringSeqHolder();
        UUIDSeqHolder serverClassesHolder = new UUIDSeqHolder();
        StringSeqHolder processOwnersHolder = new StringSeqHolder();
        UUIDSeqHolder serverIDsHolder = new UUIDSeqHolder();
        spawnerInformation.ListLaunchedServers( "", logicalServersHolder, serverComponentsHolder, serverClassesHolder, processOwnersHolder, serverIDsHolder );
        String[] logicalServers = logicalServersHolder.value;
        String[] serverComponents = serverComponentsHolder.value;
        UUID[] serverClasses = serverClassesHolder.value;
        String[] processOwners = processOwnersHolder.value;
        UUID[] serverIDs = serverIDsHolder.value;
        for( int i = 0; i < logicalServers.length; ++i ) {
            println logicalServers[i];
            println "\tComponent : " + serverComponents[i];
            println "\tClass     : " + UUIDConverter.uuidToString( serverClasses[i] ).toUpperCase();
            println "\tOwner     : " + processOwners[i];
            println "\tServer ID : " + UUIDConverter.uuidToString( serverIDs[i] ).toUpperCase();
        }
    }

    static void spawnerAbandoned( ISpawnerInformation spawnerInformation ) {
        StringSeqHolder logicalServersHolder = new StringSeqHolder();
        StringSeqHolder serverComponentsHolder = new StringSeqHolder();
        UUIDSeqHolder serverClassesHolder = new UUIDSeqHolder();
        StringSeqHolder processOwnersHolder = new StringSeqHolder();
        UUIDSeqHolder serverIDsHolder = new UUIDSeqHolder();
        spawnerInformation.ListAbandonedServers( "", logicalServersHolder, serverComponentsHolder, serverClassesHolder, processOwnersHolder, serverIDsHolder );
        String[] logicalServers = logicalServersHolder.value;
        String[] serverComponents = serverComponentsHolder.value;
        UUID[] serverClasses = serverClassesHolder.value;
        String[] processOwners = processOwnersHolder.value;
        UUID[] serverIDs = serverIDsHolder.value;
        for( int i = 0; i < logicalServers.length; ++i ) {
            println logicalServers[i];
            println "\tComponent : " + serverComponents[i];
            println "\tClass     : " + UUIDConverter.uuidToString( serverClasses[i] ).toUpperCase();
            println "\tOwner     : " + processOwners[i];
            println "\tServer ID : " + UUIDConverter.uuidToString( serverIDs[i] ).toUpperCase();
        }
    }

    /******************************************************************
    * ISpawnerAdministration methods                                  *
    ******************************************************************/
    static void spawnerRefresh( ISpawnerAdministration spawnerAdministration ) {
        spawnerAdministration.Refresh();
        println "OK.";
    }

    static void spawnerStopSpawned( ISpawnerAdministration spawnerAdministration, String[] args ) {
        String spawned = "";
        if( args.length > 0 ) {
            spawned = args[0];
        }
        if( spawned == "" ) {
            println "The Spawner.StopSpawned command requires a ServerID argument.";
            return;
        }
        UUID uuid = UUIDConverter.stringToUUID( spawned );
        spawnerAdministration.KillSpawnedServer( uuid );
        println "OK.";
    }
}

