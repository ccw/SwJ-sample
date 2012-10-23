package data;

import java.sql.*;

public class SasJdbc {
    public static void main(String[] args) {
        Connection conn = null;
        try {
            //Class.forName("com.sas.net.sharenet.ShareNetDriver");
            Class.forName("com.sas.rio.MVADriver");
            conn = DriverManager.getConnection("jdbc:sasiom://localhost:8591",
                                               //"jdbc:sharenet://localhost:8551",
                                               "sasdemo", "kKk123");
            DatabaseMetaData dma = conn.getMetaData();
            System.out.println("Connected to " + dma.getURL());
            System.out.println("Driver " + dma.getDriverName());
            System.out.println("Version " + dma.getDriverVersion());

            Statement state = conn.createStatement();
            state.executeUpdate("Create Table Work.Test (k varchar(10) NOT NULL, v float NOT NULL, v2 integer not null, v3 datetime)");
            state.executeUpdate("Insert into Work.Test(k, v, v2, v3) Values('A', 10.123, 5, null)");
            ResultSet rs = state.executeQuery("Select * From Work.Test");
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            System.out.println("=====> Column Names <=====");
            StringBuilder s = new StringBuilder();
            for (int i = 1; i <= columns; i++) {
                s.append(meta.getColumnName(i)).append(":").append(meta.getColumnClassName(i)).append('\t');
            }
            System.out.println(s);
            System.out.println("=====> Row Data <=====");
            while (rs.next()) {
                s.delete(0, s.length());
                for (int i = 1; i <= columns; i++) {
                    s.append(rs.getObject(i)).append('\t');
                }
                System.out.println(s);
            }
            state.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != conn) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}