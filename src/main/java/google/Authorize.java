package google;

import main.DBUtils;
import org.json.simple.JSONObject;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Shadilan on 28.05.2016.
 * Авторизация
 */
public class Authorize {
    public static String create(HttpServletRequest request){
        //Распарсить запрос.
        String reqName=request.getParameter("ReqName");
        String result;
        try {
            switch (reqName) {
                //Если регистрация выполнить регистрацию
                case "Authorize":
                    result = doAuthorize(request);
                    break;
                //Если вход выполнить вход
                case "Register":
                    result = doRegister(request);
                    break;
                default:
                    JSONObject jsObject = (new JSONObject());
                    jsObject.put("Error", "E0001");
                    jsObject.put("Message", "Unknown Operation");
                    result = jsObject.toJSONString();
            }
        } catch (Exception e){
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "E0002");
            jsObject.put("Message", "Server Error");
            jsObject.put("Detail", e.toString());
            jsObject.put("Stack", Arrays.toString(e.getStackTrace()));
            result = jsObject.toJSONString();
        }
        return result;
    }
    private static String doAuthorize(HttpServletRequest request) {
        String result;
        //Получение параметров
        String googleToken=request.getParameter("GoogleToken");
        String hash=request.getParameter("hash");
        String version=request.getParameter("Version");
        //Проверка Версии
        if (!Version.checkVersion(version)) {
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "L0203");
            jsObject.put("Message", "Версия не поддерживается");
            jsObject.put("Detail", version);
            result = jsObject.toJSONString();
            return result;
        }
        //Проверка HASH
        if (!Version.checkHash(hash,version,"Authorize",googleToken)){
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "H0101");
            jsObject.put("Message", "Хэш не верен");
            result = jsObject.toJSONString();
            return result;
        }
        //Проверка токена
        String email=Verify.checkID(googleToken);
        if (email.charAt(0)=='~') {
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "L0201");
            jsObject.put("Message", "Token не распознан");
            jsObject.put("Detail", email);
            result = jsObject.toJSONString();
            return result;
        }
        //Получение токена приложения
        String token=getToken(email);
        JSONObject jsObject = (new JSONObject());
        switch (token){
            case "L0202":
                jsObject.put("Error","L0202");
                jsObject.put("Message", "Пользователь не найден");
                break;
            case "U0000":
                break;
            default:
                jsObject.put("Error", "U0000");
                jsObject.put("Message", token);
        }
        result = jsObject.toJSONString();
        return result;
    }
    private static String doRegister(HttpServletRequest request){
        String result;
        String googleToken=request.getParameter("GoogleToken");
        String hash=request.getParameter("hash");
        String version=request.getParameter("Version");
        String invite=request.getParameter("InviteCode");
        String userName=request.getParameter("UserName");
        //Проверка Версии
        if (!Version.checkVersion(version)) {
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "L0304");
            jsObject.put("Message", "Версия не поддерживается");
            result = jsObject.toJSONString();
            return result;
        }
        //Проверка HASH
        if (!Version.checkHash(hash,version,"Register",googleToken,userName,invite)){
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "H0101");
            jsObject.put("Message", "Хэш не верен");
            result = jsObject.toJSONString();
            return result;
        }
        //Проверка токена
        String email=Verify.checkID(googleToken);
        if (email.charAt(0)=='~') {
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "L0301");
            jsObject.put("Message", "Token не распознан");
            jsObject.put("Detail", email);
            result = jsObject.toJSONString();
            return result;
        }
        //Регистрация
        JSONObject jsObject = (new JSONObject());
        String err=register(email,userName,invite);
        switch (err){
            case "OK":
                String token=getToken(email);
                switch (token){
                    case "L0202":
                        jsObject.put("Error","L0202");
                        jsObject.put("Message", "Пользователь не найден");
                        break;
                    case "U0000":
                        break;
                    default:
                        jsObject.put("Error", "U0000");
                        jsObject.put("Message", err);
                }
                result = jsObject.toJSONString();
                break;
            case "L0302":
                jsObject.put("Error", err);
                jsObject.put("Message", "Инвайт код не действителен");
                result = jsObject.toJSONString();
                break;
            case "L0303":
                jsObject.put("Error", err);
                jsObject.put("Message", "Пользователь уже зарегестрирован");
                result = jsObject.toJSONString();
                break;
            default:

                jsObject.put("Error", err);
                jsObject.put("Message", "Непредвиденная ошибка регистрации:"+err);
                result = jsObject.toJSONString();
        }
        return result;
    }
    private static String register(String email,String userName,String inviteCode){
        String result;
        PreparedStatement pstmt,query;
        Connection con= null;
        try {
            con = DBUtils.ConnectDB();

            //Проверка пользователя
            pstmt = con.prepareStatement("SELECT count(1) from Users WHERE email=? or Login=?");
            pstmt.setString(1, email);
            pstmt.setString(2, userName);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            int cnt = rs.getInt(1);
            if (cnt!=0){
                //Проверить Инвайт
                pstmt = con.prepareStatement("SELECT count(1) from Invites WHERE Invite=? and given=1 and used=0");
                pstmt.setString(1, inviteCode);
                rs= pstmt.executeQuery();
                rs.next();
                cnt=rs.getInt(1);
                if (cnt!=0) {
                    //Обновить Инвайт
                    pstmt = con.prepareStatement("Update Invites set used=1 WHERE Invite=? and given=1 and used=0");
                    pstmt.setString(1, inviteCode);
                    pstmt.execute();
                    //Создать пользователя
                    String UGUID=UUID.randomUUID().toString();
                    pstmt = con.prepareStatement("insert into Users (GUID, Login, Password, email) values (?,?,?,?)");
                    pstmt.setString(1, UGUID);
                    pstmt.setString(2, userName.trim());
                    pstmt.setString(3, inviteCode);
                    pstmt.setString(4, email);
                    pstmt.execute();
                    con.commit();
                    //Определить результат как Ок
                    result="OK";
                } else
                {
                    result="L0302";
                }
            } else {
                result="L0303";
            }
        } catch (NamingException | SQLException e) {
            //result="U0000";
            result=e.toString();
        }
        try {
            if (con!=null && !con.isClosed()) con.close();
        } catch (SQLException e) {
            result="U0001";
        }
        return result;

    }
    private static String getToken(String email)  {
        PreparedStatement pstmt,query;
        String Token = "T" + UUID.randomUUID().toString();
        String PGUID;
        String result;
        Connection con= null;
        try {
            con = DBUtils.ConnectDB();

            //Проверка пользователя
            pstmt = con.prepareStatement("SELECT GUID,Login from Users WHERE email=?");
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                PGUID = rs.getString(1);
                String login=rs.getString(2);
                //Проверка игрока
                query=con.prepareStatement("select count(1) from Players where GUID=?");
                query.setString(1,PGUID);
                ResultSet rs2=query.executeQuery();
                rs2.first();
                //Создание игрока если нет
                if (rs2.getInt(1)==0) {
                    query=con.prepareStatement("insert into Players (GUID, Name, Level, Exp, Gold,Class, Race) values (?,?,1,0,0,0,0)");
                    query.setString(1, PGUID);
                    query.setString(2, login);
                    query.execute();
                    query=con.prepareStatement("insert into Stats (PGUID) values (?)");
                    query.setString(1, PGUID);
                    query.execute();
                    query=con.prepareStatement("insert into GameObjects (GUID, Lat, Lng, Type) values (?,100,100,'Player')");
                    query.setString(1, PGUID);
                    query.execute();
                    query = con.prepareStatement("insert into PUpgrades(PGUID,UGUID) SELECT ?,u.GUID from Upgrades u WHERE level=0");
                    query.setString(1, PGUID);
                    query.execute();
                    con.commit();
                }
                rs2.close();
                query.close();
                pstmt = con.prepareStatement("INSERT into Connections (PGUID,Token) Values(?,?)");
                pstmt.setString(1, PGUID);
                pstmt.setString(2, Token);
                pstmt.execute();
                pstmt.close();
                con.commit();
                result=Token;

            } else {
                result="L0202";
            }
        } catch (NamingException | SQLException e) {
            result=e.toString();
        }
        try {
            if (con!=null && !con.isClosed()) con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

}
