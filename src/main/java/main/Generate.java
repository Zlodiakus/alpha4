package main;

import javax.naming.NamingException;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Random;

/**
 * @author Shadilan
 */
public class Generate {
    /**
     * Generate city from coordinates.
     * String wrapper
     * @param Lat1 Latitude
     * @param Lng1 Longtitude
     * @param Lat2 Latitude
     * @param Lng2 Longtitude
     * @param count Number of cities
     * @return Generation info
     */

    public static String GenCity(String Lat1, String Lng1, String Lat2, String Lng2, String count) {
        String result;
        try {
            int Lat1N = Integer.parseInt(Lat1);
            int Lat2N = Integer.parseInt(Lat2);
            int Lng1N = Integer.parseInt(Lng1);
            int Lng2N = Integer.parseInt(Lng2);
            int countN = Integer.parseInt(count);
            result = GenCity(Lat1N, Lng1N, Lat2N, Lng2N, countN);
        } catch (NumberFormatException e) {
            result = e.toString();
        }
        return result;
    }

    /**
     * Get city name from database
     * @param con Connection to database
     * @return Cityname
     * @throws SQLException
     */
    public static String genCityName(Connection con) throws SQLException {
       /* if (true)
        {return "test2";} else {}*/
        Random random=new Random();
        String result="";
        ResultSet rs;
        int max1,max2,type1;
        String part1,part2, tablename;
        PreparedStatement query=con.prepareStatement("select count(1) from citystart");
        rs=query.executeQuery();
        rs.next();
        max1=rs.getInt(1);
        query.close();
        rs.close();
        query=con.prepareStatement("select Start, Type from citystart where id=?");
        query.setInt(1,random.nextInt(max1)+1);
        rs=query.executeQuery();
        rs.next();
        part1=rs.getString("Start");
        type1=rs.getInt("Type");
        query.close();
        rs.close();
        if (type1==1) {
            query = con.prepareStatement("select count(1) from cityparts1");
            rs = query.executeQuery();
            rs.next();
            max2 = rs.getInt(1);
            query.close();
            rs.close();
            query = con.prepareStatement("select part from cityparts1 where id=?");
            query.setInt(1, random.nextInt(max2) + 1);
            rs = query.executeQuery();
            rs.next();
            part2 = rs.getString(1);
            query.close();
            rs.close();
        }
        else {
            query = con.prepareStatement("select count(1) from cityparts2");
            rs = query.executeQuery();
            rs.next();
            max2 = rs.getInt(1);
            query.close();
            rs.close();
            query = con.prepareStatement("select part from cityparts2 where id=?");
            query.setInt(1, random.nextInt(max2) + 1);
            rs = query.executeQuery();
            rs.next();
            part2 = rs.getString(1);
            query.close();
            rs.close();
        }

        return part1+part2;
    }
    /**
     * Gen Cities in target area.
     * @param Lat1     Latitude of start of rect
     * @param Lng1     Longtitude of start of rect
     * @param Lat2     Latitude of end of rect
     * @param Lng2     Longtitude of end of rect
     * @param count Count of cities
     * @return Generation information
     */
    public static String GenCity(int Lat1, int Lng1, int Lat2, int Lng2, int count) {
        //Count valid coord;
        String name="";
        String result="";
        String test="";
        int Lat1N;
        int Lat2N;
        int Lng1N;
        int Lng2N;
        int i=0;
        Random random=new Random();
        String [] upgrades = new String [6];

        if (Lat1 < Lat2) {
            Lat1N = Lat1;
            Lat2N = Lat2;
        } else {
            Lat1N = Lat2;
            Lat2N = Lat1;
        }
        if (Lng1 < Lng2) {
            Lng1N = Lng1;
            Lng2N = Lng2;
        } else {
            Lng1N = Lng2;
            Lng2N = Lng1;
        }
        result = "L" + Lat1N + "X" + Lng1N + "\n";
        result += "L" + Lat2N + "X" + Lng2N + "\n";
        int width = Lat2N - Lat1N;
        int height = Lng2N - Lng1N;
        result += "W" + width + " H" + height + "\n";
        result += "Count:" + count;
        //Remove all current city
        try {
            Connection con = DBUtils.ConnectDB();
            PreparedStatement query;
            query=con.prepareStatement("select distinct Type from Upgrades");
            ResultSet rs=query.executeQuery();
            if (rs.isBeforeFirst()) {
            while (rs.next()) {
               i=i+1;
               test=test+rs.getString(1);
               upgrades[i-1]=rs.getString(1);
            }
            query.close();
            rs.close();
            }
            else {query.close();rs.close();return "Ooops!";}

            PreparedStatement stmt;
//            stmt = con.prepareStatement("delete from Cities");
//            stmt.execute();
//           stmt = con.prepareStatement("delete from GameObjects where Type='City'");
//            stmt.execute();
            ArrayList<Point> cities = MyUtils.createCitiesOnMap(width, height, count);
            //ArrayList<Point> cities = MyUtils.newCreateCitiesOnMap(Lat1, Lng1, Lat2, Lng2);
            int j=0;

            for (Point a : cities) {

                String GUID = UUID.randomUUID().toString();
                stmt = con.prepareStatement("INSERT INTO Cities (GUID,Name,Level,Exp,UpgradeType) VALUES(?,?,?,?,?)");
                stmt.setString(1, GUID);
                name=genCityName(con);
                test=test+name;
                stmt.setString(2,name);
                stmt.setInt(3, 1);
                stmt.setInt(4, 0);
                stmt.setString(5, upgrades[random.nextInt(6)]);
                /*if (true) {
                    return name + " " + upgrades[random.nextInt(6)];}
                else {};*/
                stmt.execute();
                stmt = con.prepareStatement("INSERT INTO GameObjects(GUID,Lat,Lng,Type)VALUES(?,?,?,'City')");
                stmt.setString(1, GUID);
                stmt.setInt(2, (int) a.getX() + Lat1N);
                stmt.setInt(3, (int) a.getY() + Lng1N);
                stmt.execute();
                if (j>=100) {
                    con.commit();
                    j=0;
                }
                else {j=j+1;}
                result += "GUID:" + GUID + " Lat:" + (a.getX() + Lat1N) + " Lng:" + (a.getY() + Lng1N) + "\n";
            }
            con.commit();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return test+e.toString();
        } catch (NamingException e) {
            e.printStackTrace();
            return test+e.toString();
        }
        return result;
        //Generate positions
        //write to db
    }


