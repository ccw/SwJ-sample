package data;

import com.sas.iom.SAS.ILanguageService;
import com.sas.iom.SAS.ILanguageServicePackage.CarriageControlSeqHolder;
import com.sas.iom.SAS.ILanguageServicePackage.LineTypeSeqHolder;
import com.sas.iom.SAS.IWorkspace;
import com.sas.iom.SAS.IWorkspaceHelper;
import com.sas.iom.SASIOMDefs.StringSeqHolder;
import com.sas.services.connection.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 */
public class SasIom {

    String procedure = "LIBNAME EDA ORACLE  PATH=XE  SCHEMA=EDA  USER=eda  PASSWORD=eda;" +
                       "DATA WAT;" + 
                       "    SET EDA.WAT;" +
                       "    KEEP PRODUCT_NAME SPEC_FILE LOT_ID WAFER_ID SITE_ID ATT1 ATT2 ATT3;" + 
                       "    WHERE LOT_ID like 'P0M78.1';" + 
                       "RUN;" +
                       "DATA _NULL_;" +
                       "    file print PS=32767;" +
                       "    set WAT end=lastrec;" +
                       "    if _N_ eq 1 then do;" +
                       "        put '[';" +
                       "    end;" +
                       "    put '{\"productName\":\"' PRODUCT_NAME '\",';" +
                       "    put '\"lotId\":\"' LOT_ID '\",';" +
                       "    put '\"waferId\":' WAFER_ID ',';" +
                       "    put '\"siteId\":\"' SITE_ID '\",';" +
                       "    put '\"specFile\":\"' SPEC_FILE '\",';" +
                       "    put '\"attrs\": [';" +
                       "    put '{\"attr1\":' ATT1 '},';" +
                       "    put '{\"attr2\":' ATT2 '},';" +
                       "    put '{\"attr3\":' ATT3 '}]}';" +
                       "    if lastrec eq 1 then do;" +
                       "        put ']';" +
                       "    end;" +
                       "    else do;" +
                       "        put ',';" +
                       "    end;" +
                       "RUN;";

    public SasIom() throws Exception {
        loadScript("C:\\Projects\\Testing\\SasJdbc\\script\\Fetch_MKPs.sas");
        procedure = procedure.replaceAll("&QRY_START_DT", "01APR2011").replaceAll("&QRY_END_DT", "31MAY2011");
        //System.out.println(procedure);
        runProcedure();
    }

    public void loadScript(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
        StringBuilder procedureBuilder = new StringBuilder();
        String line = reader.readLine();
        while(line != null) {
            procedureBuilder.append(line);
            line = reader.readLine();
        }
        procedure = procedureBuilder.toString();
    }

    public void runProcedure() throws Exception {
        String classID = Server.CLSID_SAS;
        String host = "172.17.64.177"; //"localhost";
        int port = 8591;
        String userName = "sasdemo"; //"twncwc";
        String password = "Aa123456"; //"kKk123";

        Server server = new BridgeServer(classID, host, port);
        ConnectionFactoryConfiguration cxfConfig =
                new ManualConnectionFactoryConfiguration(server);

        ConnectionFactoryManager cxfManager =
                new ConnectionFactoryManager();

        ConnectionFactoryInterface cxf = cxfManager.getFactory(cxfConfig);
        ConnectionInterface cx = cxf.getConnection(userName, password);
        IWorkspace iWorkspace = IWorkspaceHelper.narrow(cx.getObject());
        ILanguageService sasLanguage = iWorkspace.LanguageService();

        sasLanguage.Submit(procedure);

        CarriageControlSeqHolder logCarriageControlHldr = new CarriageControlSeqHolder();
        LineTypeSeqHolder logLineTypeHldr = new LineTypeSeqHolder();
        StringSeqHolder logHldr = new StringSeqHolder();
        sasLanguage.FlushLogLines(Integer.MAX_VALUE, logCarriageControlHldr, logLineTypeHldr, logHldr);
        System.out.println("==> Log <==");
        for (String line: logHldr.value) {
            System.out.println(line);
        }

        CarriageControlSeqHolder holder = new CarriageControlSeqHolder();
        LineTypeSeqHolder listLineTypeHldr = new LineTypeSeqHolder();
        StringSeqHolder listHldr = new StringSeqHolder();
        sasLanguage.FlushListLines(Integer.MAX_VALUE, holder, listLineTypeHldr, listHldr);
        
        System.out.println("==> OUT <==");
        /*
        for (CarriageControl control: holder.value) {
            System.out.println("control -> [" + control.getClass() + ":" + control.value() + "] " + control.toString());
        }
        for (LineType type: listLineTypeHldr.value) {
            System.out.println("type -> [" + type.getClass() + ":" + type.value() + "]" + type.toString());
        } 
        */
        for (String line: listHldr.value) {
            System.out.println(line);
        }

        iWorkspace.Close();
        cx.close();
    }

    public static void main(String[] args) {
        try {
            new SasIom();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
