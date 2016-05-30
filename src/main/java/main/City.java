package main;

import org.json.simple.JSONObject;

import javax.naming.NamingException;
import javax.persistence.criteria.CriteriaBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Well on 03.02.2016.
 */
public class City {
    String GUID,UpgradeType,Name;
    int Exp,Level, Influence1, Influence2, Influence3;
    Connection con;
    public City (String CGUID,Connection CON) {
        GUID=CGUID;
        con=CON;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select Name,Level,Exp,UpgradeType, Influence1, Influence2, Influence3 from Cities where GUID=?");
            query.setString(1, GUID);
            ResultSet rs=query.executeQuery();
            rs.first();
            Name=rs.getString("Name");
            Level=rs.getInt("Level");
            Exp=rs.getInt("Exp");
            UpgradeType=rs.getString("UpgradeType");
            Influence1=rs.getInt("Influence1");
            Influence2=rs.getInt("Influence2");
            Influence3=rs.getInt("Influence3");
            rs.close();
            query.close();
            //MyUtils.Logwrite("City","Object City "+GUID+" loaded");
        } catch (SQLException e) {
            MyUtils.Logwrite("City","Oops, can't load object "+GUID+". SQL Error: "+e.toString());
        }

    }

    public City (String CGUID, int CExp, int CLevel, String CName, Connection CON) {
        GUID=CGUID;
        Exp=CExp;
        Level=CLevel;
        Name=CName;
        con=CON;
    }

    private boolean canCreateCity(String PGUID, int LAT, int LNG, int mapper, Connection con) {
        //TODO use deltas instead of numbers
        PreparedStatement query;
        ResultSet rs;
        String CName, minName="в чистом поле";
        int TLat,TLng, TRadius, CLevel;
        double minDist=10000, curDist;
        boolean result=true;
        int delta_lat=(int)(1000000*Math.asin((180/3.1415926)*(375-mapper)/(6378137))); //это 375 минус апгрейд картографера метров
        int delta_lng=(int)(1000000*Math.asin((180/3.1415926)*(375-mapper)/(6378137*Math.cos((LAT/1000000)*3.1415926/180)))); //и это 375 минус апгрейд картографера метров
        int delta_lat2=(int)(1000000*Math.asin((180/3.1415926)*(625-mapper)/(6378137))); //это 375 минус апгрейд картографера метров
        int delta_lng2=(int)(1000000*Math.asin((180/3.1415926)*(625-mapper)/(6378137*Math.cos((LAT/1000000)*3.1415926/180)))); //и это 375 минус апгрейд картографера метров
        try {
           query = con.prepareStatement("select z2.Name from GameObjects z1, Cities z2 where z2.GUID=z1.GUID and z2.Creator=? and z1.Type='City' and ? between z1.Lat-? and z1.Lat+? and ? between z1.Lng-? and z1.Lng+?");
            query.setString(1,PGUID);
            query.setInt(2, LAT);
            query.setInt(3,delta_lat2);
            query.setInt(4,delta_lat2);
            query.setInt(5, LNG);
            query.setInt(6,delta_lng2);
            query.setInt(7,delta_lng2);
            rs = query.executeQuery();
            if (rs.first()) {
                        result=false;
                    }
            rs.close();
            query.close();
            if (result) {
                query = con.prepareStatement("select z2.Name from GameObjects z1, Cities z2 where z2.GUID=z1.GUID and z1.Type='City' and ? between z1.Lat-? and z1.Lat+? and ? between z1.Lng-? and z1.Lng+?");
                query.setInt(1, LAT);
                query.setInt(2,delta_lat);
                query.setInt(3,delta_lat);
                query.setInt(4, LNG);
                query.setInt(5,delta_lng);
                query.setInt(6,delta_lng);
                rs = query.executeQuery();
                if (rs.first()) {
                    result=false;
                }
                rs.close();
                query.close();
            }
        } catch (SQLException e) {
            MyUtils.Logwrite("City.canCreateCity", "SQL Error: " + e.toString());
            result=false;
        }
        return result;
    }

    public String createCity(String PGUID, int TLAT, int TLNG) {
        PreparedStatement query;
        int LAT, LNG, rand, mapper=0;
        JSONObject jresult = new JSONObject();
        if (canCreateCity(PGUID, TLAT, TLNG, mapper, con)) {
            try {

                int i=0;
                Random random=new Random();
                String [] upgrades = new String [7];
                    query=con.prepareStatement("select distinct Type from Upgrades");
                    ResultSet rs=query.executeQuery();
                    if (rs.isBeforeFirst()) {
                        while (rs.next()) {
                            i=i+1;
                            upgrades[i-1]=rs.getString(1);
                        }
                        query.close();
                        rs.close();
                    }
                    else {query.close();rs.close();return "Ошибка обращения к базе данных при поиске доступных апгрейдов!";}

                query = con.prepareStatement("INSERT INTO Cities (GUID,Name,UpgradeType, Creator) VALUES(?,?,?,?)");
                query.setString(1, GUID);
                query.setString(2, Generate.genCityName(con));
                query.setString(3, upgrades[random.nextInt(7)]);
                query.setString(4, PGUID);
                query.execute();
                query.close();

                query = con.prepareStatement("INSERT INTO GameObjects(GUID,Lat,Lng,Type)VALUES(?,?,?,'city')");
                query.setString(1, GUID);
                    rand=(int)(Math.random()*2*(125-mapper))-(125-mapper);
                    int delta_lat_rand=(int)(1000000*Math.asin((180/3.1415926)*(rand)/(6378137)));
                    int delta_lng_rand=(int)(1000000*Math.asin((180/3.1415926)*(rand)/(6378137*Math.cos((TLAT/1000000)*3.1415926/180))));
                    LAT=TLAT+delta_lat_rand;
                    LNG=TLNG+delta_lng_rand;
                query.setInt(2, LAT);
                query.setInt(3, LNG);
                query.execute();
                query.close();
                con.commit();
                jresult.put("Result", "OK");
            } catch (SQLException e) {
                jresult.put("Error", "Ошибка взаимодействия с базой данных при установке города.");
                    MyUtils.Logwrite("City.createCity", "PGUID=(" + PGUID + ")" + e.toString());
            }
        } else {
            //jresult.put("Error", "Can't set ambush here. City or another ambush is too close.");
            jresult.put("Error", "Невозможно основать город здесь. Другой город слишком близко!");
        }


        return jresult.toString();
    }

    public String getUpgradeType() {
        return UpgradeType;
    }

    public void bonusCityRecount(float koef) {
        PreparedStatement query;
        try {
            query = con.prepareStatement("update Caravans set bonus=bonus*?, profit=profit*? where Start=? or Finish=?");
            query.setFloat(1, koef);
            query.setFloat(2, koef);
            query.setString(3,GUID);
            query.setString(4,GUID);
            query.execute();
            con.commit();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("City.bonusCityRecount","Failed. City "+Name+". SQL Error: "+e.toString());
        }
    }

    public void getGold(int GOLD, int Race) {
        PreparedStatement query;
        Exp += GOLD;
        if (checkForLevel()) {
            bonusCityRecount((float)Math.sqrt((float)(Level+1)/Level));
            Level += 1;
        }
        if (Race==1) Influence1+=GOLD;
        if (Race==2) Influence2+=GOLD;
        if (Race==3) Influence3+=GOLD;
        try {
            query = con.prepareStatement("update Cities set Exp=?,Level=?, Influence1=?, Influence2=?, Influence3=? where GUID=?");
            query.setInt(1, Exp);
            query.setInt(2, Level);
            query.setInt(3, Influence1);
            query.setInt(4, Influence2);
            query.setInt(5, Influence3);
            query.setString(6, GUID);
            query.execute();
            query.close();
            con.commit();
            if (Influence1+Influence2+Influence3>0) {
                Fraction fraction = new Fraction(1, con);
                fraction.getGold((int)(GOLD * Math.pow(Influence1/(Influence1+Influence2+Influence3),2)), con);
                fraction = new Fraction(2, con);
                fraction.getGold((int)(GOLD * Math.pow(Influence2/(Influence1+Influence2+Influence3),2)), con);
                fraction = new Fraction(3, con);
                fraction.getGold((int)(GOLD * Math.pow(Influence3/(Influence1+Influence2+Influence3),2)), con);
            }
            //MyUtils.Logwrite("City.getGold","City "+Name+" got "+GOLD+" gold!");
        } catch (SQLException e) {
            MyUtils.Logwrite("City.getGold","Oops, no coins for "+GUID+". SQL Error: "+e.toString());
        }

    }

    public void getGold(int GOLD) {
        PreparedStatement query;
        Exp += GOLD;
        if (checkForLevel()) {
            Level += 1;
        }
        try {
            query = con.prepareStatement("update Cities set Exp=?,Level=? where GUID=?");
            query.setInt(1, Exp);
            query.setInt(2, Level);
            query.setString(3, GUID);
            query.execute();
            query.close();
            con.commit();
            if (Influence1+Influence2+Influence3>0) {
                Fraction fraction = new Fraction(1, con);
                fraction.getGold((int)(GOLD * Math.pow(Influence1/(Influence1+Influence2+Influence3),2)), con);
                fraction = new Fraction(2, con);
                fraction.getGold((int)(GOLD * Math.pow(Influence2/(Influence1+Influence2+Influence3),2)), con);
                fraction = new Fraction(3, con);
                fraction.getGold((int)(GOLD * Math.pow(Influence3/(Influence1+Influence2+Influence3),2)), con);
            }
            //MyUtils.Logwrite("City.getGold","City "+Name+" got "+GOLD+" gold!");
        } catch (SQLException e) {
            MyUtils.Logwrite("City.getGold","Oops, no coins for "+GUID+". SQL Error: "+e.toString());
        }

    }

    private boolean checkForLevel() {
        return (Exp>=getTNL());
    }

    private int getTNL() {
        PreparedStatement query;
        int TNL;
        try {
            query = con.prepareStatement("select exp from Levels where Type='city' and level=?");
            query.setInt(1, Level + 1);
            ResultSet rs = query.executeQuery();
            rs.first();
            TNL = rs.getInt(1);
            rs.close();
            query.close();
        } catch (SQLException e) {TNL=99999999;}
        return TNL;
    }

}
