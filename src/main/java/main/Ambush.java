package main;

import org.json.simple.JSONObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Well on 17.01.2016.
 * Ambush object
 */
public class Ambush {
    String PGUID, GUID, Name;
    int Lat,Lng,Radius,Life, TTS, Race;
    Connection con;

    public Ambush() {

    }

    public Ambush(String GUID, Connection con) {
        PreparedStatement query;
        try {
            query = con.prepareStatement("select z1.PGUID, z1.Radius, z1.TTS, z1.Life, z1.Name, z2.Lat, z2.Lng, z3.Race from Ambushes z1, GameObjects z2, Players z3 where z3.GUID=z1.PGUID and z2.GUID=z1.GUID and z1.GUID=?");
            query.setString(1, GUID);
            ResultSet rs=query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                PGUID=rs.getString("PGUID");
                Radius=rs.getInt("Radius");
                TTS=rs.getInt("TTS");
                Life=rs.getInt("Life");
                Name=rs.getString("Name");
                Lat=rs.getInt("Lat");
                Lng=rs.getInt("Lng");
                Race=rs.getInt("Race");
            }
            rs.close();
            query.close();
        } catch (SQLException e) {MyUtils.Logwrite("Ambush","SQL Error: "+e.toString());}
    }


    public Ambush(String aGUID, int aLat, int aLng, int aRadius, int aLife, String aName) {
        GUID=aGUID;
        Lat=aLat;
        Lng=aLng;
        Radius=aRadius;
        Life=aLife;
        Name=aName;
    }

    public Ambush(String AGUID, String APGUID, String AName, int ALife, int ALat, int ALng, Connection CON) {
        GUID=AGUID;
        PGUID=APGUID;
        Name=AName;
        Life=ALife;
        Lat=ALat;
        Lng=ALng;
        con=CON;
    }

    public String Set(String PGUID, int LAT, int LNG, int radius, int TTS, int Life, boolean Elf, Connection con) {
        //TODO check for near cities and ambushes
        PreparedStatement query;
        String AGUID;
        JSONObject jresult = new JSONObject();
        JSONObject jobj = new JSONObject();
                if (canSetAmbush(LAT, LNG, con)) {
                    AGUID = UUID.randomUUID().toString();
                    try {
                        query = con.prepareStatement("insert into Ambushes(GUID,PGUID,Radius,TTS,Life,Name) values (?,?,?,?,?,?)");
                        query.setString(1, AGUID);
                        query.setString(2, PGUID);
                        query.setInt(3, radius);
                        query.setInt(4, TTS);
                        query.setInt(5, Life);
                        query.setString(6, Name);
                        query.execute();
                        query.close();

                        query = con.prepareStatement("insert into GameObjects(GUID,Lat,Lng,Type) values (?,?,?,'Ambush')");
                        query.setString(1, AGUID);
                        query.setInt(2, LAT);
                        query.setInt(3, LNG);
                        query.execute();
                        query.close();
                        con.commit();
                        jresult.put("Result", "OK");
                        jobj.put("GUID", AGUID);
                        jobj.put("Type", "Ambush");
                        jobj.put("Lat", LAT);
                        jobj.put("Lng", LNG);
                        jobj.put("Owner", 0);
                        jobj.put("Radius", radius);
                        jobj.put("Ready", TTS);
                        jobj.put("Name",Name);
                        jobj.put("Life",Life*10);
                        jresult.put("Ambush",jobj);
                    } catch (SQLException e) {
                        jresult.put("Result", "DB001");
                        jresult.put("Message", "DBError in SetAmbush: PGUID=(" + PGUID + ")" + e.toString() + Arrays.toString(e.getStackTrace()));
                    }
                }
                    else {
                    //jresult.put("Error", "Can't set ambush here. City or another ambush is too close.");
                    jresult.put("Result","O0201");
                    jresult.put("Message","Невозможно установить засаду здесь. Город или другая засада слишком близко!");
                }


        return jresult.toString();
    }

    public boolean canSetAmbush(int LAT, int LNG, Connection con) {
        //TODO use deltas instead of numbers
        PreparedStatement query;
        ResultSet rs;
        String CName, minName="в чистом поле";
        int TLat,TLng, TRadius, CLevel;
        double minDist=10000, curDist;
        boolean result=true;
        try {
            query = con.prepareStatement("select z1.Lat, z1.Lng, z2.Radius from GameObjects z1, Ambushes z2 where z2.GUID=z1.GUID and abs(z1.Lat-?)<1500 and abs(z1.Lng-?)<1500");
            query.setInt(1, LAT);
            query.setInt(2, LNG);
            rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                while (rs.next()) {
                    TLat = rs.getInt("Lat");
                    TLng = rs.getInt("Lng");
                    TRadius = rs.getInt("Radius");
                    if (MyUtils.RangeCheck(LAT,LNG,TLat,TLng)<TRadius) {
                        result=false;
                        rs.last();
                    }
                }
            }
            if (result) {
                query = con.prepareStatement("select z1.Lat, z1.Lng, z2.Name, z2.Level from GameObjects z1, Cities z2 where z2.GUID=z1.GUID and z1.Type='City' and abs(z1.Lat-?)<15000 and abs(z1.Lng-?)<15000");
                query.setInt(1, LAT);
                query.setInt(2, LNG);
                rs = query.executeQuery();
                if (rs.isBeforeFirst()) {
                    while (rs.next()) {
                        TLat = rs.getInt("Lat");
                        TLng = rs.getInt("Lng");
                        CName = rs.getString("Name");
                        CLevel=rs.getInt("Level");
                        curDist=MyUtils.RangeCheck(LAT,LNG,TLat,TLng);
                        if (curDist<minDist) {
                            minDist=curDist;
                            minName="возле города "+CName;
                        }
                        if (curDist<50+2*(CLevel-1)) {
                            result=false;
                            rs.last();
                        }
                    }
                }
            }
            rs.close();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("Ambush.canSetAmbush", "SQL Error: " + e.toString());
            result=false;
        }
        Name=minName;
        return result;
    }

    public String Destroy(String AGUID, Connection con) {
        String result;
        JSONObject jresult = new JSONObject();
        GUID=AGUID;
        result=delete(con);
        if (result.equals("OK")) {jresult.put("Result","OK");}
             else {jresult.put("Result","DB001");jresult.put("Message",result);}
        return jresult.toString();
    }

    public String delete(Connection con) {
        PreparedStatement query;
        try {
            query = con.prepareStatement("delete from Ambushes where GUID=?");
            query.setString(1, GUID);
            query.execute();
            query.close();
            query = con.prepareStatement("delete from GameObjects where GUID=?");
            query.setString(1, GUID);
            query.execute();
            query.close();
            con.commit();
            MyUtils.Logwrite("Ambush.delete","Ambush deleted GUID="+GUID);
            return "OK";
        } catch (SQLException e) {
            MyUtils.Logwrite("Ambush.delete","Error while deleting ambush GUID="+GUID+". SQLERROR:"+e.toString());return e.toString();
        }
    }

    public String update(Connection con) {
        PreparedStatement query;
        try {
            query = con.prepareStatement("update Ambushes set Life=? where GUID=?");
            query.setInt(1, Life);
            query.setString(2, GUID);
            query.execute();
            query.close();
            con.commit();
            //MyUtils.Logwrite("Ambush.update","Ambush updated. GUID="+GUID);
            return "OK";
        } catch (SQLException e) {MyUtils.Logwrite("Ambush.update","Error while deleting ambush GUID="+GUID+". SQLERROR:"+e.toString());return e.toString();}
    }


    public void caravaned(int bonus, Connection con) {
        PreparedStatement query;
        String PGUID,result;
        try {
            query=con.prepareStatement("select PGUID from Ambushes where GUID=?");
            query.setString(1,GUID);
            ResultSet rs=query.executeQuery();
            rs.first();
            PGUID=rs.getString("PGUID");
            query.close();
            rs.close();
            Player player=new Player(PGUID,con);
            //MyUtils.Logwrite("Ambush.caravaned","Owner of ambush detected, PGUID="+PGUID+", preparing to reward him...");
            player.getGold(bonus);
            //MyUtils.Message(PGUID,"Караван ограблен. Вы получили деньги в размере"+Integer.toString(bonus)+" золота!",0,0);
            MyUtils.Message(PGUID,"Чей-то караван попался в вашу засаду "+Name+"! Добыча составила "+Integer.toString(bonus)+" монет!",0,0,Lat,Lng);
        } catch (SQLException e) {
            MyUtils.Logwrite("Ambush.caravaned","Oops, unknown player won't get his money for ambushing caravan because of "+e.toString());
        }
        Life-=1;
        if (Life<=0) {
            MyUtils.Logwrite("Ambush.caravaned", "Preparing to delete ambush GUID=" + GUID);
            result = delete(con);
            MyUtils.Logwrite("Ambush.caravaned", "result=" + result);
        }
        else {
            MyUtils.Logwrite("Ambush.caravaned", "Ambush lost 1 life. GUID=" + GUID);
            result = update(con);
            MyUtils.Logwrite("Ambush.caravaned", "result=" + result);
        }
    }

    public void caravaned(int bonus) {
        String CGUID;
        //PreparedStatement query;
        Player player=new Player(PGUID,con);
        player.getGold(bonus);
        player.drinkAway(bonus);
        /*CGUID=player.getRandomCity();
        if (CGUID.length()>0) {
            City city = new City(CGUID,con);
            city.getGold(bonus,player.Race);
            MyUtils.Logwrite("Ambush.caravaned", "В городе "+city.Name+"("+ CGUID+") разбойники пропили " + bonus + " золота.");
        }
        */
        player.addStat("ambushed",bonus);
        if (!PGUID.equals("Elf")) MyUtils.Message(PGUID,"Чей-то караван попался в вашу засаду "+Name+"! Добыча составила "+Integer.toString(bonus)+" монет!",3,0,Lat,Lng);
        Life-=1;
        if (Life<=0) {
            delete(con);
        }
        else {
            MyUtils.Logwrite("Ambush.caravaned", "Ambush lost 1 life. GUID=" + GUID);
            update(con);
        }
    }

