package main;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author Shadilan
 */
public class DBUtils {
    //private static DBUtils instance;
    //private Connection con;
    private static ArrayList<Connection> connections;
    public static Connection ConnectDB() throws NamingException, SQLException {
      //  if (instance ==null) {
        if (connections==null) connections= new ArrayList<>();
        ArrayList<Connection> rem= new ArrayList<>();

        for (Connection conn:connections){

            try {
                if (conn.isClosed()) rem.add(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        connections.removeAll(rem);
            Context ctx;
            DataSource ds;
            ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:jboss/datasources/MySQLDS");

            //instance=new DBUtils();
            Connection con = ds.getConnection("adminrpIFmWj", "6XCs61ikDPyj");
            con.setAutoCommit(false);
            connections.add(con);
    //    }
        return con;
    }
    public static int getConCount(){
        int count=0;
        ArrayList<Connection> rem= new ArrayList<>();
        for (Connection conn:connections){
            try {
                if (conn.isClosed()) rem.add(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        connections.removeAll(rem);
        if (connections.size()>count) {count=connections.size();}
        return count;
    }
    public static String closeAllConnection(){
        ArrayList<Connection> rem= new ArrayList<>();
        for (Connection conn:connections){
            try {
                if (!conn.isClosed()) conn.close();
            } catch (SQLException e) {
                return e.toString();
            }
        }
        return "Ok";
    }

}
