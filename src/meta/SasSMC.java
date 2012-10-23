package meta;

import com.sas.console.visuals.MainConsole;
import com.sas.iom.SASIOMCommon.IServerSessions;
import com.sas.iom.SASIOMCommon.IServerSessionsHelper;
import com.sas.iom.SASIOMCommon.ISessionAdministration;
import com.sas.iom.SASIOMCommon.ISessionAdministrationHelper;
import com.sas.iom.SASIOMDefs.GenericError;
import com.sas.management.MetadataFactory;
import com.sas.management.servers.ServerUtil;
import com.sas.management.servers.impl.WorkspaceLogicalServerImpl;
import com.sas.management.util.MetaServerConnection;
import com.sas.services.connection.ConnectionInterface;
import org.omg.CORBA.SystemException;

import java.util.Properties;

/**
 *
 */
public class SasSMC {

    public static void main(String[] args) throws Exception {

        MainConsole console = new MainConsole();
        console.setVisible(true);

//        Properties properties = new Properties();
//        properties.put("iomsrv.metadatasrv.host", "172.26.4.42");
//        properties.put("iomsrv.metadatasrv.port ", 8591);
//        properties.put("iomsrv.metadatasrv.host", "172.26.4.42");
//        properties.put("iomsrv.metadatasrv.host", "172.26.4.42");
//        properties.put("iomsrv.metadatasrv.host", "172.26.4.42");
//        properties.put("iomsrv.metadatasrv.host", "172.26.4.42");
//        properties.put("iomsrv.metadatasrv.host", "172.26.4.42");

//        MetaServerConnection conn = new MetaServerConnection("172.26.4.42", "8561", "sasadm@saspw", "Sas1234");
//        WorkspaceLogicalServerImpl server = new WorkspaceLogicalServerImpl(conn, "A5ARVWWI.AU000007", null, null);
//
//        ConnectionInterface cx = (ConnectionInterface) server.getMetaConnection();
//        org.omg.CORBA.Object obj = cx.getObject();
//        try {
//            IServerSessions session = IServerSessionsHelper.narrow(obj);
//
//            org.omg.CORBA.Object ssnObj = session.UseSession(ServerUtil.convertStringToUUID("PID 1700"));
//
//            ISessionAdministration admin = ISessionAdministrationHelper.narrow(ssnObj);
//
//            admin.EndSession();
//        } catch (GenericError error) {
//            error.printStackTrace();
//        } catch (SystemException exc) {
//            exc.printStackTrace();
//        }

        /*
        String classID = Server.CLSID_SAS;
        String host = "172.26.4.42";
        int port = 8591;
        String userName = "sasdemo";
        String password = "Sas1234";

        Server server = new BridgeServer(classID, host, port);
        ConnectionFactoryConfiguration cxfConfig =
                new ManualConnectionFactoryConfiguration(server);

        ConnectionFactoryManager cxfManager =
                new ConnectionFactoryManager();

        ConnectionFactoryInterface cxf = cxfManager.getFactory(cxfConfig);
        ConnectionInterface cx = cxf.getConnection(userName, password);

        IWorkspace workspace = IWorkspaceHelper.narrow(cx.getObject());
        IServerSessions session = IServerSessionsHelper.narrow(cx.getObject());

        System.out.println("session " + session);
        System.out.println("workspace " + workspace);
        //org.omg.CORBA.Object ssnObj = session.UseSession(ServerUtil.convertStringToUUID("1700"));

        //ISessionAdministration admin = ISessionAdministrationHelper.narrow(ssnObj);

        //admin.EndSession();

        if (workspace != null) workspace.Close();

        cx.close();
        */
    }

}
