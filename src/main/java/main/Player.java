package main;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.naming.NamingException;
//import javax.resource.cci.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;



/**
 * Created by Well on 17.01.2016.
 * Player object
 */
public class Player {
    String LastError;
    String GUID = "";
    String Name = "";
    int Level = 0;
    int Exp = 0;
    int Race;
    int Gold = 0;
    int Lat = 100;
    int Lng = 200;
    JSONObject jresult = new JSONObject();
    JSONArray jarr = new JSONArray();
    String result = "";
    Connection con = null;
    Runtime r = Runtime.getRuntime();


    public Player (String PGUID) {
        GUID=PGUID;
        try {
            con = DBUtils.ConnectDB();
        } catch (SQLException e) {
            result = MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        } catch (NamingException e) {
            result = MyUtils.getJSONError("ResourceError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        }
        if (!result.equals("")) result = "No access to DB: " + result;
    }
    public Player() {
        try {
            con = DBUtils.ConnectDB();
        } catch (SQLException e) {
            result = MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        } catch (NamingException e) {
            result = MyUtils.getJSONError("ResourceError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        }
        if (!result.equals("")) result = "No access to DB: " + result;
    }

    public Player(String PGUID, Connection CON) {
        PreparedStatement query;
        con = CON;
        GUID = PGUID;
        try {
            query = con.prepareStatement("select Name, Level, Exp, Gold, Race from Players where GUID=?");
            query.setString(1, GUID);
            ResultSet rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                Name = rs.getString("Name");
                Level = rs.getInt("Level");
                Exp = rs.getInt("Exp");
                Gold = rs.getInt("Gold");
                Race = rs.getInt("Race");
            } else {
                MyUtils.Logwrite("Player","Player "+GUID+" not found");
            }
            query.close();
            rs.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("Player",e.toString());
        }
    }

    public Player(String Token, int PLAT, int PLNG) {
        //TODO: handle OldLat,OldLng/Lat,Lng
        MyUtils.Logwrite("Player","создаем игрока для "+Token);
        int OldLat, OldLng;
        PreparedStatement query;
        ResultSet rs;
        Lat = PLAT;
        Lng = PLNG;
        try {
            con = DBUtils.ConnectDB();
        } catch (SQLException e) {
            result = MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        } catch (NamingException e) {
            result = MyUtils.getJSONError("ResourceError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        }
        if (!result.equals("")) result = "No access to DB: " + result;

        try {
            query = con.prepareStatement("select z1.GUID, z1.Name, z1.Level, z1.Exp, z1.Gold, z1.Race, z2.Lat, z2.Lng from Connections z0, Players z1, GameObjects z2 where z0.Token=? and z0.PGUID=z1.GUID and z2.GUID=z1.GUID");
            query.setString(1, Token);
            rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                GUID = rs.getString("GUID");
                Name = rs.getString("Name");
                Level = rs.getInt("Level");
                Exp = rs.getInt("Exp");
                Gold = rs.getInt("Gold");
                Race = rs.getInt("Race");
                OldLat = rs.getInt("Lat");
                OldLng = rs.getInt("Lng");
                rs.close();
            } else {
                LastError = MyUtils.getJSONError("NOUSERFOUND", "(" + Token + ")");
            }
            query.close();
        } catch (SQLException e) {
            LastError = MyUtils.getJSONError("DBError", e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    public String setRace(int race) {
        Race=race;
        update();
        return jresult.toString();
    }

    public void addStat(String columnC, int valueV) {

        MyUtils.Logwrite("Player.addStat","Start! "+Name+". column = "+columnC+". value = "+Integer.toString(valueV));
        PreparedStatement query;
        try {
            String tempStr="update Stats set " + columnC + " = " + columnC + " + ? where PGUID = ?";
            query=con.prepareStatement(tempStr);
            query.setInt(1,valueV);
            query.setString(2,GUID);
            //MyUtils.Logwrite("Player.addStat","tempStr = "+tempStr+", query="+query.toString());
            query.execute();
            con.commit();
            query.close();
            MyUtils.Logwrite("Player.addStat",Name+". column = "+columnC+". value = "+Integer.toString(valueV));
        } catch (SQLException e) {
            MyUtils.Logwrite("Player.addStat",Name+". Error "+e.toString());
        }

    }

    public String register(String Login, String Password) {
        PreparedStatement query;

        try {
            query=con.prepareStatement("select GUID from Users where Login=? and Password=?");
            query.setString(1,Login);
            query.setString(2,Password);
            ResultSet rs = query.executeQuery();
            rs.first();
            GUID=rs.getString("GUID");
            rs.close();
            query=con.prepareStatement("insert into Players (GUID, Name, Level, Exp, Gold,Class, Race) values (?,?,1,0,0,0,0)");
            query.setString(1,GUID);
            query.setString(2,Login);
            query.execute();
            query=con.prepareStatement("insert into Stats (PGUID) values (?)");
            query.setString(1,GUID);
            query.execute();
            query=con.prepareStatement("insert into GameObjects (GUID, Lat, Lng, Type) values (?,100,100,'Player')");
            query.setString(1,GUID);
            query.execute();
            query.close();
            generateStartUpgrades();
            con.commit();

            return Login + " succesfully registered!";
        } catch (SQLException e)
        {
            return "SQL Error while registration: "+e.toString();
        }

    }

    public String userRegister(String Login, String Password, String EMail, String Invite) {
        PreparedStatement query;
        String UGUID=UUID.randomUUID().toString();
        String ret;
        try {
            query=con.prepareStatement("select count(1) from Invites where used=0 and Invite=?");
            query.setString(1,Invite);
            ResultSet rs=query.executeQuery();
            rs.first();
            if (rs.getInt(1)>0) {
                query=con.prepareStatement("update Invites set used=1 where Invite=?");
                query.setString(1,Invite);
                query.execute();
                query = con.prepareStatement("insert into Users (GUID, Login, Password, email) values (?,?,?,?)");
                query.setString(1, UGUID);
                query.setString(2, Login.trim());
                query.setString(3, Password.trim());
                query.setString(4, EMail.trim());
                query.execute();
                con.commit();
                ret=register(Login, Password);
            } else {ret="Try another invite, this one don't work!";}
            rs.close();
            query.close();
            con.close();
            return ret;
        } catch (SQLException e)
        {
            return "SQL Error while registration: "+e.toString();
        }
    }

    public String getInvite(String Login, String Password) {
        MyUtils.Logwrite("getInvite","Запрошен инвайт");
        PreparedStatement query;
        String Invite,ret;
        try {
            query=con.prepareStatement("select Invite from Invites where given=0 and used=0");
            ResultSet rs=query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                Invite=rs.getString("Invite");
                query=con.prepareStatement("update Invites set given=1 where Invite=?");
                query.setString(1,Invite);
                query.execute();
                con.commit();
                ret=Invite;
            } else {ret="No more ungiven invites!";}
            query.close();
            rs.close();
        } catch(SQLException e) {ret=e.toString();}
        return ret;
    }


    public boolean generateStartUpgrades() {
        PreparedStatement query, query2, query3;
        String UGUID;
        try {
            query = con.prepareStatement("select GUID from Upgrades WHERE level=0");
            ResultSet rs = query.executeQuery();
            while (rs.next()) {
                    UGUID=rs.getString("GUID");
                    query = con.prepareStatement("insert into PUpgrades(PGUID,UGUID) values(?,?) ");
                    query.setString(1, GUID);
                    query.setString(2, UGUID);
                    query.execute();
                }
            query.close();
            rs.close();
            return true;
        } catch (SQLException e) { MyUtils.Logwrite("Player.generateStartUpgrades","Error: "+e.toString());return false; }
    }


    public void getGold(int GOLD) {
        PreparedStatement query;
        Exp += GOLD;
        Gold += GOLD;
        if (checkForLevel()) {
            Level += 1;
        }
        try {
            query = con.prepareStatement("update Players set Exp=?,Gold=?,Level=? where GUID=?");
            query.setInt(1, Exp);
            query.setInt(2, Gold);
            query.setInt(3, Level);
            query.setString(4, GUID);
            query.execute();
            query.close();
            con.commit();
            //MyUtils.Logwrite("Player.getGold","Grats with "+GOLD+" gold, "+Name+"!");
        } catch (SQLException e) {
            MyUtils.Logwrite("Player.getGold","Oops, no coins "+e.toString());
        }
        Fraction fraction = new Fraction(Race,con);
        fraction.getGold(GOLD,con);
    }

    private boolean checkForLevel() {
        return (Exp>=getTNL());
    }


    public void update() {
        PreparedStatement query;
        try {
            query = con.prepareStatement("update Players set Level=?, Exp=?, Gold=?, Race=? where GUID=?");
            query.setInt(1,Level);
            query.setInt(2,Exp);
            query.setInt(3,Gold);
            query.setInt(4,Race);
            query.setString(5,GUID);
            query.execute();
            con.commit();
            query.close();
            jresult.put("Result","OK");
        } catch (SQLException e) {
            MyUtils.Logwrite("Player.update","Failed player "+Name+" update. SQL Error: "+e.toString());
            jresult.put("Error","Ошибка обновления данных игрока: "+e.toString());
        }
    }

    public void UpdatePUpgrades(String UGUIDold, String UGUIDnew) {
        PreparedStatement query;
        try {
            query = con.prepareStatement("update PUpgrades set UGUID=? where PGUID=? and UGUID=?");
            query.setString(1, UGUIDnew);
            query.setString(2, GUID);
            query.setString(3,UGUIDold);
            query.execute();
            con.commit();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("Player.UpdatePUpgrades","Failed. Player "+Name+" UGUID old = "+UGUIDold+" UGUID new = "+UGUIDnew+". SQL Error: "+e.toString());
        }
    }

    public void bonusUpgradeRecount(float koef) {
        PreparedStatement query;
        try {
            query = con.prepareStatement("update Caravans set bonus=bonus*?, profit=profit*? where PGUID=?");
            query.setFloat(1, koef);
            query.setFloat(2, koef);
            query.setString(3,GUID);
            query.execute();
            con.commit();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("Player.bonusUpgradeRecount","Failed. Player "+Name+". SQL Error: "+e.toString());
        }
    }

    public void profitUpgradeRecount(int newacc, int newspeed) {
        PreparedStatement query;
        try {
            query = con.prepareStatement("Update Caravans z1 set z1.profit=60*z1.bonus/((?-1-?)/?+(z1.Distance-((1+?)*(?-1-?)/?+POW((?-1-?)/?,2)*?/2))/?) WHERE PGUID = ?");
            query.setInt(1, newspeed);
            query.setInt(2, newacc);
            query.setInt(3, newacc);
            query.setInt(4, newacc);
            query.setInt(5, newspeed);
            query.setInt(6, newacc);
            query.setInt(7, newacc);
            query.setInt(8, newspeed);
            query.setInt(9, newacc);
            query.setInt(10, newacc);
            query.setInt(11, newacc);
            query.setInt(12, newspeed);
            query.setString(13,GUID);
            query.execute();
            con.commit();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("Player.profitUpgradeRecount","Failed. Player "+Name+". SQL Error: "+e.toString());
        }
    }


    public String BuyUpgrade(String CGUID) {
        MyUtils.Logwrite("BuyUpgrade","Started by "+Name, r.freeMemory());
        String ret;
        String flag="nothing";
        int upcost;
        float RaceDiscount, RaceBonus;
        if (checkRangeToObj(CGUID)) {
            City city = new City(CGUID, con);
            Upgrade currentUpgrade = new Upgrade(GUID, CGUID, con);
            if (currentUpgrade.GUID.equals("0")) {
                jresult.put("Error", "Техническая ошибка 20001, обратитесь в службу моральной поддержки!");
                MyUtils.Logwrite("BuyUpgrade","Can't create object targetUpgrade " + currentUpgrade.result);
                return jresult.toString();
            }
            Upgrade targetUpgrade = new Upgrade(currentUpgrade.Type, currentUpgrade.Level + 1, con);
            if (targetUpgrade.GUID.equals("0")) {
                MyUtils.Logwrite("BuyUpgrade","Can't create object targetUpgrade " + targetUpgrade.result);
                jresult.put("Error", "Техническая ошибка 20002, обратитесь в службу моральной поддержки!");
                return jresult.toString();
            }
            if (Level >= targetUpgrade.ReqPlayerLev) {
                if (city.Level >= targetUpgrade.ReqCityLev) {
                    RaceBonus=0;
                    if (city.Influence1+city.Influence2+city.Influence3>0) {
                        flag="sum inf ok.";
                        if (Race==1) {RaceBonus=(float)city.Influence1/(city.Influence1+city.Influence2+city.Influence3);flag=flag+" race1 ok.";}
                        if (Race==2) {RaceBonus=(float)city.Influence2/(city.Influence1+city.Influence2+city.Influence3);flag=flag+" race2 ok.";}
                        if (Race==3) {RaceBonus=(float)city.Influence3/(city.Influence1+city.Influence2+city.Influence3);flag=flag+" race3 ok.";}
                    }
                    RaceDiscount=1-RaceBonus/4;
                    upcost=(int)(targetUpgrade.Cost*RaceDiscount*(100-getPlayerUpgradeEffect1("bargain"))/100);
                    MyUtils.Logwrite("Player.BuyUpgrade","Player="+Name+", Race="+Race+", Inf1="+city.Influence1+",Inf2="+city.Influence2+",Inf3="+city.Influence3+", RaceBonus="+Float.toString(RaceBonus)+", RaceDiscount="+Float.toString(RaceDiscount)+", upcost="+upcost+", flag="+flag);
                    if (Gold >= upcost) {
                        Gold -= upcost;
                        UpdatePUpgrades(currentUpgrade.GUID, targetUpgrade.GUID);
                        update();
                        if (targetUpgrade.Type.equals("cargo")) {bonusUpgradeRecount(targetUpgrade.Effect1/currentUpgrade.Effect1);}
                        if (targetUpgrade.Type.equals("speed")) {profitUpgradeRecount(targetUpgrade.Effect1,targetUpgrade.Effect2);}
                        city.getGold(upcost/10);
                        targetUpgrade.update(GUID, con);
                        jresult.put("Result","OK");
                        ret = jresult.toString();
                    } else {
                        jresult.put("Error", "Тебе не хватает золота на покупку этого умения");
                        ret = jresult.toString();
                    }
                } else {
                    jresult.put("Error", "Этот город слишком мал, в нем некому обучить тебя умению "+targetUpgrade.Level+" уровня. Требуемый уровень города - "+targetUpgrade.ReqCityLev);
                    ret = jresult.toString();
                }
            } else {
                jresult.put("Error", "Твой уровень слишком мал для приобретения этого умения!");
                ret = jresult.toString();
            }
        } else {
            jresult.put("Error", "Ты слишком далеко.");
            ret = jresult.toString();
        }
        MyUtils.Logwrite("BuyUpgrade","Finished by "+Name, r.freeMemory());
        return ret;
    }

    private int getTNL() {
        PreparedStatement query;
        int TNL;
        try {
            query = con.prepareStatement("select exp from Levels where Type='player' and level=?");
            query.setInt(1, Level + 1);
            ResultSet rs = query.executeQuery();
            rs.first();
            TNL = rs.getInt(1);
            rs.close();
            query.close();
        } catch (SQLException e) {TNL=99999999;}
        return TNL;
    }

    public String GetPlayerInfo() {
        MyUtils.Logwrite("GetPlayerInfo","Started by "+Name, r.freeMemory());
        int TNL, Caravans,Ambushes,AmbushesMax,AmbushesLeft,UpLevel,Distance,AmbLat,AmbLng,AmbushRadius,ActionDistance,LatS,LngS,LatF,LngF,UpCost,UpReqCityLevel, profit;
        String UpType,UpName,UpDesc,CarGUID,StartGUID,StartName,FinishGUID,FinishName,AmbGUID,AmbName;
        ResultSet rs;
        JSONArray jarr2 = new JSONArray();
        if (GUID.equals("")) {jresult.put("Error","No player found."); return jresult.toString();}

        jresult.put("GUID",GUID);
        jresult.put("PlayerName",Name);
        jresult.put("Level",Level);
        jresult.put("Exp",Exp);
        jresult.put("Gold",Gold);
        jresult.put("Race",Race);
        PreparedStatement query;
        try {
            TNL = getTNL() - Exp;
            jresult.put("TNL",TNL);
            query=con.prepareStatement("select count(1) from Caravans where Finish is not null and PGUID=?");
            query.setString(1, GUID);
            rs = query.executeQuery();
            rs.first();
            Caravans = rs.getInt(1);
            rs.close();
            jresult.put("Caravans",Caravans);
            query=con.prepareStatement("select count(1) from Ambushes where PGUID=?");
            query.setString(1, GUID);
            rs = query.executeQuery();
            rs.first();
            Ambushes = rs.getInt(1);
            rs.close();

            AmbushesMax=getPlayerUpgradeEffect1("set_ambushes");
            AmbushesLeft=AmbushesMax-Ambushes;
            jresult.put("AmbushesMax",AmbushesMax);
            jresult.put("AmbushesLeft",AmbushesLeft);
            jresult.put("MostIn",0);
            AmbushRadius=getPlayerUpgradeEffect1("ambushes");
            jresult.put("AmbushRadius",AmbushRadius);
            ActionDistance=getPlayerUpgradeEffect1("paladin");
            jresult.put("ActionDistance",ActionDistance);
            query=con.prepareStatement("select z1.Type,z1.Name,z1.Description,z1.Level, z1.Effect1,z1.Effect2 from Upgrades z1, PUpgrades z2 where z2.PGUID=? and z2.UGUID=z1.GUID order by z1.Level desc");
            query.setString(1, GUID);
            rs = query.executeQuery();
            while (rs.next()) {
                JSONObject jobj = new JSONObject();
                UpType=rs.getString("Type");
                UpName=rs.getString("Name");
                UpDesc=rs.getString("Description");
                UpLevel=rs.getInt("Level");
                jobj.put("Type", UpType);
                jobj.put("Name", UpName);
                jobj.put("Description", UpDesc);
                jobj.put("Level", UpLevel);
                jobj.put("Effect1",rs.getInt("Effect1"));
                jobj.put("Effect2",rs.getInt("Effect2"));
                jarr.add(jobj);
            }
            rs.close();
            jresult.put("Upgrades",jarr);

            query=con.prepareStatement("select z3.Type,z3.Name,z3.ChangedDesc as Description,z3.Level, z3.ReqCityLev, z3.Cost from Upgrades z3 where (z3.Type,z3.Level) in (select z1.Type,z1.Level+1 from Upgrades z1, PUpgrades z2 where z2.PGUID=? and z2.UGUID=z1.GUID)");
            query.setString(1, GUID);
            rs = query.executeQuery();
            while (rs.next()) {
                JSONObject jobj = new JSONObject();
                UpType=rs.getString("Type");
                UpName=rs.getString("Name");
                UpDesc=rs.getString("Description");
                UpLevel=rs.getInt("Level");
                UpCost=rs.getInt("Cost");
                UpReqCityLevel=rs.getInt("ReqCityLev");
                jobj.put("Type", UpType);
                jobj.put("Name", UpName);
                jobj.put("Description", UpDesc);
                jobj.put("Level", UpLevel);
                jobj.put("ReqCityLev", UpReqCityLevel);
                jobj.put("Cost",UpCost);
                jarr2.add(jobj);
            }
            rs.close();
            jresult.put("NextUpgrades",jarr2);


            jarr2 = new JSONArray();
            //query=con.prepareStatement("select z1.GUID,z1.Start,(select Name from Cities z2 where z2.GUID=z1.Start) StartName,z1.Finish,(select Name from Cities z2 where z2.GUID=z1.Finish) FinishName,z1.Distance from Caravans z1 where PGUID=?");
            query=con.prepareStatement("select z1.GUID,z2.Lat,z2.Lng,z1.Start,(select Name from Cities z2 where z2.GUID=z1.Start) StartName,(select Lat from GameObjects where GUID=z1.Start) as LatS,(select Lng from GameObjects where GUID=z1.Start) as LngS,z1.Finish,(select Name from Cities z2 where z2.GUID=z1.Finish) FinishName,(select Lat from GameObjects where GUID=z1.Finish) as LatF,(select Lng from GameObjects where GUID=z1.Finish) as LngF,z1.Distance, z1.profit from Caravans z1 left join GameObjects z2 on (z1.GUID=z2.GUID) where z1.PGUID=?");
            query.setString(1, GUID);
            rs = query.executeQuery();
            while (rs.next()) {
                JSONObject jobj = new JSONObject();
                CarGUID=rs.getString("GUID");
                StartGUID=rs.getString("Start");
                StartName=rs.getString("StartName");
                FinishGUID=rs.getString("Finish");
                FinishName=rs.getString("FinishName");
                Distance=rs.getInt("Distance");
                profit=rs.getInt("profit");
                LatS=rs.getInt("LatS");
                LngS=rs.getInt("LngS");
                LatF=rs.getInt("LatF");
                LngF=rs.getInt("LngF");
                jobj.put("GUID", CarGUID);
                jobj.put("Lat",rs.getInt("Lat"));
                jobj.put("Lng",rs.getInt("Lng"));
                jobj.put("StartGUID", StartGUID);
                jobj.put("StartName", StartName);
                jobj.put("FinishGUID", FinishGUID);
                jobj.put("FinishName", FinishName);
                jobj.put("Distance", Distance);
                jobj.put("profit", profit);
                jobj.put("StartLat",LatS);
                jobj.put("StartLng",LngS);
                jobj.put("FinishLat",LatF);
                jobj.put("FinishLng",LngF);
                jarr2.add(jobj);
            }
            rs.close();
            jresult.put("Routes",jarr2);

            jarr2 = new JSONArray();
            query=con.prepareStatement("select z1.GUID,z1.Lat,z1.Lng,z2.Name from GameObjects z1, Ambushes z2 where z2.GUID=z1.GUID and z2.PGUID=?");
            query.setString(1, GUID);
            rs = query.executeQuery();
            while (rs.next()) {
                JSONObject jobj = new JSONObject();
                AmbGUID=rs.getString("GUID");
                AmbLat=rs.getInt("Lat");
                AmbLng=rs.getInt("Lng");
                AmbName="Засада "+rs.getString("Name");
                jobj.put("GUID", AmbGUID);
                jobj.put("Name", AmbName);
                jobj.put("Lat", AmbLat);
                jobj.put("Lng", AmbLng);
                jarr2.add(jobj);
            }
            rs.close();
            query.close();
            jresult.put("Ambushes",jarr2);
        } catch (SQLException e) {
            jresult.put("Error",e.toString());
        }
        MyUtils.Logwrite("GetPlayerInfo","Finished by "+Name, r.freeMemory());
        return jresult.toString();
    }

    public boolean CheckAmbushesQuantity() {
        //select z1.Effect1-(select count(1) from Ambushes where PGUID=?) from Upgrades z1, PUpgrades z2 where z2.UGUID=z1.GUID and z2.PGUID=? and z1.TYPE='set_ambushes'
        //if (result>0) return true
        //else return false
        PreparedStatement query;
        ResultSet rs;
        int AmbLeft;
        String result;
        boolean ret;
        try {
            query = con.prepareStatement("select z1.Effect1-(select count(1) from Ambushes where PGUID=?) from Upgrades z1, PUpgrades z2 where z2.UGUID=z1.GUID and z2.PGUID=? and z1.TYPE='set_ambushes'");
            query.setString(1, GUID);
            query.setString(2, GUID);
            rs = query.executeQuery();
            rs.first();
            AmbLeft = rs.getInt(1);
            rs.close();
            query.close();
            ret = (AmbLeft > 0);
        } catch (SQLException e) {
            ret = false;
        }
        return ret;
    }

    public boolean CheckCitiesQuantity() {
        //select z1.Effect1-(select count(1) from Ambushes where PGUID=?) from Upgrades z1, PUpgrades z2 where z2.UGUID=z1.GUID and z2.PGUID=? and z1.TYPE='set_ambushes'
        //if (result>0) return true
        //else return false
        PreparedStatement query;
        ResultSet rs;
        int CitLeft;
        String result;
        boolean ret;
        try {
            query = con.prepareStatement("select z1.Effect1-(select count(1) from Cities where Creator=?) from Upgrades z1, PUpgrades z2 where z2.UGUID=z1.GUID and z2.PGUID=? and z1.TYPE='founder'");
            query.setString(1, GUID);
            query.setString(2, GUID);
            rs = query.executeQuery();
            rs.first();
            CitLeft = rs.getInt(1);
            rs.close();
            query.close();
            ret = (CitLeft > 0);
        } catch (SQLException e) {
            ret = false;
        }
        return ret;
    }

    public String ScanRange() {
        Random random = new Random();

        PreparedStatement query, query2;
        ResultSet rs;
        String TPGUID, CName, CUpgradeType, Start, Finish, StartName, FinishName, CUName, TName;
        int CLevel, TRadius, TTTS, CRadius, StartLat, StartLng, FinishLat, FinishLng, Speed, progress,COwner,AOwner;
        MyUtils.Logwrite("ScanRange","Started by "+Name,r.freeMemory());
        String TGUID, Type, TLat, TLng, Result;
        long CExp, NExp, TExp, Inf1, Inf2, Inf3;
        if (GUID.equals("")) {jresult.put("Error","No player found."); return jresult.toString();}
        try {
            //Караваны
            query = con.prepareStatement("select z1.GUID, z1.Lat, z1.Lng, z1.Type, z2.PGUID, z2.Start, z2.Finish, z2.Speed, z3.Race from GameObjects z1, Caravans z2, Players z3 where z3.GUID=z2.PGUID and z2.GUID=z1.GUID and ? between z1.Lat-10000 and z1.Lat+10000 and ? between z1.Lng-10000 and z1.Lng+10000");
            query.setInt(1, Lat);
            query.setInt(2, Lng);
            rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                while (rs.next()) {
                    JSONObject jobj = new JSONObject();
                    TGUID = rs.getString("GUID");
                    Type = rs.getString("Type");
                    TLat = rs.getString("Lat");
                    TLng = rs.getString("Lng");
                    TPGUID = rs.getString("PGUID");
                    Start = rs.getString("Start");
                    Finish = rs.getString("Finish");
                    Speed = rs.getInt("Speed");
                    if (TPGUID.equals(GUID)) COwner=0;
                    else COwner=rs.getInt("Race");
                    //1-апрельские шутки
                    //COwner+=1;if (COwner==4) COwner=1;}
                    //else COwner=random.nextInt(3)+1;
                    jobj.put("GUID", TGUID);
                    jobj.put("Type", Type);
                    jobj.put("Lat", TLat);
                    jobj.put("Lng", TLng);
                    jobj.put("Owner", COwner);

                    query2 = con.prepareStatement("select z2.Name StartName,z1.Lat StartLat,z1.Lng StartLng,z4.Name FinishName,z3.Lat FinishLat,z3.Lng FinishLng from GameObjects z1, Cities z2, GameObjects z3, Cities z4 where z2.GUID=z1.GUID and z4.GUID=z3.GUID and z1.GUID=? and z3.GUID=?");
                    query2.setString(1, Start);
                    query2.setString(2, Finish);
                    ResultSet rs2=query2.executeQuery();
                    rs2.first();
                    StartName=rs2.getString("StartName");
                    StartLat=rs2.getInt("StartLat");
                    StartLng=rs2.getInt("StartLng");
                    FinishName=rs2.getString("FinishName");
                    FinishLat=rs2.getInt("FinishLat");
                    FinishLng=rs2.getInt("FinishLng");
                    jobj.put("StartName", StartName);
                    jobj.put("StartLat", StartLat);
                    jobj.put("StartLng", StartLng);
                    jobj.put("FinishName", FinishName);
                    jobj.put("FinishLat", FinishLat);
                    jobj.put("FinishLng", FinishLng);
                    jobj.put("Speed", Speed);
                    jarr.add(jobj);
                }
            }

            //Засады
            query = con.prepareStatement("select z1.GUID, z1.Lat, z1.Lng, z1.Type,z2.PGUID,z2.Radius,z2.TTS,z2.Name,z3.Race from GameObjects z1, Ambushes z2, Players z3 where z3.GUID=z2.PGUID and z2.GUID=z1.GUID and ? between z1.Lat-10000 and z1.Lat+10000 and ? between z1.Lng-10000 and z1.Lng+10000");
            query.setInt(1, Lat);
            query.setInt(2, Lng);
            rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                while (rs.next()) {
                    JSONObject jobj = new JSONObject();
                    TGUID = rs.getString("GUID");
                    Type = rs.getString("Type");
                    TLat = rs.getString("Lat");
                    TLng = rs.getString("Lng");
                    TPGUID = rs.getString("PGUID");
                    TRadius = rs.getInt("Radius");
                    TTTS = rs.getInt("TTS");
                    TName = rs.getString("Name");
                    if (TPGUID.equals(GUID)) AOwner=0;
                    else AOwner=rs.getInt("Race");
                    jobj.put("GUID", TGUID);
                    jobj.put("Type", Type);
                    jobj.put("Lat", TLat);
                    jobj.put("Lng", TLng);
                    jobj.put("Owner", AOwner);
                    jobj.put("Radius", TRadius);
                    jobj.put("Ready", TTTS);
                    jobj.put("Name",TName);
                    jarr.add(jobj);
                }
            }

            //Города
            query = con.prepareStatement("select z1.GUID, z1.Lat, z1.Lng, z1.Type,z2.Creator,z2.Name,z2.Level,z2.Exp currentExp,z3.Exp nextLevelExp,z4.Exp thisLevelExp, z2.UpgradeType, (select z3.Name from Upgrades z3 where z2.UpgradeType=z3.Type and z3.Level=0 LIMIT 1) UName, z2.Influence1, z2.Influence2, z2.Influence3 from GameObjects z1 USE INDEX (`LatLng`), Cities z2, Levels z3, Levels z4 where z2.GUID=z1.GUID and ? between z1.Lat-10000 and z1.Lat+10000 and ? between z1.Lng-10000 and z1.Lng+10000 and z3.Type='city' and z3.Level=z2.Level+1 and z4.level=z2.level and z4.Type='City'");
            query.setInt(1, Lat);
            query.setInt(2, Lng);
            rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                while (rs.next()) {
                    JSONObject jobj = new JSONObject();
                    TGUID = rs.getString("GUID");
                    Type = rs.getString("Type");
                    TLat = rs.getString("Lat");
                    TLng = rs.getString("Lng");
                    CName = rs.getString("Name");
                    CLevel = rs.getInt("Level");
                    CExp = rs.getLong("currentExp");
                    NExp = rs.getLong("nextLevelExp");
                    TExp = rs.getLong("thisLevelExp");
                    progress=(int)(100*(CExp-TExp)/(NExp-TExp));
                    CUpgradeType = rs.getString("UpgradeType");
                    CUName = rs.getString("UName");
                    Inf1 = rs.getLong("Influence1");
                    Inf2 = rs.getLong("Influence2");
                    Inf3 = rs.getLong("Influence3");
                    CRadius=100+5*(CLevel - 1);
                    jobj.put("GUID", TGUID);
                    jobj.put("Type", Type);
                    jobj.put("Lat", TLat);
                    jobj.put("Lng", TLng);
                    jobj.put("Name", CName);
                    jobj.put("Level", CLevel);
                    jobj.put("Progress",progress);
                    jobj.put("UpgradeType",CUpgradeType);
                    jobj.put("UpgradeName",CUName);
                    jobj.put("Radius",CRadius);
                    jobj.put("Influence1",Inf1);
                    jobj.put("Influence2",Inf2);
                    jobj.put("Influence3",Inf3);
                    jobj.put("Owner",rs.getString("Creator").equals(GUID));
                    jarr.add(jobj);
                }
            }
            jresult.put("Objects", jarr);
            rs.close();
            query.close();
        } catch (SQLException e) {
            jresult.put("Error", "ScanRange. "+e.toString() + "\n" + Arrays.toString(e.getStackTrace()));
        }
        MyUtils.Logwrite("ScanRange","Finished by "+Name, r.freeMemory());
        return jresult.toString();
    }

    public static double getRadius(String PGUID, Connection con) {
        int result;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select z3.Effect1 from Players z1, PUpgrades z2, Upgrades z3 where z1.GUID=? and z2.PGUID=z1.GUID and z2.UGUID=z3.GUID and z3.Type='paladin'");
            query.setString(1, PGUID);
            ResultSet rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                result = rs.getInt(1);
            } else {
                result = 0;
            }
            rs.close();
            query.close();
            return result;
        } catch (SQLException e) {
            return 0; //pizdec. esli polomaetsia zdes - hyi naidesh )
        }
    }

    public double getRadius() {
        int result;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select z3.Effect1 from Players z1, PUpgrades z2, Upgrades z3 where z1.GUID=? and z2.PGUID=z1.GUID and z2.UGUID=z3.GUID and z3.Type='paladin'");
            query.setString(1, GUID);
            ResultSet rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                result = rs.getInt(1);
            } else {
                result = 0;
            }
            rs.close();
            query.close();
            return result;
        } catch (SQLException e) {
            return 0; //pizdec. esli polomaetsia zdes - hyi naidesh )
        }
    }

    public String GetGUIDByToken(Connection con, String Token) throws SQLException {
        PreparedStatement query;
        query = con.prepareStatement("select PGUID from Connection where Token=? limit 1");
        query.setString(1, Token);
        ResultSet rs = query.executeQuery();
        if (rs.isBeforeFirst()) {
            rs.first();
            GUID = rs.getString("PGUID");
            rs.close();
            query.close();
            return GUID;
        } else {
            query.close();
            LastError = "Error: NOUSERFOUND (" + Token + ")";
            return LastError;
        }
    }

    public boolean checkRangeToObj(String TGUID) {
        int lat, lng;
        PreparedStatement query;
        try {
            query=con.prepareStatement("select Lat,Lng from GameObjects where GUID=?");
            query.setString(1,TGUID);
            ResultSet rs = query.executeQuery();
            rs.first();
            lat = rs.getInt(1);
            lng = rs.getInt(2);
            rs.close();
            query.close();
        } catch (SQLException e) {return false;}
        return (MyUtils.RangeCheck(Lat, Lng, lat, lng) <= getRadius());
    }

    public String getUnfinishedRoute() {
        String RGUID;
        PreparedStatement query;
        try {
            query=con.prepareStatement("select GUID from Caravans where PGUID=? and Finish is null");
            query.setString(1,GUID);
            ResultSet rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                RGUID = rs.getString("GUID");
                rs.close();
            }
            else {RGUID="No route";}
            query.close();
            return RGUID;
        } catch (SQLException e) {return "Error";}
    }

    public String getGUID() {
        return GUID;
    }

    public String getLastError() {
        return LastError;
    }

    public String StartRoute(String TGUID) {
        MyUtils.Logwrite("StartRoute","Started by "+Name, r.freeMemory());
        String res;
        String checkUnfinishedRoute;
        if (checkRangeToObj(TGUID)) {
            checkUnfinishedRoute=getUnfinishedRoute();
            if (checkUnfinishedRoute.equals("No route")) {
                Caravan caravan=new Caravan(con);
                res=caravan.StartRoute(GUID, TGUID, con);
            }
            else {
                if (checkUnfinishedRoute.equals("Error")) {
                    jresult.put("Error", "Error in getUnfinishedRoute()");
                    res=jresult.toString();
                }
                else {
                    jresult.put("Error", "У тебя уже есть незавершенный маршрут!");
                    res=jresult.toString();
                }
            }
        } else {
            jresult.put("Error", "Город слишком далеко.");
            res=jresult.toString();
        }
        MyUtils.Logwrite("StartRoute","Finished by "+Name, r.freeMemory());
        return res;
    }

    public String SetAmbush(int TLAT, int TLNG) {
        int TTS, Radius, Life;
        String res;
        MyUtils.Logwrite("SetAmbush","Started by "+Name, r.freeMemory());
        if (MyUtils.RangeCheck(Lat, Lng, TLAT, TLNG) <= getRadius()) {
            if (CheckAmbushesQuantity()) {
                TTS=getPlayerUpgradeEffect2("set_ambushes");
                Radius=getPlayerUpgradeEffect1("ambushes");
                Life=getPlayerUpgradeEffect2("ambushes");
                Ambush ambush = new Ambush();
                res = ambush.Set(GUID, TLAT, TLNG, Radius, -TTS, Life, false, con);
            } else {
                jresult.put("Error", "Все засады уже установлены!");
                res=jresult.toString();
            }
        } else {
            jresult.put("Error", "Засада слишком далеко!");
            res=jresult.toString();
        }
        MyUtils.Logwrite("SetAmbush","Finished by "+Name, r.freeMemory());
        return res;
    }

 /*   private String getAmbushOwner(String TGUID) {
        PreparedStatement query;
        ResultSet rs;
        String OwnerGUID="";
        try {
            query= con.prepareStatement("select PGUID from Ambushes where GUID=?");
            query.setString(1,TGUID);
            rs = query.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.first();
                OwnerGUID=rs.getString("PGUID");
            }
            rs.close();
            query.close();
        } catch (SQLException e) {}
        return OwnerGUID;
    }
*/
    public String DestroyAmbush(String TGUID) {
        MyUtils.Logwrite("DestroyAmbush","Started by "+Name, r.freeMemory());
        String res;
        int bonus;
        JSONObject jobj=new JSONObject();
        Ambush ambush=new Ambush(TGUID, con);
        if (ambush.Race==Race) {jresult.put("Error", "Нельзя уничтожать засады своей фракции!"); res=jresult.toString();}
        else {
                if (MyUtils.RangeCheck(ambush.Lat, ambush.Lng, Lat, Lng) <= getRadius()) {
                res = ambush.Destroy(TGUID, con);
                jobj.put("Result", "OK");
                if (res.equals(jobj.toString())) {
                    //bonus = 10 + getPlayerUpgradeEffect2("paladin");
                    bonus=(20+Math.min(720,ambush.TTS+180)*ambush.Life)*getPlayerUpgradeEffect2("paladin")/20;
                    jobj.put("Message","Награда за уничтожение засады составила "+Integer.toString(bonus)+" золота!");
                    getGold(bonus);
                    addStat("paladined",bonus);
                    MyUtils.Message(ambush.PGUID, "Ваша засада " + ambush.Name + " была уничтожена!", 2, 0, ambush.Lat, ambush.Lng);
                    res = jobj.toString();
                }
            } else {
                jresult.put("Error", "Засада слишком далеко!");
                res = jresult.toString();
            }
        }
    MyUtils.Logwrite("DestroyAmbush","Finished by "+Name, r.freeMemory());
    return res;
    }

    public String CancelAmbush(String TGUID) {
        MyUtils.Logwrite("CancelAmbush","Started by "+Name, r.freeMemory());
        PreparedStatement query;
        String res, OwnerGUID="";
        JSONObject jobj=new JSONObject();
        try {
            query = con.prepareStatement("select PGUID from Ambushes where GUID=?");
            query.setString(1, TGUID);
            ResultSet rs = query.executeQuery();
            if (rs.isBeforeFirst()){
            rs.first();
            OwnerGUID = rs.getString("PGUID");}
            else {jresult.put("Error","Засада не найдена.");return jresult.toString();}
            rs.close();
        }
        catch (SQLException e) {jresult.put("Error","SQL Error:" +e.toString());return jresult.toString();}
        if (GUID.equals(OwnerGUID)) {
            Ambush ambush=new Ambush();
            res=ambush.Destroy(TGUID,con);
            jobj.put("Result","OK");
            if (res.equals(jobj.toString())) {
                MyUtils.Logwrite("Player.CancelAmbush","Ambush "+TGUID+" canceled by owner "+GUID);
            }
        } else {
            jresult.put("Error", "You are not owner, cheater!");
            res=jresult.toString();
        }
        MyUtils.Logwrite("CancelAmbush","Started by "+Name, r.freeMemory());
        return res;
    }


    public int getPlayerUpgradeEffect1(String Type) {
        int res;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select z1.Effect1 from Upgrades z1, PUpgrades z2 where z2.UGUID=z1.GUID and z2.PGUID=? and z1.type=?");
            query.setString(1,GUID);
            query.setString(2,Type);
            ResultSet rs=query.executeQuery();
            rs.first();
            res=rs.getInt(1);
            rs.close();
            query.close();
        } catch (SQLException e) {return 0;}
        return res;
    }

    public int getPlayerUpgradeEffect2(String Type) {
        int res;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select z1.Effect2 from Upgrades z1, PUpgrades z2 where z2.UGUID=z1.GUID and z2.PGUID=? and z1.type=?");
            query.setString(1,GUID);
            query.setString(2,Type);
            ResultSet rs=query.executeQuery();
            rs.first();
            res=rs.getInt(1);
            rs.close();
            query.close();
        } catch (SQLException e) {return 0;}
        return res;
    }

    public String FinishRoute(String TGUID) {
        MyUtils.Logwrite("FinishRoute","Started by "+Name, r.freeMemory());
        String res;
        String checkUnfinishedRoute,RGUID;
        if (checkRangeToObj(TGUID)) {
            checkUnfinishedRoute=getUnfinishedRoute();
            if (checkUnfinishedRoute.equals("No route")) {
                jresult.put("Error", "Сначала начните маршрут!");
                res=jresult.toString();
            }
            else {
                if (checkUnfinishedRoute.equals("Error")) {
                    jresult.put("Error", "Error in getUnfinishedRoute()");
                    res=jresult.toString();
                }
                else {
                    RGUID=checkUnfinishedRoute;
                    if (!doubleRoute(TGUID,RGUID)) {
                        int accel = getPlayerUpgradeEffect1("speed");
                        int speed = getPlayerUpgradeEffect2("speed");
                        int cargo = getPlayerUpgradeEffect1("cargo");
                        Caravan caravan = new Caravan(con);
                        res = caravan.FinishRoute(RGUID, TGUID, speed, accel, cargo, con);
                    }
                    else {
                        jresult.put("Error","Такой маршрут уже существует, вы не можете создать два одинаковых маршрута!");
                        res=jresult.toString();
                    }
                }
            }
        }
        else {
            jresult.put("Error", "Город слишком далеко.");
            res=jresult.toString();
        }
        MyUtils.Logwrite("FinishRoute","Finished by "+Name, r.freeMemory());
        return res;
    }

    private boolean doubleRoute(String TGUID, String RGUID) {
        PreparedStatement query;
        String SGUID;
        try {
            query=con.prepareStatement("select Start from Caravans where Finish is null and PGUID=?");
            query.setString(1,GUID);
            ResultSet rs0=query.executeQuery();
            rs0.first();
            SGUID=rs0.getString("Start");
            rs0.close();
            query.close();
            query=con.prepareStatement("select count(1) from Caravans where ((Finish=? and Start=?) or (Finish=? and Start=?)) and PGUID=?");
            query.setString(1,TGUID);
            query.setString(2,SGUID);
            query.setString(3,SGUID);
            query.setString(4,TGUID);
            query.setString(5,GUID);
            ResultSet rs = query.executeQuery();
            rs.first();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            return true;
        }
    }

    public String DropUnfinishedRoute() {
        MyUtils.Logwrite("DropUnfinishedRoute","Started by "+Name, r.freeMemory());
        PreparedStatement query;
        try {
            query = con.prepareStatement("delete from Caravans where PGUID=? and Finish is null");
            query.setString(1,GUID);
            query.execute();
            con.commit();
            query.close();
            jresult.put("Result", "OK");
            MyUtils.Logwrite("DropUnfinishedRoute","Finished by "+Name, r.freeMemory());
            return jresult.toString();
        } catch (SQLException e) {jresult.put("Error","DropUnfinishedRoute "+e.toString()); return jresult.toString();}
    }

    public String DropRoute(String TGUID) {
        MyUtils.Logwrite("DropRoute","Started by "+Name, r.freeMemory());
        PreparedStatement query;
        String OwnerGUID;
        try {
            query = con.prepareStatement("select PGUID from Caravans where GUID=?");
            query.setString(1,TGUID);
            ResultSet rs=query.executeQuery();
            rs.first();
            OwnerGUID=rs.getString("PGUID");
            rs.close();
            if (GUID.equals(OwnerGUID)) {
                query = con.prepareStatement("delete from Caravans where PGUID=? and GUID=?");
                query.setString(1, GUID);
                query.setString(2, TGUID);
                query.execute();
                query = con.prepareStatement("delete from GameObjects where GUID=?");
                query.setString(1, TGUID);
                query.execute();
                con.commit();
                query.close();
                jresult.put("Result", "OK");

            }
            else {query.close();jresult.put("Error", "Ты не можешь отменить маршрут другого игрока!");}
        } catch (SQLException e) {jresult.put("Error","DropRoute "+e.toString());}
        MyUtils.Logwrite("DropRoute","Finished by "+Name, r.freeMemory());
        return jresult.toString();
    }

