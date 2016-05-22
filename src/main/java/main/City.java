package main;

import javax.naming.NamingException;
import javax.persistence.criteria.CriteriaBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
