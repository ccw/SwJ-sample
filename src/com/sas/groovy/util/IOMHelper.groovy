package com.sas.groovy.util;

import org.omg.CORBA.Any
import org.omg.CORBA.TCKind
import org.omg.CORBA.ORB;
import org.omg.CORBA.StringHolder;
import com.sas.iom.orb.SASURI;
import com.sas.iom.SASIOMDefs.UUID;
import com.sas.iom.SASIOMDefs.UUIDHelper;
import com.sas.iom.SASIOMDefs.DateTimeHelper;
import com.sas.iom.SASIOMDefs.AnySeqHolder;
import com.sas.iom.SASIOMDefs.AnySeqHelper;
import com.sas.iom.SASIOMDefs.BooleanSeqHelper;
import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.iom.SASIOMDefs.UUIDSeqHolder;
import com.sas.iom.SASIOMDefs.DateTimeHolder;
import com.sas.iom.SASIOMCommon.IServerStatus;
import com.sas.iom.SASIOMCommon.ServerState;
import com.sas.iom.SAS.ILanguageService;
import com.sas.iom.SAS.ILanguageServicePackage.CarriageControlSeqHolder;
import com.sas.iom.SAS.ILanguageServicePackage.LineTypeSeqHolder;
import com.sas.net.util.DateConverter;
import com.sas.meta.SASOMI.IOMI;

import java.text.SimpleDateFormat;

public class IOMHelper {

    /*
     * The ILanguageServiceLogToString method takes an ILanguageService interface and
     * returns a string representation of the log it contains.  Note that this can be
     * memory intensive if the log is large.
     */
    static String ILanguageServiceLogToString( ILanguageService iLang )
    {
        StringBuffer                buffer              = new StringBuffer();
        int                         numLinesRequested   = 999;
        CarriageControlSeqHolder    carriageControls    = new CarriageControlSeqHolder();
        LineTypeSeqHolder           lineTypes           = new LineTypeSeqHolder();
        StringSeqHolder             logLinesHolder      = new StringSeqHolder();
        String[]                    logLines            = [];

        // No do-while support in groovy
        iLang.FlushLogLines( numLinesRequested, carriageControls, lineTypes, logLinesHolder );
        logLines = logLinesHolder.value;
        while( logLines.length > 0 ) {
            logLines.each{ buffer.append( it + "\n" ) };
            if( logLines.length != numLinesRequested ) {
                break;
            }
            iLang.FlushLogLines( numLinesRequested, carriageControls, lineTypes, logLinesHolder );
            logLines = logLinesHolder.value;
        }

        return buffer.toString();
    }

    /*
     * The ILanguageServiceListToString method takes an ILanguageService interface and
     * returns a string representation of the list it contains.  Note that this can be
     * memory intensive if the list is large.
     */
    static String ILanguageServiceListToString( ILanguageService iLang )
    {
        StringBuffer                buffer              = new StringBuffer();
        int                         numLinesRequested   = 999;
        CarriageControlSeqHolder    carriageControls    = new CarriageControlSeqHolder();
        LineTypeSeqHolder           lineTypes           = new LineTypeSeqHolder();
        StringSeqHolder             listLinesHolder     = new StringSeqHolder();
        String[]                    listLines           = [];

        // No do-while support in groovy
        iLang.FlushListLines( numLinesRequested, carriageControls, lineTypes, listLinesHolder );
        listLines = listLinesHolder.value;
        while( listLines.length > 0 ) {
            listLines.each{ buffer.append( it + "\n" ) };
            if( listLines.length != numLinesRequested ) {
                break;
            }
            iLang.FlushListLines( numLinesRequested, carriageControls, lineTypes, listLinesHolder );
            listLines = listLinesHolder.value;
        }

        return buffer.toString();
    }