/*    public static void CheckCaravans(Connection con) {
        String aGUID,aPGUID,cGUID,cPGUID;
        int aRadius,aLat,aLng,deltaLat,deltaLng,cLat,cLng;
        PreparedStatement query, query2;
        try {
            query = con.prepareStatement("select z1.GUID,z1.PGUID,z1.Radius,z2.Lat,z2.Lng from Ambushes z1, GameObjects z2 where z1.GUID=z2.GUID");
            ResultSet rs=query.executeQuery();
            while (rs.next()) {
                aGUID=rs.getString("GUID");
                aPGUID=rs.getString("PGUID");
                aRadius=rs.getInt("Radius");
                aLat=rs.getInt("Lat");
                aLng=rs.getInt("Lng");
                deltaLat=(int)Math.acos(aRadius/(2 * 6378137));
                deltaLng=(int)Math.acos(aRadius / (2 * 6378137 * Math.cos(aLat / 1e6 * Math.PI / 180)));
                MyUtils.Logwrite("Ambush.CheckCaravans","deltaLat= "+Integer.toString(deltaLat)+", deltaLng= "+Integer.toString(deltaLng));
                query2 = con.prepareStatement("select GUID,Lat,Lng from GameObjects where abs(Lat-?)<? and abs(Lng-?)<? and Type='Caravan'");
                query2.setInt(1,aLat);
                query2.setInt(2,deltaLat);
                query2.setInt(3,aLng);
                query2.setInt(4,deltaLng);
                ResultSet rs2= query.executeQuery();
                while (rs.next()) {
                    cGUID=rs.getString("GUID");
                    cLat=rs.getInt("Lat");
                    cLng=rs.getInt("Lng");
                    if (MyUtils.RangeCheck(cLat,cLng,aLat,aLng)<=aRadius) {
                        caravan.ambushed();
                        if (ambush.caravaned()) {rs.last();}
                    }
                }


            }

            query.close();
            query = con.prepareStatement("delete from GameObjects where GUID=?");
            query.setString(1, AGUID);
            query.execute();
            query.close();
            jresult.put("Result","OK");
            con.commit();
        } catch (SQLException e) {jresult.put("Error",e.toString());}
    } else {}

    }
*/
}
