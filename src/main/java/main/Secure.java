package main;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Shadilan on 19.01.2016.
 * Security old object
 */
public class Secure {
    /**
     * getSecureToken
     * @param Login Login of user
     * @param Password Password of user
     * @return Message with generated token
     */
    public static String getSecureToken(String Login,String Password){
        Connection con;
        PreparedStatement pstmt;
        String Token = "T" + UUID.randomUUID().toString();
        String result = "";
        try {
            con = DBUtils.ConnectDB();
            pstmt = con.prepareStatement("SELECT GUID from Users WHERE Login=? and Password=?");
            pstmt.setString(1, Login);
            pstmt.setString(2, Password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.isBeforeFirst()) {
            rs.first();
                String GUID=rs.getString(1);
                pstmt = con.prepareStatement("UPDATE Players SET SecureToken=? WHERE GUID=?");
                pstmt.setString(1, Token);
                pstmt.setString(2, GUID);
                pstmt.execute();
                con.commit();
                con.close();
            } else {
                result = MyUtils.getJSONError("AccessDenied", "User not found.");
            }

        } catch (SQLException e) {
            result = MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));

        } catch (NamingException e) {
            result = MyUtils.getJSONError("ResourceError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));

        }
        if (result.equals("")) return MyUtils.getJSONSuccess(Token);
        else return result;
    }
    /**
     * Create New Player
     *
     * @param Login Player Login
     * @param Password Player Password
     * @param email Player Email
     *
     * TODO: Переписать на новые таблицы
     */
    public String NewPlayer(String Login,String Password,String email,String InviteCode){

        PreparedStatement stmt;
        String result;
        try {
            Connection con = DBUtils.ConnectDB();
            ResultSet rs;
            //Check inviteCode
            stmt = con.prepareStatement("select count(1) cnt from invites where inviteCode=? and Invited=''");
            stmt.setString(1, InviteCode);
            rs=stmt.executeQuery();
            rs.first();
            if (rs.getInt("cnt")==0){
                stmt.close();
                con.close();
                result = "NoInviteCode";
                return MyUtils.getJSONError(result, result);
            }
            //Check Name Available
            stmt = con.prepareStatement("select count(1) cnt from gplayers where PlayerName=? or email=?");
            stmt.setString(1,Login);
            stmt.setString(2,email);
            rs=stmt.executeQuery();
            rs.first();
            if (rs.getInt("cnt")>0){
                stmt.close();
                con.close();
                result = "UserExists";
                return MyUtils.getJSONError(result, result);
            }
            if (Password.length()<6){
                stmt.close();
                con.close();
                result = "EasyPassword";
                return MyUtils.getJSONError(result, result);
            }
            //Write InviteCode
            String GUID=UUID.randomUUID().toString();
            stmt=con.prepareStatement("update invites set Invited=? where inviteCode=?");
            stmt.setString(1, GUID);
            stmt.setString(2, InviteCode);
            stmt.execute();
            //Write Player Info
            stmt = con.prepareStatement("insert into gplayers(GUID,PlayerName,Password,email,HomeCity) VALUES(?,?,?,?,(select guid from cities order by RAND() limit 0,1))");
            stmt.setString(1, GUID);
            stmt.setString(2,Login);
            stmt.setString(3, Password);
            stmt.setString(4, email);
            stmt.execute();
            stmt = con.prepareStatement("insert into aobject(GUID,ObjectType) VALUES(?,\"PLAYER\")");
            stmt.setString(1, GUID);
            stmt.execute();
            stmt.close();
            con.commit();
            con.close();

        } catch (SQLException e) {
            return MyUtils.getJSONError("DBError", e.toString() + Arrays.toString(e.getStackTrace()));
        } catch (NamingException e) {
            return MyUtils.getJSONError("DBError", e.toString() + Arrays.toString(e.getStackTrace()));
        }

        return MyUtils.getJSONSuccess("User Creater.");
    }

}
