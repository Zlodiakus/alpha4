package intel;

import google.Verify;
import main.DBUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import java.security.acl.Owner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by Shadilan on 11.06.2016.
 */
public class intel {
    public static String getRequest(HttpServletRequest request){
        String result="";
        try {
            //Определить тип запроса.
            String reqName = request.getParameter("ReqName");
            switch (reqName) {
                case "Authorize":
                    String googleToken = request.getParameter("GoogleToken");
                    result = doIntelAuthorize(googleToken);
                    break;
                case "GetData":
                    String token = request.getParameter("Token");
                    int lat1 = Integer.parseInt(request.getParameter("StartLat"));
                    int lng1 = Integer.parseInt(request.getParameter("StartLng"));
                    int lat2 = Integer.parseInt(request.getParameter("FinishLat"));
                    int lng2 = Integer.parseInt(request.getParameter("FinishLng"));
                    result = getData(token, lat1, lng1, lat2, lng2);
                    break;
                default:
                    JSONObject obj = new JSONObject();
                    obj.put("Result", "I0101");
                    obj.put("Message", "Operation not found");
                    result = obj.toJSONString();
            }
        } catch (Exception e){
            JSONObject obj = new JSONObject();
            obj.put("Result", "U0000");
            obj.put("Message", e.toString());
            result = obj.toJSONString();
        }
        return result;
    }
    private static String doIntelAuthorize(String googleToken) {
        String result;

        //Проверка токена
        String email= Verify.checkID(googleToken);
        if (email.charAt(0)=='~') {
            JSONObject jsObject = (new JSONObject());
            jsObject.put("Error", "L0201");
            jsObject.put("Message", "Token не распознан");
            jsObject.put("Detail", email);
            result = jsObject.toJSONString();
            return result;
        }
        //Получение токена приложения
        String token=getIntelToken(email);
        JSONObject jsObject = (new JSONObject());
        switch (token){
            case "L0202":
                jsObject.put("Error","L0202");
                jsObject.put("Message", "Пользователь не найден");
                break;
            case "U0000":
                jsObject.put("Error", "U0000");
                jsObject.put("Message", token);
                break;
            default:
                jsObject.put("Token",token);
        }
        result = jsObject.toJSONString();
        return result;
    }
    private static String getIntelToken(String email)  {
        PreparedStatement pstmt,query;
        String Token = "T" + UUID.randomUUID().toString();
        String PGUID;
        String result="Not Changed";
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
                //Очистка старых коннектов
                pstmt = con.prepareStatement("DELETE FROM Connections WHERE CurrentDate<ADDDATE( CURRENT_TIMESTAMP( ) , INTERVAL -1\n" +
                        "DAY )");
                pstmt.execute();
                pstmt = con.prepareStatement("INSERT into Connections (PGUID,Token,TokenType) Values(?,?,'I')");
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
    private static String getData(String token,int Lat1,int Lng1,int Lat2,int Lng2){
        String result="";
        String pguid="";
        Connection con;
        PreparedStatement pstmt;
        //Проверить токен
        try {

            con = DBUtils.ConnectDB();
            //Проверка пользователя
            //pstmt = con.prepareStatement("SELECT PGUID from Connections WHERE Token=?");
            pstmt = con.prepareStatement("SELECT PGUID from Users WHERE Login='Shadilan'");
            pstmt.setString(1,token);
            ResultSet rs = pstmt.executeQuery();
            if (rs.isBeforeFirst()){
                rs.next();
                pguid=rs.getString(1);
                //Получить данные игрока
                /*
                    GUID
                    PlayerName
                    Level
                    TNL
                    Exp
                    Caravans
                    Gold
                    Race
                    AmbushesMax
                    AmbushesLeft
                    AmbushRadius
                    ActionDistance
                    Hirelings
                    LeftToHire
                    FoundedCities

                    Upgrades
                    Routes
                    Ambushes

                    Error
                    Message
                */
                pstmt = con.prepareStatement("SELECT Name,Level,Exp,Gold,Race from Players WHERE GUID=?");
                pstmt.setString(1,pguid);
                rs = pstmt.executeQuery();
                if (rs.isBeforeFirst()){
                    rs.next();
                    String Name=rs.getString(1) ;
                    int Level=rs.getInt(2);
                    int Exp=rs.getInt(3);
                    int Gold=rs.getInt(4);
                    int Race=rs.getInt(5);
                    JSONObject robj=new JSONObject();
                    robj.put("Name",Name);
                    robj.put("Exp",Exp);
                    robj.put("Gold",Gold);
                    robj.put("Race",Race);
                    JSONArray Upgrades=new JSONArray();
                    pstmt=con.prepareStatement("SELECT Type,Name,Level,Description from PUpgrades pu,Upgrades u " +
                            "where u.GUID=pu.UGUID and pu.PGUID=?");
                            pstmt.setString(1,pguid);
                    while (rs.next()){
                        JSONObject obj=new JSONObject();
                        obj.put("Type",rs.getString(2));
                        obj.put("Name",rs.getString(2));
                        obj.put("Level",rs.getInt(3));
                        obj.put("Description",rs.getString(4));
                        Upgrades.add(obj);
                    }
                    robj.put("Upgrades",Upgrades);
                    pstmt=con.prepareStatement("SELECT c.GUID GUID, c.Profit, g.Lat, g.Lng, gcs.GUID sGUID, gcs.Lat sLat, gcs.Lng sLng, gcf.GUID fGUID, gcf.Lat, gcf.Lng\n" +
                            "FROM Caravans c, GameObjects g, GameObjects gcs, Cities cs, GameObjects gcf, Cities cf\n" +
                            "WHERE c.GUID = g.GUID\n" +
                            "AND c.Start = cs.GUID\n" +
                            "AND c.Finish = cf.GUID\n" +
                            "AND cs.GUID = gcs.GUID\n" +
                            "AND cf.GUID = gcf.GUID\n" +
                            "AND c.PGUID=?");
                    pstmt.setString(1,pguid);
                    JSONArray routes=new JSONArray();
                    while (rs.next()){
                        JSONObject obj=new JSONObject();
                        obj.put("GUID",rs.getString(1));
                        obj.put("Profit",rs.getString(2));
                        obj.put("Lat",rs.getInt(3));
                        obj.put("Lng",rs.getInt(4));
                        obj.put("SGUID",rs.getString(5));
                        obj.put("SLat",rs.getInt(6));
                        obj.put("SLng",rs.getInt(7));
                        obj.put("FGUID",rs.getString(8));
                        obj.put("FLat",rs.getInt(9));
                        obj.put("FLng",rs.getInt(10));

                        routes.add(obj);
                    }
                    robj.put("Routes",routes);
                    pstmt=con.prepareStatement("SELECT a.GUID, a.Radius, a.TTS, a.Life, a.Name, g.Lat, g.Lng\n" +
                            "FROM Ambushes a, GameObjects g\n" +
                            "WHERE g.GUID = a.GUID " +
                            "and a.PGUID=?");
                    pstmt.setString(1,pguid);
                    JSONArray ambushes=new JSONArray();
                    while (rs.next()){
                        JSONObject obj=new JSONObject();
                        obj.put("GUID",rs.getString(1));
                        obj.put("Radius",rs.getInt(2));
                        obj.put("TTS",rs.getInt(3));
                        obj.put("Life",rs.getInt(4));
                        obj.put("Name",rs.getString(5));
                        obj.put("Lat",rs.getInt(6));
                        obj.put("Lng",rs.getInt(7));
                        ambushes.add(obj);
                    }
                    robj.put("Ambushes",ambushes);
                    pstmt=con.prepareStatement("SELECT GUID,Lat , Lng , c.Name, c.Level, p.Name founder, u.Name, \n" +
                            "CASE \n" +
                            "WHEN Influence1 > Influence2\n" +
                            "AND Influence1 > Influence3\n" +
                            "THEN  '1'\n" +
                            "WHEN Influence2 > Influence1\n" +
                            "AND Influence2 > Influence3\n" +
                            "THEN  '2'\n" +
                            "WHEN Influence3 > Influence1\n" +
                            "AND Influence3 > Influence2\n" +
                            "THEN  '3'\n" +
                            "ELSE  '0'\n" +
                            "END faction \n" +
                            ",Influence1 ,Influence2, Influence3"+
                            ",Owner "+
                            "FROM GameObjects ob, Cities c, Players p, Upgrades u\n" +
                            "WHERE c.GUID = ob.GUID\n" +
                            "AND ob.Type =  'City'\n" +
                            "AND p.guid = c.creator\n" +
                            "AND c.UpgradeType = u.Type\n" +
                            "AND u.level =0" +
                            "AND g.Lat between ? and ?"+
                            "AND g.Lng between ? and ?");
                    pstmt.setString(1,pguid);
                    JSONArray cities=new JSONArray();
                    while (rs.next()){
                        JSONObject obj=new JSONObject();
                        obj.put("GUID",rs.getString(1));
                        obj.put("Lat",rs.getInt(6));
                        obj.put("Lng",rs.getInt(7));
                        obj.put("Name",rs.getString(1));
                        obj.put("Level",rs.getInt(2));
                        obj.put("Founder",rs.getString(6));
                        obj.put("Upgrade",rs.getString(6));
                        obj.put("Faction",rs.getString(6));
                        obj.put("Inf1",rs.getInt(3));
                        obj.put("Inf2",rs.getInt(3));
                        obj.put("Inf3",rs.getInt(3));
                        obj.put("Owner",rs.getInt(3));

                        cities.add(obj);
                    }
                    robj.put("Cities",cities);
                    result=robj.toJSONString();


                } else
                {
                    JSONObject obj=new JSONObject();
                    obj.put("Error","L0001");
                    obj.put("Message", "User not found");
                    result=obj.toJSONString();
                }

                //Сформировать ответ
            } else
            {
                JSONObject obj=new JSONObject();
                obj.put("Error","L0001");
                obj.put("Message", "User not found");
                result=obj.toJSONString();
            }

        }
        catch (Exception e){
            JSONObject obj=new JSONObject();
            obj.put("Error","U0000");
            obj.put("Message",e.toString());
            result=obj.toJSONString();
        }
        return result;
    }
}
