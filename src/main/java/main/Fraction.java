package main;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Well on 17.03.2016.
 */
public class Fraction {
    String Name;
    int Id;
    long Gold;

    public Fraction(int id, Connection con) {
        PreparedStatement query;
        Id=id;
        if (id>=1 && id<=3) {
            try {
                query = con.prepareStatement("select Name, Gold from Fractions where Id=?");
                query.setInt(1, id);
                ResultSet rs = query.executeQuery();
                rs.first();
                Name = rs.getString("Name");
                Gold = rs.getLong("Gold");
                rs.close();
                query.close();
            } catch (SQLException e) {
                MyUtils.Logwrite("Fraction", "При запросе данных фракции id = " + id + " возникла ошибка " + e.toString());
                Name = "";
                Gold = 0;
            }
        }
    }

    public void getGold(int GOLD, Connection con) {
        PreparedStatement query;
        Gold += GOLD;
        try {
            query = con.prepareStatement("update Fractions set Gold=? where Id=?");
            query.setLong(1, Gold);
            query.setInt(2, Id);
            query.execute();
            query.close();
            con.commit();
        } catch (SQLException e) {
            MyUtils.Logwrite("Fraction.getGold","Error: "+e.toString());
        }
    }

}
