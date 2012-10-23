package chart;

import com.sas.iom.SAS.ILanguageService;
import com.sas.iom.SAS.IWorkspace;
import com.sas.iom.SAS.IWorkspaceHelper;
import com.sas.rio.MVAConnection;
import com.sas.services.connection.*;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 */
public class SampleDataGenerator {

    public static final String def_proc = "LIBNAME EDA ORACLE  PATH=XE  SCHEMA=EDA  USER=eda  PASSWORD=eda;\n" +
                                          "DATA WAT;\n" +
                                          "    SET EDA.WAT;\n" +
                                          "    KEEP PRODUCT_NAME SPEC_FILE LOT_ID WAFER_ID SITE_ID ATT1 ATT2 ATT3;\n" +
                                          "    WHERE LOT_ID like 'P%';\n" +
                                          //"    WHERE LOT_ID eq 'F50RF';\n" +
                                          "RUN;";

    private String procedure;

    public SampleDataGenerator() {
        procedure = def_proc;
    }

    public SampleDataGenerator(String proc) {
        procedure = proc;
    }

    public TableModel generate() throws Exception {
        TableModel result = null;
        //
        String classID = Server.CLSID_SAS;
        String host = "172.26.4.36";
        int port = 8591;
        String userName = "twncwc";
        String password = "kKk123";
        //
        ConnectionInterface cx = null;
        IWorkspace sasWorkspace = null;
        Connection conn = null;
        Statement state = null;
        ResultSet res = null;
        ResultSetMetaData meta = null;
        try {
            Server server = new BridgeServer(classID, host, port);
            ConnectionFactoryConfiguration cxfConfig = new ManualConnectionFactoryConfiguration(server);
            ConnectionFactoryManager cxfManager = new ConnectionFactoryManager();
            ConnectionFactoryInterface cxf = cxfManager.getFactory(cxfConfig);
            cx = cxf.getConnection(userName, password);
            sasWorkspace = IWorkspaceHelper.narrow(cx.getObject());

            ILanguageService sasLanguage = sasWorkspace.LanguageService();

            sasLanguage.Submit(procedure);
            sasLanguage.FlushList(Integer.MAX_VALUE);

            //result = new JDBCToTableModelAdapter(conn, "Select * From WORK.WAT");

            conn = new MVAConnection(sasWorkspace, new Properties());
            state = conn.createStatement();
            res = state.executeQuery("Select * From WORK.WAT");
            meta = res.getMetaData();

            String[] columns = new String[meta.getColumnCount()];
            for (int i = 1; i <= columns.length; i++) {
                columns[i - 1] = meta.getColumnName(i);
            }

            ArrayList<Object[]> rows = new ArrayList<Object[]>();
            while (res.next()) {
                Object[] row = new Object[columns.length];
                for (int i = 1; i <= columns.length; i++) {
                    row[i - 1] = res.getObject(i);
                }
                rows.add(row);
            }
            result = new DefaultTableModel(rows.toArray(new Object[rows.size()][]), columns);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
                if (state != null) {
                    state.close();
                }
                if (conn != null) {
                    conn.close();
                }
                if (sasWorkspace != null) {
                    sasWorkspace.Close();
                }
                if (cx != null) {
                    cx.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //
        return result;
    }

    public static void main(String[] args) throws Exception {
        TableModel result = new SampleDataGenerator().generate();
        System.out.println("Columns -> " + result.getColumnCount());
        System.out.println("Rows -> " + result.getRowCount());
    }
}
