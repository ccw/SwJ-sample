import com.sas.iom.SASIOMCommon.IServerAdmin
import com.sas.iom.SASIOMCommon.IServerAdminHelper
import com.sas.iom.SASIOMCommon.IServerSessions
import com.sas.iom.SASIOMCommon.IServerSessionsHelper
import com.sas.iom.SASIOMDefs.DoubleSeqHolder
import com.sas.iom.SASIOMDefs.StringSeqHolder
import com.sas.iom.SASIOMDefs.UUIDSeqHolder
import com.sas.services.connection.*
import com.sas.management.util.MetaServerConnection
import com.sas.management.servers.impl.WorkspaceLogicalServerImpl
import com.sas.management.servers.ServerFactory

/*
try {
    //UserContextInterface user = CorePlatformServices.userService.newUser("sasadm@saspw", "Sas1234", "DefaultAuth")

    BridgeServer server = new BridgeServer(Server.CLSID_SAS, "172.26.4.42", 8591)
    server.domain = "DefaultAuth"
    ConnectionFactoryConfiguration config = new ManualConnectionFactoryConfiguration(server)

    ConnectionFactoryInterface factory = ConnectionFactoryManager.getConnectionFactory(config)
    connection = factory.getConnection("sassrv", "Sas1234", "DefaultAuth")

    org.omg.CORBA.Object obj = connection.object

    IServerSessions srvSessions = IServerSessionsHelper.narrow(obj);

    if (srvSessions != null) {
        UUIDSeqHolder sessIds = new UUIDSeqHolder();
        StringSeqHolder owners = new StringSeqHolder();
        DoubleSeqHolder secsInact = new DoubleSeqHolder();
        srvSessions.SessionList("", sessIds, owners, secsInact);
        numSessions = sessIds.value.length;
        System.out.println("\n# of sessions: " + numSessions);
        for (int i = 0; i < numSessions; i++) {
            println(i + 1 + ".\t" + sessIds.value[i].toString() + "\t" + owners.value[i] + "\t" + secsInact.value[i]);
        }
    }

    connection.close()
    factory.adminInterface.destroy()
} catch (Exception e) {
    e.printStackTrace();
    println(e.getMessage());
}
*/
try {
    ServerFactory.initialize()
} catch (Exception e) {
    e.printStackTrace()
}