//Ex-Client

    public Connection getCon() {
        return con;
    }

    //TODO move to GenPlayer
    public String temp() {
        PreparedStatement query, query2, query3;
        int p=0;
        int u=0;
        String UGUID, PGUID; // = UUID.randomUUID().toString();
        String result="";
        try {
            query = con.prepareStatement("select GUID from Upgrades WHERE level=0");
            ResultSet rs = query.executeQuery();
            while (rs.next()) {
                u=u+1;
                result=result+"u="+Integer.toString(u)+";";
                UGUID = rs.getString(1);
                query2 = con.prepareStatement("select GUID from Players");
                ResultSet rs2 = query2.executeQuery();
                p=0;
                while (rs2.next()) {
                    p=p+1;
                    result=result+"p="+Integer.toString(p)+";";
                    PGUID = rs2.getString(1);
                    query3 = con.prepareStatement("insert into PUpgrades(PGUID,UGUID) values(?,?) ");
                    query3.setString(1, PGUID);
                    query3.setString(2, UGUID);
                    query3.execute();
                    con.commit();
                    //query3.close();
                }
                rs2.close();
                //query.close();
            }
            rs.close();
//            con.commit();
//            query.close();
            result=result+"Done!";
        } catch (SQLException e) { result = result + e.toString(); }

        try {
            con.close();
        } catch (SQLException e) {result = result + e.toString();}
        return result;
    }

    public String tempExp() {
        PreparedStatement query, query2,query3,query4;
        ResultSet rs,rs2;
        int level;
        long exp, totalexp=0;
        try {
            query = con.prepareStatement("select Exp, Level from Levels where Type='player' order by Level asc");
            rs=query.executeQuery();
            while (rs.next())
            {
                exp=rs.getLong("Exp");
                level=rs.getInt("Level");
                totalexp+=exp*10;
                query2 = con.prepareStatement("update Levels set Exp=? where Type='player' and Level = ?");
                query2.setLong(1,totalexp);
                query2.setInt(2,level);
                query2.execute();
                query2.close();
            }
            rs.close();
            query.close();

            totalexp=0;
            query3 = con.prepareStatement("select Exp, Level from Levels where Type='city' order by Level asc");
            rs2=query3.executeQuery();
            while (rs2.next())
            {
                exp=rs2.getLong("Exp");
                level=rs2.getInt("Level");
                totalexp+=exp*5;
                query4 = con.prepareStatement("update Levels set Exp=? where Type='city' and Level = ?");
                query4.setLong(1,totalexp);
                query4.setInt(2,level);
                query4.execute();
                query4.close();
            }
            rs2.close();
            query3.close();
            con.commit();
        } catch (SQLException e) {return "Error while fixing Exp table " +e.toString();}
        return "Exp table rebuilded!";
    }


    public String sendData(String ReqName, String TGUID, int TLAT, int TLNG, int RACE, int AMOUNT) {
        MyUtils.Logwrite("sendData","дошли");
        String result;
        switch (ReqName) {
            case "ScanRange":
                result = ScanRange();
                break;
            case "SetAmbush":
                result = SetAmbush(TLAT, TLNG);
                break;
            case "DestroyAmbush":
                result = DestroyAmbush(TGUID);
                break;
            case "CancelAmbush":
                result = CancelAmbush(TGUID);
                break;
            case "StartRoute":
                result = StartRoute(TGUID);
                break;
            case "FinishRoute":
                result = FinishRoute(TGUID);
                break;
            case "BuyUpgrade":
                result = BuyUpgrade(TGUID);
                break;
            case "DropUnfinishedRoute":
                result = DropUnfinishedRoute();
                break;
            case "DropRoute":
                result = DropRoute(TGUID);
                break;
            case "GetPlayerInfo":
                result = GetPlayerInfo();
                break;
            case "GetMessage":
                result=GetMessage();
                break;
            case "SetRace":
                result=setRace(RACE);
                break;
            case "CreateCity":
                result=createCity(TLAT,TLNG);
                break;
            case "HirePeople":
                result=hirePeople(TGUID, AMOUNT);
                break;
            default:
                result = "{" + '"' + "Error" + '"' + ": " + '"' + "Unknown command." + '"' + "}";
        }
        try {
            con.close();
        } catch (SQLException e) {result="Connection not closed! "+result;}
        return result;
    }

    private String hirePeople(String TGUID, int AMOUNT) {
        jresult.put("Result", "OK");
        return jresult.toString();
    }

    private String createCity(int TLAT, int TLNG) {
        String res, CGUID;
        int mapper;
        MyUtils.Logwrite("createCity","Started by "+Name, r.freeMemory());
        if (MyUtils.RangeCheck(Lat, Lng, TLAT, TLNG) <= getRadius()) {
            if (CheckCitiesQuantity()) {
                CGUID=UUID.randomUUID().toString();
                City city = new City(CGUID, con);
                mapper=getPlayerUpgradeEffect2("founder");
                res = city.createCity(GUID, TLAT, TLNG, mapper);
            } else {
                jresult.put("Result","O1203");
                jresult.put("Message", "Достигнут лимит основанных городов!");
                res=jresult.toString();
            }
        } else {
            jresult.put("Result","O1201");
            jresult.put("Error", "Слишком далеко!");
            res=jresult.toString();
        }
        MyUtils.Logwrite("createCity","Finished by "+Name, r.freeMemory());
        return res;
    }

    private String GetMessage() {
        MyUtils.Logwrite("GetMessage","Started by "+Name, r.freeMemory());
        PreparedStatement query, query2;
        ResultSet rs;
        try {
            query = con.prepareStatement("select GUID,PGUID,Message,Type,State,Lat,Lng,Time from Messages where State<100 and PGUID=? order by Time");
            query.setString(1,GUID);
            rs=query.executeQuery();
            if (rs.isBeforeFirst()) {
                while (rs.next()) {
                    JSONObject jobj = new JSONObject();
                    jobj.put("GUID",rs.getString("GUID"));
                    //jobj.put("PGUID",rs.getString("PGUID"));
                    jobj.put("Message",rs.getString("Message"));
                    jobj.put("Type",rs.getInt("Type"));
                    jobj.put("State",rs.getInt("State"));
                    jobj.put("TargetLat",rs.getInt("Lat"));
                    jobj.put("TargetLng",rs.getInt("Lng"));
                    jobj.put("Time",rs.getTimestamp("Time").getTime());
                    jarr.add(jobj);
                    query2 = con.prepareStatement("update Messages set State=100 where GUID=?");
                    query2.setString(1,rs.getString("GUID"));
                    query2.execute();
                    query2.close();
                    con.commit();
                }
            }
            jresult.put("Messages",jarr);
        } catch (SQLException e) {jresult.put("Error","SQL Error: "+e.toString());}
        MyUtils.Logwrite("GetMessage","Finished by "+Name, r.freeMemory());
        return jresult.toString();
    }

    public String GetToken(String Login, String Password) {
        PreparedStatement pstmt,query;
        String Token = "T" + UUID.randomUUID().toString();
        String PGUID;
        //      JSONObject jresult = new JSONObject();
        try {
            pstmt = con.prepareStatement("SELECT GUID from Users WHERE Login=? and Password=?");
            pstmt.setString(1, Login);
            pstmt.setString(2, Password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.isBeforeFirst()) {
                rs.next();
                PGUID = rs.getString(1);
                query=con.prepareStatement("select count(1) from Players where GUID=?");
                query.setString(1,PGUID);
                ResultSet rs2=query.executeQuery();
                rs2.first();
                if (rs2.getInt(1)==0) {register(Login, Password);}
                rs2.close();
                query.close();
                pstmt = con.prepareStatement("INSERT into Connections (PGUID,Token) Values(?,?)");
                pstmt.setString(1, PGUID);
                pstmt.setString(2, Token);
                pstmt.execute();
                con.commit();
                con.close();
                jresult.put("Token", Token);
            } else {
                jresult.put("Error", "User not found.");
                con.close();
            }
        } catch (SQLException e) {
            jresult.put("Error", "DBError. " + e.toString() + ". " + Arrays.toString(e.getStackTrace()));
        } /*catch (NamingException e) {
            jresult.put("Error", "ResourceError. " + e.toString() + ". " + Arrays.toString(e.getStackTrace()));
        }*/
        return jresult.toString();
    }
//End of Ex-Client

}
