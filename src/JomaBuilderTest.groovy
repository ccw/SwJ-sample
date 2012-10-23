import com.sas.groovy.util.JomaBuilder;
import com.sas.groovy.util.JomaBuilderTemplate;

println "start";
JomaBuilder b = new JomaBuilder( "localhost", "15975" );
JomaBuilderTemplate sat = new JomaBuilderTemplate(b, "SASApplicationServerTemplate");
JomaBuilderTemplate wst = new JomaBuilderTemplate(b, "WorkspaceServerTemplate");
JomaBuilderTemplate spt = new JomaBuilderTemplate(b, "StoredProcessServerTemplate");
JomaBuilderTemplate pwt = new JomaBuilderTemplate(b, "PooledWorkspaceServerTemplate");
JomaBuilderTemplate ost = new JomaBuilderTemplate(b, "ObjectSpawnerTemplate");

b.Person(Name:"Darren2")
    .withLogins(
        b.Login(Name:"Login2", UserID:"carynt\\mickey2")
            //.withDomain( b.AuthenticationDomain(Name:"DefaultAuth") )
    )
    .commit();

b.createInternalUserAndCommit(Name:"sastrust2", Password:"sastrust1");

sat.SASAppServer(Name:"YYYpp")
    .withServerComponents(
        wst.LogicalWorkspaceServer(Name:"YYYpp - Logical Workspace Server")
            .withProperties(
                wst.Property(PropertyName:"AuthService", DefaultValue:"Metadata")
            )
            .withServers(
                wst.WorkspaceServer(Name:"YYYpp - Workspace Server")
                    .withMachines(
                        wst.Machine(Name:"localhost")
                    )
                    .withProperties(
                        wst.Property(PropertyName:"Command", DefaultValue:"\\\\ronco\\public\\dnt\\tools\\sdssas -t dev/mva-v920 -armsubsys (ARM_ALL) -armagent log4sas -logcfgloc \\bin\\servers\\ms-test\\s.cbe.xml -logapplname workspace -pp /u/sasdrs/playarm -cprxp"),
                        wst.Property(PropertyName:"UseSpawnerID", DefaultValue:"True")
                    )
                    .withSourceConnections( wst.BridgeConnection(Name:"YYYConnection: - Workspace Server", Port:"8592") )
            ),
        spt.LogicalStoredProcessServer(Name:"YYYpp - Logical Stored Process Server")
            .withProperties(
                spt.Property(PropertyName:"Maximum Session Timeout", DefaultValue:"3600000"),
                spt.Property(PropertyName:"AuthService", DefaultValue:"Metadata")
            )
            .withServers(
                spt.StoredProcessServer(Name:"YYYpp - Stored Process Server")
                    .withMachines( spt.Machine(Name:"localhost") )
                    .withProperties(
                        spt.Property(PropertyName:"Command", DefaultValue:"\\\\ronco\\public\\dnt\\tools\\sdssas -t dev/mva-v920 -armsubsys (ARM_ALL) -armagent log4sas -logcfgloc \\bin\\servers\\ms-test\\s.cbe.xml -logapplname storedprocess"),
                        spt.Property(PropertyName:"LBAvailabilityTimeout", DefaultValue:"3600000"),
                        spt.Property(PropertyName:"UseSpawnerID", DefaultValue:"True")
                    )
                    .withSourceConnections(
                        spt.BridgeConnection(Name:"YYYConnection: pp - Stored Process Server", Port:"8602"),
                        spt.MultiBridgeConnection(Name:"YYYlb2", Port:"8612")
                    )
            )
    )
    .commit();

ost.ObjectSpawner(Name:"YYYSpawner")
    .withInitProcesses(
        ost.SpawnerInit(Name:"Initialization")
            .withProperties( ost.Property(PropertyName:"DisableTelnet", DefaultValue:"True") )
     )
    .withMachines( ost.Machine(Name:"localhost") )
    .withSourceConnections(
        ost.OperatorConnection(Name:"YYYOperator connection1", Port:"8581"),
        ost.PortBankConnection(Name:"YYYPortBank connection1", Port:"8642"),
        ost.PortBankConnection(Name:"YYYPortBank connection2", Port:"8643"),
        ost.PortBankConnection(Name:"YYYPortBank connection3", Port:"8644"),
        ost.PortBankConnection(Name:"YYYPortBank connection4", Port:"8645")
    )
    .withSpawnedServers(
        wst.WorkspaceServerReference(Name:"YYYpp - Workspace Server"),
        spt.StoredProcessServerReference(Name:"YYYpp - Stored Process Server")
    )
    .commit();

return;