    public static String newGenCity(int Lat1, int Lng1, int Lat2, int Lng2) {
        //Count valid coord;
        String name="";
        String result="";
        String test="";
        int Lat1N;
        int Lat2N;
        int Lng1N;
        int Lng2N;
        int i=0;
        Random random=new Random();
        String [] upgrades = new String [6];

        if (Lat1 < Lat2) {
            Lat1N = Lat1;
            Lat2N = Lat2;
        } else {
            Lat1N = Lat2;
            Lat2N = Lat1;
        }
        if (Lng1 < Lng2) {
            Lng1N = Lng1;
            Lng2N = Lng2;
        } else {
            Lng1N = Lng2;
            Lng2N = Lng1;
        }
        result = "L" + Lat1N + "X" + Lng1N + "\n";
        result += "L" + Lat2N + "X" + Lng2N + "\n";
        int width = Lat2N - Lat1N;
        int height = Lng2N - Lng1N;
        //result += "W" + width + " H" + height + "\n";
        //result += "Count:" + count;
        //Remove all current city
        try {
            Connection con = DBUtils.ConnectDB();
            PreparedStatement query;
            query=con.prepareStatement("select distinct Type from Upgrades");
            ResultSet rs=query.executeQuery();
            if (rs.isBeforeFirst()) {
                while (rs.next()) {
                    i=i+1;
                    test=test+rs.getString(1);
                    upgrades[i-1]=rs.getString(1);
                }
                query.close();
                rs.close();
            }
            else {query.close();rs.close();return "Ooops!";}

            PreparedStatement stmt;
//            stmt = con.prepareStatement("delete from Cities");
//            stmt.execute();
//           stmt = con.prepareStatement("delete from GameObjects where Type='City'");
//            stmt.execute();
            //ArrayList<Point> cities = MyUtils.createCitiesOnMap(width, height, count);
            ArrayList<Point> cities = MyUtils.newCreateCitiesOnMap(Lat1N, Lng1N, Lat2N, Lng2N);
            int j=0;
            int jj=0;

            for (Point a : cities) {
                jj++;
                String GUID = UUID.randomUUID().toString();
                stmt = con.prepareStatement("INSERT INTO Cities (GUID,Name,Level,Exp,UpgradeType, Influence1, Influence2, Influence3) VALUES(?,?,1,0,?,0,0,0)");
                stmt.setString(1, GUID);
                name=genCityName(con);
                test=test+name;
                stmt.setString(2,name);
                stmt.setString(3, upgrades[random.nextInt(6)]);
                stmt.execute();
                stmt = con.prepareStatement("INSERT INTO GameObjects(GUID,Lat,Lng,Type)VALUES(?,?,?,'City')");
                stmt.setString(1, GUID);
                stmt.setInt(2, (int) a.getX());
                stmt.setInt(3, (int) a.getY());
                stmt.execute();
                if (j>=100) {
                    con.commit();
                    j=0;
                    result += "GUID:" + GUID + " Lat:" + (a.getX()) + " Lng:" + (a.getY()) + "\n";
                }
                else {j=j+1;}
                //result += "GUID:" + GUID + " Lat:" + (a.getX()) + " Lng:" + (a.getY()) + "\n";
            }
            result +="Сгенерировано " +Integer.toString(jj)+" городов.\n";
            con.commit();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return test+e.toString();
        } catch (NamingException e) {
            e.printStackTrace();
            return test+e.toString();
        }
        return result;
        //Generate positions
        //write to db
    }