    /*
     * The IServerStatusToString method takes an IServerStatus interface and
     * returns a string representation of the information it contains.
     */
    static String IServerStatusToString( IServerStatus iStatus )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "Unique ID       : " + UUIDToString( iStatus.ServerStatusUniqueID() ) + "\n" );
        buffer.append( "DNS name        : " + iStatus.ServerStatusDNSName() + "\n" );
        buffer.append( "State           : " + ServerStateToString( iStatus.ServerStatusState() ) + "\n" );

        UUIDSeqHolder UUIDs = new UUIDSeqHolder();
        iStatus.ServerStatusServerClassIDs( UUIDs );
        UUID[] UUIDvalues = UUIDs.value;
        buffer.append( "Class IDs       : " );
        UUIDvalues.each{ buffer.append( UUIDToString( it ) + " " ) }
        buffer.append( "\n" );

        StringSeqHolder softwareInfo = new StringSeqHolder();
        iStatus.ServerStatusGetInfo( softwareInfo );
        String[] info = softwareInfo.value;
        buffer.append( "SAS version     : " + info[0] + "\n" );
        buffer.append( "SAS version long: " + info[1] + "\n" );
        buffer.append( "OS name         : " + info[2] + "\n" );
        buffer.append( "OS family       : " + info[3] + "\n" );
        buffer.append( "OS version      : " + info[4] + "\n" );
        buffer.append( "Client user ID  : " + info[5] + "\n" );

        DateTimeHolder gmtHolder = new DateTimeHolder();
        DateTimeHolder localHolder = new DateTimeHolder();
        iStatus.ServerStatusServerTime( gmtHolder, localHolder );
        Date gmt = null;
        Date local = null;

        SimpleDateFormat s = new SimpleDateFormat("ddMMMyyyy HH:mm:ss,SSS z");
        s.setTimeZone(TimeZone.getTimeZone("GMT"));
        gmt = DateConverter.corbaToJavaGMT( gmtHolder.value );
        buffer.append( "Server time     : " + s.format(gmt) + " (" );

        s = new SimpleDateFormat("ddMMMyyyy hh:mm:ss,SSS a");
        s.setTimeZone(TimeZone.getTimeZone("GMT"));
        local = DateConverter.corbaToJavaGMT( localHolder.value );
        TimeZone defaultTimeZone = TimeZone.getDefault();
        buffer.append( s.format(local) + " " + defaultTimeZone.getDisplayName(defaultTimeZone.useDaylightTime(), TimeZone.SHORT) + ")" );

        return buffer.toString();
    }

    /*
     * The ServerStateToString method takes a ServerState and returns
     * a string representation of that ServerState
     */
    static String ServerStateToString( ServerState state ) {
        switch( state ) {
            case ServerState.ServerStateNotStarted:
                return "Not started";
            case ServerState.ServerStateStartPending:
                return "Start pending";
            case ServerState.ServerStateRunning:
                return "Running";
            case ServerState.ServerStatePausePending:
                return "Pause pending";
            case ServerState.ServerStatePaused:
                return "Paused";
            case ServerState.ServerStateContinuePending:
                return "Continue pending";
            case ServerState.ServerStateDeferredStop:
                return "Deferred stop";
            case ServerState.ServerStateStopPending:
                return "Stop pending";
            case ServerState.ServerStateStopped:
                return "Stopped";
            default:
                return "Unknown state";
        }
    }

    /*
     * The UUIDToString method takes a UUID and retuns a String
     * representation of that UUID.
     */
    static String UUIDToString( UUID u ) {
        return String.format( "%08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X", u.Data1, u.Data2, u.Data3,
            u.Data4[0], u.Data4[1], u.Data4[2], u.Data4[3], u.Data4[4], u.Data4[5], u.Data4[6], u.Data4[7] );
    }

    /*
     * The AnyToString method takes an Any and retuns a String
     * representation of that Any.
     */
    static String AnyToString( Any a ) {
        switch( a.type().kind().value() ) {
            case TCKind._tk_short:
                return a.extract_short().toString();
                break;
            case TCKind._tk_long:
                return a.extract_long().toString();
                break;
            case TCKind._tk_longlong:
                return a.extract_longlong().toString();
                break;
            case TCKind._tk_double:
                return a.extract_double().toString();
                break;
            case TCKind._tk_boolean:
                return a.extract_boolean().toString();
                break;
            case TCKind._tk_string:
                return a.extract_string().toString();
                break;
            case UUIDHelper.type().kind().value():
                UUID u = UUIDHelper.extract( a );
                return UUIDToString( u );
                break;
            case DateTimeHelper.type().kind().value():
                return DateTimeHelper.extract( a ).toString();
                break;
            default:
                return "unknown type: " + a.type() + "(" + a.type().kind().value() + ")";
                break;
        }
    }

    /*
     * The ExtractAny method takes an Any and retuns it's inner value
     * as the type that it is.
     */
    static ExtractAny( Any a ) {
        switch( a.type().kind().value() ) {
            case TCKind._tk_short:
                return a.extract_short();
                break;
            case TCKind._tk_long:
                return a.extract_long();
                break;
            case TCKind._tk_longlong:
                return a.extract_longlong();
                break;
            case TCKind._tk_double:
                return a.extract_double();
                break;
            case TCKind._tk_boolean:
                return a.extract_boolean();
                break;
            case TCKind._tk_string:
                return a.extract_string();
                break;
            case UUIDHelper.type().kind().value():
                return UUIDHelper.extract( a );
                break;
            case DateTimeHelper.type().kind().value():
                return DateTimeHelper.extract( a );
                break;
            default:
                return "unknown type: " + a.type() + "(" + a.type().kind().value() + ")";
                break;
        }
    }

    /*
     * The StringToAny method takes a type (as returned from IFilteredList)
     * and a string value and returns an Any of the appropriate type with
     * the given value.
     */
    static Any StringToAny( ORB orb, String type, String value ) {
        Any any = orb.create_any();
        switch( type ) {
            case "Int32":
                any.insert_long( Integer.parseInt( value ) );
                break;
            case "Double":
                any.insert_double( Double.parseDouble( value ) );
                break;
            case "Boolean":
                any.insert_boolean( Boolean.parseBoolean( value ) );
                break;
            case "UUID":
                break;
            case "DateTime":
                break;
            case "String":
            case "Level":
                any.insert_string( value );
                break;
        }
        return any;
    }

    /*
     * The PromptForPassword method takes a prompt and returns a
     * password read from stdin.  It starts a masking thread that
     * hides the characters typed.  Note that if you are using
     * Java 1.6 the System.console() object has a readPassword
     * method that probably does something similar.
     */
    static String PromptForPassword( String prompt ) {
        print prompt + " ";
        volatile boolean stopMasking = false;

        def maskingClosure = {
            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority( Thread.MAX_PRIORITY );
            try {
                while(!stopMasking) {
                    System.out.print( "\010*" );
                    Thread.currentThread().sleep( 1 );
                }
            } finally {
                Thread.currentThread().setPriority( priority );
            }
        };

        Thread maskingThread = Thread.start( maskingClosure );

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String password = input.readLine();

        stopMasking = true;
        maskingThread.join();

        return password;
    }

    /*
     * The ReposIdForName method takes an IOMI interface and a repository name
     * and returns the id of that repository if one could be found.  Returns
     * null if a repository with the given name could not be found.
     */
    static String ReposIdForName( IOMI iomi, String reposName ) {
        StringHolder response = new StringHolder();
        iomi.GetRepositories( response, 0, "" );
        Node repositories = new XmlParser().parseText( response.value );
        Node repository = repositories.Repository.find{ it.@Name == reposName }
        if( repository ) {
            return repository.@Id;
        } else {
            return null;
        }

    }

}