sat.SASAppServer(Name:"YYYTESTApp")
    .withUsingComponents(
        wst.LogicalWorkspaceServer(Name:"YYYTESTApp - Logical Workspace Server")
            .withProperties(
                wst.Property(PropertyName:"EnableSACS", DefaultValue:"True"),
                wst.Property(PropertyName:"AuthService", DefaultValue:"Metadata")
            )
            .withUsingComponents(
                wst.WorkspaceServer(Name:"YYYTESTApp - Workspace Server")
                    .withMachines( b.Machine(Name:"localhost") )
                    .withProperties(
                        wst.Property(PropertyName:"Command", DefaultValue:"\\\\ronco\\public\\dnt\\tools\\sdssas -t dev/mva-v920 -armsubsys (ARM_ALL) -armagent log4sas -logcfgloc \\bin\\servers\\ms-test\\s.cbe.xml -logapplname workspace"),
                        wst.Property(PropertyName:"UseSpawnerID", DefaultValue:"True")
                    )
                    .withSourceConnections( wst.BridgeConnection(Name:"YYYConnection: TESTApp - Workspace Server") )
                    .withSpawners( ost.ObjectSpawnerReference( Name:"YYYSpawner" ) )
            ),
        spt.LogicalStoredProcessServer(Name:"YYYTESTApp - Logical Stored Process Server")
            .withProperties(
                spt.Property(PropertyName:"Maximum Session Timeout", DefaultValue:"3600000"),
                spt.Property(PropertyName:"EnableSACS", DefaultValue:"True"),
                spt.Property(PropertyName:"AuthService", DefaultValue:"Metadata")
            )
            .withUsingComponents(
                spt.StoredProcessServer(Name:"YYYTESTApp - Stored Process Server")
                    .withMachines( spt.Machine(Name:"localhost") )
                    .withProperties(
                        spt.Property(PropertyName:"Command", DefaultValue:"\\\\ronco\\public\\dnt\\tools\\sdssas -t dev/mva-v920 -armsubsys (ARM_ALL) -armagent log4sas -logcfgloc \\bin\\servers\\ms-test\\s.cbe.xml -logapplname storedprocess"),
                        spt.Property(PropertyName:"LBAvailabilityTimeout", DefaultValue:"3600000"),
                        spt.Property(PropertyName:"UseSpawnerID", DefaultValue:"True")
                    )
                    .withSourceConnections(
                        spt.BridgeConnection(Name:"YYYConnection: TESTApp - Stored Process Server"),
                        spt.MultiBridgeConnection(Name:"YYYlb1", Port:"8611"),
                        spt.MultiBridgeConnection(Name:"YYYremoveme - lb2", Port:"8612")
                    )
                    .withSpawners( ost.ObjectSpawnerReference(Name:"YYYSpawner") )
            ),
        pwt.LogicalPooledWorkspaceServer(Name:"YYYTESTApp - Logical Pooled Workspace Server")
            .withProperties(
                pwt.Property(PropertyName:"EnableSACS", DefaultValue:"True"),
                pwt.Property(PropertyName:"AuthService", DefaultValue:"Metadata")
            )
            .withUsingComponents(
                pwt.PooledWorkspaceServer(Name:"YYYTESTApp - Pooled Workspace Server")
                    .withMachines( pwt.Machine(Name:"localhost") )
                    .withProperties(
                        pwt.Property(PropertyName:"Command", DefaultValue:"\\\\ronco\\public\\dnt\\tools\\sdssas -t dev/mva-v920 -armsubsys (ARM_ALL) -armagent log4sas -logcfgloc \\bin\\servers\\ms-test\\s.cbe.xml -logapplname pooledworkspace"),
                        pwt.Property(PropertyName:"LBAvailabilityTimeout", DefaultValue:"3600000"),
                        pwt.Property(PropertyName:"UseSpawnerID", DefaultValue:"True")
                    )
                    .withSourceConnections( pwt.BridgeConnection(Name:"YYYConnection: TESTApp - Pooled Workspace Server") )
                    .withSpawners( ost.ObjectSpawnerReference(Name:"YYYSpawner") )
            )
    )
    .commit();

b.AuthenticationDomain(Name:"DefaultAuth")
    .withLogins(
        b.LoginReference(Name:"Login2")
    )
    .withConnections(
        ost.OperatorConnectionReference(Name:"YYYOperator connection1"),
        ost.PortBankConnectionReference(Name:"YYYPortBank connection1"),
        ost.PortBankConnectionReference(Name:"YYYPortBank connection2"),
        ost.PortBankConnectionReference(Name:"YYYPortBank connection3"),
        ost.PortBankConnectionReference(Name:"YYYPortBank connection4"),
        wst.BridgeConnectionReference(Name:"YYYConnection: TESTApp - Workspace Server"),
        spt.BridgeConnectionReference(Name:"YYYConnection: TESTApp - Stored Process Server"),
        pwt.BridgeConnectionReference(Name:"YYYConnection: TESTApp - Pooled Workspace Server"),
        spt.MultiBridgeConnectionReference(Name:"YYYlb1"),
        spt.MultiBridgeConnectionReference(Name:"YYYremoveme - lb2"),
        spt.BridgeConnectionReference(Name:"YYYConnection: pp - Stored Process Server"),
        b.TCPIPConnectionReference(Name:"YYYlb2")
    )
    .commit();

b.TCPIPConnectionReference(Name:"YYYPortBank connection4").delete().commit();

b.dispose();

/*
b.StoredProcessServerReference(Name:"TESTApp - Stored Process Server"){
    b.BridgeConnectionReference(Name:"removeme - lb2").delete();
}*/