    /**
     * Basic function of generation delete all objects.
     * @deprecated
     * @param x     Width
     * @param y     Height
     * @param count Count of cities
     * @return Result of operation
     */

    public static String GenCity(int x, int y, int count) {
        //Remove all current city
        try {
            Connection con = DBUtils.ConnectDB();
            PreparedStatement stmt;
//            stmt = con.prepareStatement("delete from cities");
//            stmt.execute();
//            stmt = con.prepareStatement("delete from aobject where ObjectType='CITY'");
//            stmt.execute();
            ArrayList<Point> cities = MyUtils.createCitiesOnMap(x, y, count);
            for (Point a : cities) {
                String GUID = UUID.randomUUID().toString();
                stmt = con.prepareStatement("INSERT INTO cities(GUID,Lat,Lng,CITYNAME)VALUES(?,?,?,'TEST')");
                stmt.setString(1, GUID);
                stmt.setInt(2, (int) a.getX());
                stmt.setInt(3, (int) a.getY());
                stmt.execute();
                stmt = con.prepareStatement("INSERT INTO aobject(GUID,Lat,Lng,ObjectType)VALUES(?,?,?,'CITY')");
                stmt.setString(1, GUID);
                stmt.setInt(2, (int) a.getX());
                stmt.setInt(3, (int) a.getY());
                stmt.execute();
                con.commit();
            }
            con.commit();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return e.toString();
        } catch (NamingException e) {
            e.printStackTrace();
            return e.toString();
        }
        return "Success";
        //Generate positions
        //write to db

    }

    public static String genInvites() {
        try {
            Connection con = DBUtils.ConnectDB();
            PreparedStatement query;
            String Invite;
            int i;
            for (i = 0; i < 100; i++) {
                Invite = UUID.randomUUID().toString();
                try {
                    query = con.prepareStatement("insert into Invites (Invite) values (?)");
                    query.setString(1, Invite);
                    query.execute();
                } catch (SQLException e) {
                    return e.toString();
                }
            }
            con.commit();
            return "100 invites generated.";
        } catch (SQLException | NamingException e) {
            return e.toString();
        }
    }

    public static String genUUID() {
        return UUID.randomUUID().toString();
    }



}
