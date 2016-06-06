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
    int Exp,Level, Influence1, Influence2, Influence3, Hirelings;
    Connection con;
    public City (String CGUID,Connection CON) {
        GUID=CGUID;
        con=CON;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select Name,Level,Exp,UpgradeType, Influence1, Influence2, Influence3, Hirelings from Cities where GUID=?");
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
            Hirelings=rs.getInt("Hirelings");
            rs.close();
            query.close();
            //MyUtils.Logwrite("City","Object City "+GUID+" loaded");
        } catch (SQLException e) {
            MyUtils.Logwrite("City","Oops, can't load object "+GUID+". SQL Error: "+e.toString());
        }

    }

/*    public City (String CGUID, int CExp, int CLevel, String CName, Connection CON) {
        GUID=CGUID;
        Exp=CExp;
        Level=CLevel;
        Name=CName;
        con=CON;
    }
*/

    public void update() {
        PreparedStatement query;
        try {
            query = con.prepareStatement("update Cities set Exp=?,Level=?, Influence1=?, Influence2=?, Influence3=?, Hirelings=? where GUID=?");
            query.setInt(1, Exp);
            query.setInt(2, Level);
            query.setInt(3, Influence1);
            query.setInt(4, Influence2);
            query.setInt(5, Influence3);
            query.setInt(6, Hirelings);
            query.setString(7, GUID);
            query.execute();
            query.close();
            con.commit();
        } catch (SQLException e) {
            MyUtils.Logwrite("City.update","Failed city "+Name+" update. SQL Error: "+e.toString());
        }
    }

    private boolean canCreateCity(String PGUID, int LAT, int LNG, int mapper, Connection con) {
        //TODO use deltas instead of numbers
        PreparedStatement query;
        ResultSet rs;
        String CName, minName="в чистом поле";
        int TLat,TLng, TRadius, CLevel;
        int minDist=250+mapper;
        int minSelfDist=500+mapper;
        boolean result=true;
        try {
            query = con.prepareStatement("select z2.Name,z1.Lat,z1.Lng,z2.Creator from GameObjects z1, Cities z2 where z2.GUID=z1.GUID and z1.Type='City' and z2.Creator=? and round(6378137 * acos(cos(z1.Lat / 1e6 * PI() / 180) * cos(? / 1e6 * PI() / 180) * cos(z1.Lng / 1e6 * PI() / 180 - ? / 1e6 * PI() / 180) + sin(z1.Lat / 1e6 * PI() / 180) * sin(? / 1e6 * PI() / 180)))<=?");
            query.setString(1,PGUID);
            query.setInt(2, LAT);
            query.setInt(3, LNG);
            query.setInt(4, LAT);
            query.setInt(5,minSelfDist);
            rs = query.executeQuery();
            if (rs.first()) {
                        result=false;
                    }
            rs.close();
            query.close();
            if (result) {
                query = con.prepareStatement("select z2.Name,z1.Lat,z1.Lng,z2.Creator from GameObjects z1, Cities z2 where z2.GUID=z1.GUID and z1.Type='City' and round(6378137 * acos(cos(z1.Lat / 1e6 * PI() / 180) * cos(? / 1e6 * PI() / 180) * cos(z1.Lng / 1e6 * PI() / 180 - ? / 1e6 * PI() / 180) + sin(z1.Lat / 1e6 * PI() / 180) * sin(? / 1e6 * PI() / 180)))<=?");
                query.setInt(1, LAT);
                query.setInt(2, LNG);
                query.setInt(3, LAT);
                query.setInt(4,minDist);
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

    public void createKvantCity(String PGUID, int TLAT, int TLNG, String Name) {
        PreparedStatement query;
        String CUpgradeType,CUName;
        int LAT, LNG, r;
        int randLat,randLng,maxRandLng,minRandLng;

        randLat=(int)(Math.random()*20000)-10000;
        maxRandLng=(int)Math.sqrt(10000*10000-randLat*randLat);
        if (Math.abs(randLat)<1000) {minRandLng=(int)Math.sqrt(1000*1000-randLat*randLat);}
        else minRandLng=0;
        randLng= (int)Math.signum((Math.random()*20000-10000))*(minRandLng+(int)(Math.random()*(maxRandLng-minRandLng)));
        int delta_lat_rand=(int)(1000000*Math.asin((180/3.1415926)*(randLat)/(6378137)));
        int delta_lng_rand=(int)(1000000*Math.asin((180/3.1415926)*(randLng)/(6378137*Math.cos((TLAT/1000000)*3.1415926/180))));
        LAT=TLAT+delta_lat_rand;
        LNG=TLNG+delta_lng_rand;

        if (canCreateCity(PGUID, LAT, LNG, 0, con)) {
            try {
                int i=0;
                Random random=new Random();
                String [] upgrades = new String [7];
                //String [] upnames = new String [8];
                query=con.prepareStatement("select Type from Upgrades where level=0");
                ResultSet rs=query.executeQuery();
                if (rs.isBeforeFirst()) {
                    while (rs.next()) {
                        i=i+1;
                        upgrades[i-1]=rs.getString("Type");
                        //upnames[i-1]=rs.getString("Name");
                    }
                    query.close();
                    rs.close();
                }
                else {query.close();rs.close();return;}

                query = con.prepareStatement("INSERT INTO Cities (GUID,Name,UpgradeType, Creator, Kvant) VALUES(?,?,?,?,?)");
                String kvantGUID=UUID.randomUUID().toString();
                query.setString(1, kvantGUID);
                String CName = new StringBuffer(Name).reverse().toString();
                String CityName = CName.substring(0,1).toUpperCase()+CName.substring(1).toLowerCase();
                query.setString(2, CityName);
                r=random.nextInt(7);
                CUpgradeType=upgrades[r];
                //CUName=upnames[r];
                query.setString(3, CUpgradeType);
                query.setString(4, PGUID);
                query.setBoolean(5,true);
                query.execute();
                query.close();
                MyUtils.Logwrite("City.createKvantCity", "Создан квантовый город "+CityName+" GUID=(" + GUID + ")");

                query = con.prepareStatement("INSERT INTO GameObjects(GUID,Lat,Lng,Type)VALUES(?,?,?,'City')");
                query.setString(1, kvantGUID);
                query.setInt(2, LAT);
                query.setInt(3, LNG);
                query.execute();
                query.close();
                con.commit();
            } catch (SQLException e) {
                MyUtils.Logwrite("City.createKvantCity", "PGUID=(" + PGUID + ")" + e.toString());
            }
        } else {
            //jresult.put("Error", "Can't set ambush here. City or another ambush is too close.");
            MyUtils.Logwrite("City.createKvantCity", "Квантовому городу не повезло, другой город был рядом. PGUID=(" + PGUID + ")");
        }
    }


    public String createCity(String PGUID, int TLAT, int TLNG, int mapper) {
        PreparedStatement query;
        String CName, CUpgradeType,CUName;
        int LAT, LNG, r, dist;
        int randLat,randLng,maxRandLng;
        JSONObject jresult = new JSONObject();
        JSONObject jobj = new JSONObject();
        if (canCreateCity(PGUID, TLAT, TLNG, mapper, con)) {
            try {

                int i=0;
                Random random=new Random();
                String [] upgrades = new String [7];
                String [] upnames = new String [7];
                    query=con.prepareStatement("select Type,Name from Upgrades where level=0");
                    ResultSet rs=query.executeQuery();
                    if (rs.isBeforeFirst()) {
                        while (rs.next()) {
                            i=i+1;
                            upgrades[i-1]=rs.getString("Type");
                            upnames[i-1]=rs.getString("Name");
                        }
                        query.close();
                        rs.close();
                    }
                    else {query.close();rs.close(); jresult.put("Result","BD001");jresult.put("Message","Ошибка обращения к БД"); return jresult.toString();}

                query = con.prepareStatement("INSERT INTO Cities (GUID,Name,UpgradeType, Creator) VALUES(?,?,?,?)");
                query.setString(1, GUID);
                CName=Generate.genCityName(con);
                query.setString(2, CName);
                r=random.nextInt(7);
                CUpgradeType=upgrades[r];
                CUName=upnames[r];
                query.setString(3, CUpgradeType);
                query.setString(4, PGUID);
                query.execute();
                query.close();

                query = con.prepareStatement("INSERT INTO GameObjects(GUID,Lat,Lng,Type)VALUES(?,?,?,'City')");
                query.setString(1, GUID);
                randLat=(int)(Math.random()*2*mapper)-mapper;
                maxRandLng=(int)Math.sqrt(mapper*mapper-randLat*randLat);
                randLng=(int) (Math.random()*2*maxRandLng)-maxRandLng;
                int delta_lat_rand=(int)(1000000*Math.asin((180/3.1415926)*(randLat)/(6378137)));
                int delta_lng_rand=(int)(1000000*Math.asin((180/3.1415926)*(randLng)/(6378137*Math.cos((TLAT/1000000)*3.1415926/180))));
                LAT=TLAT+delta_lat_rand;
                LNG=TLNG+delta_lng_rand;
                query.setInt(2, LAT);
                query.setInt(3, LNG);
                query.execute();
                query.close();
                con.commit();
                jresult.put("Result", "OK");
                jobj.put("GUID", GUID);
                jobj.put("Type", "City");
                jobj.put("Lat", LAT);
                jobj.put("Lng", LNG);
                jobj.put("Name", CName);
                jobj.put("Level", 1);
                jobj.put("Progress",0);
                jobj.put("UpgradeType",CUpgradeType);
                jobj.put("UpgradeName",CUName);
                jobj.put("Radius",100);
                jobj.put("Influence1",0);
                jobj.put("Influence2",0);
                jobj.put("Influence3",0);
                jobj.put("Owner",true);
                jobj.put("Hirelings",100);
                jobj.put("Creator",Name);
                jresult.put("City",jobj);
                dist=(int)MyUtils.RangeCheck(LAT,LNG,TLAT,TLNG);
                jresult.put("Message","Город успешно основан в "+dist+" метрах от запланированного места");
                createKvantCity(PGUID,LAT,LNG,CName);
            } catch (SQLException e) {
                jresult.put("Result","BD001");
                jresult.put("Message", "Ошибка взаимодействия с базой данных при установке города.");
                    MyUtils.Logwrite("City.createCity", "PGUID=(" + PGUID + ")" + e.toString());
            }
        } else {
            //jresult.put("Error", "Can't set ambush here. City or another ambush is too close.");
            jresult.put("Result","O1202");
            jresult.put("Message", "Невозможно основать город здесь. Другой город слишком близко!");
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
            bonusCityRecount((float)Math.sqrt((float)(Level+1)/Level));
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
        } catch (SQLException e) {TNL=999999999;}
        return TNL;
    }

}
