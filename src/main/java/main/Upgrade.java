package main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Well on 03.02.2016.
 */
public class Upgrade {
    Connection con;
    String GUID, Name, Description, Type,result;
    int Level, ReqPlayerLev, ReqCityLev, Effect1, Effect2, Cost;
    public Upgrade(String PGUID, String CGUID, Connection CON)  {
        con=CON;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select z1.GUID, z1.Name, z1.Description, z1.Type, z1.Level, z1.ReqPlayerLev, z1.ReqCityLev, z1.Effect1, z1.Effect2, z1.Cost from Upgrades z1, Cities z3, PUpgrades z4 where z4.UGUID=z1.GUID and z3.UpgradeType=z1.Type and z4.PGUID=? and z3.GUID=?");
            query.setString(1,PGUID);
            query.setString(2,CGUID);
            ResultSet rs = query.executeQuery();
            rs.first();
            GUID = rs.getString("GUID");
            Name = rs.getString("Name");
            Description = rs.getString("Description");
            Type = rs.getString("Type");
            Level = rs.getInt("Level");
            ReqPlayerLev = rs.getInt("ReqPlayerLev");
            ReqCityLev = rs.getInt("ReqCityLev");
            Effect1 = rs.getInt("Effect1");
            Effect2 = rs.getInt("Effect2");
            Cost = rs.getInt("Cost");
        } catch (SQLException e)
        {
            GUID="0";result=e.toString();
        }
    }

    public Upgrade(String UType, int ULevel, Connection CON)  {
        con=CON;
        PreparedStatement query;
        try {
            query = con.prepareStatement("select z1.GUID, z1.Name, z1.Description, z1.Type, z1.Level, z1.ReqPlayerLev, z1.ReqCityLev, z1.Effect1, z1.Effect2, z1.Cost from Upgrades z1 where Type=? and Level=?");
            query.setString(1,UType);
            query.setInt(2,ULevel);
            ResultSet rs = query.executeQuery();
            rs.first();
            GUID = rs.getString("GUID");
            Name = rs.getString("Name");
            Description = rs.getString("Description");
            Type = rs.getString("Type");
            Level = rs.getInt("Level");
            ReqPlayerLev = rs.getInt("ReqPlayerLev");
            ReqCityLev = rs.getInt("ReqCityLev");
            Effect1 = rs.getInt("Effect1");
            Effect2 = rs.getInt("Effect2");
            Cost = rs.getInt("Cost");
        } catch (SQLException e)
        {
            GUID="0";result=e.toString();
        }
    }

    public void update(String PGUID, Connection CON) {
        con=CON;
        PreparedStatement query;

    }


}
