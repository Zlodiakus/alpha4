package main;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Created by Well on 30.01.2016.
 */
public class World {
    Connection con;
    String result;

public World() throws SQLException {
    try {
        con = DBUtils.ConnectDB();
    } catch (SQLException | NamingException e) {
        e.printStackTrace();
    }
}

    public void moveFast() {
        tickAmbushes();
        moveAllCaravans();
        handleAmbushes();
        handleFinishedCaravans();
    }

    public void moveHour() {
        citiesHire();
    }

    public void close() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void citiesHire() {
        PreparedStatement query;
        MyUtils.Logwrite("World.citiesHire", "Start");
        try {
            query = con.prepareStatement("update Cities set Hirelings=MIN(100*Level,Hirelings+4*Level) where Hirelings<100*Level");
            query.execute();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("World.citiesHire", "SQL Error: " + e.toString());
        }
        MyUtils.Logwrite("World.citiesHire", "Finish");
    }

    private void tickAmbushes() {
        PreparedStatement query;
        MyUtils.Logwrite("World.tickAmbushes", "Start");
        try {
            query = con.prepareStatement("update Ambushes set TTS=TTS+1");
            query.execute();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("World.tickAmbushes", "SQL Error: " + e.toString());
        }
        MyUtils.Logwrite("World.tickAmbushes", "Finish");
    }


    private void moveAllCaravans() {
        PreparedStatement query;
        MyUtils.Logwrite("World.moveAllCaravans", "Start");
        try {
            //пересчитали скорость
            query = con.prepareStatement("update Caravans z1, PUpgrades z3, Upgrades z4 set z1.Lifetime=z1.Lifetime+1, z1.speed=sign(z1.speed)*least(abs(z1.speed)+z4.effect1,z4.effect2) where z1.PGUID=z3.PGUID and z3.UGUID=z4.GUID and z4.Type='speed'");
            query.execute();
            query.close();
            //изменили координаты караванов
            query = con.prepareStatement("update Caravans z1, GameObjects z2, GameObjects z3, GameObjects z4 set z2.Lat=round(z2.Lat+(z4.Lat-z3.Lat)*z1.Speed/z1.Distance), z2.Lng=round(z2.Lng+(z4.Lng-z3.Lng)*z1.Speed/z1.Distance) where z1.GUID=z2.GUID and z1.Start=z3.GUID and z1.Finish=z4.GUID");
            query.execute();
            query.close();
            con.commit();
        } catch (SQLException e) {
            MyUtils.Logwrite("World.moveAllCaravans", "SQL Error: " + e.toString());
        }
        MyUtils.Logwrite("World.moveAllCaravans", "Finish");
    }
/*
    private void fastAmbushes() {
        String CGUID, CPGUID, AGUID, APGUID, AName, SName, FName, prevCaravan="111",Start,Finish;
        int ALife, Bonus, SLevel, FLevel, Distance, ALat, ALng, PLat, PLng, Lat,Lng, Speed;
        PreparedStatement query,query2;
        Player player;
        ResultSet rs;
        int deltaLat = 1125;
        int deltaLng = 2500;
        MyUtils.Logwrite("World.fastAmbushes", "Start");
        try {
            //query = con.prepareStatement("select z1.GUID as AGUID, z3.GUID as CGUID, z2.Life, z2.Name, z2.PGUID, z2.Radius, z2.TTS from GameObjects z1, Ambushes z2, GameObjects z3 where z2.GUID=z1.GUID and z3.Lat between z1.Lat-1125 and z1.Lat+1125 and z3.Lng between z1.Lng-2500 and z1.Lng+2500 and z1.Type='Ambush' and z3.Type='Caravan' and z2.Radius>=round(6378137 * acos(cos(z1.Lat / 1e6 * PI() / 180) * cos(z3.Lat / 1e6 * PI() / 180) * cos(z1.Lng / 1e6 * PI() / 180 - z3.Lng / 1e6 * PI() / 180) + sin(z1.Lat / 1e6 * PI() / 180) * sin(z3.Lat / 1e6 * PI() / 180)))");
            query = con.prepareStatement("select z1.GUID as AGUID, z1.Lat as ALat, z1.Lng as ALng, z3.GUID as CGUID, z3.Lat as CLat, z3.Lng as CLng from GameObjects z1, GameObjects z3 where z3.Lat between z1.Lat-1125 and z1.Lat+1125 and z3.Lng between z1.Lng-2500 and z1.Lng+2500 and z1.Type='Ambush' and z3.Type='Caravan'");
            rs=query.executeQuery();
            while (rs.next()) {
                AGUID=rs.getString("AGUID");
                CGUID=rs.getString("CGUID");
            }
        } catch (SQLException e) {
            MyUtils.Logwrite("World.handleAmbushes", "SQL Error: " + e.toString());
        }
    }
*/
    private void handleAmbushes() {
        String CGUID, CPGUID, AGUID, APGUID, AName, SName, FName, prevCaravan="111",Start,Finish;
        int ALife, Bonus, SLevel, FLevel, Distance, ALat, ALng, PLat, PLng, Lat,Lng, Speed;
        PreparedStatement query,query2;
        Player player;
        ResultSet rs;
        int deltaLat = 1125;
        int deltaLng = 2500; //Надо бы как-то считать наверное
        MyUtils.Logwrite("World.handleAmbushes", "Start");
        try {
            query = con.prepareStatement("select z4.Start,z4.Finish,z4.Speed,z4.GUID as CGUID, z4.PGUID as CPGUID, z3.Lat as PLat, z3.Lng as PLng, z2.GUID as AGUID, z2.PGUID as APGUID, z2.Life as ALife, z2.Name as AName, z1.Lat as ALat, z1.Lng as ALng, (select level from Cities where GUID=z4.Start) as SLevel, (select Name from Cities where GUID=z4.Start) as SName, (select level from Cities where GUID=z4.Finish) as FLevel, (select Name from Cities where GUID=z4.Finish) as FName, z4.Distance " +
                    "from GameObjects z1 ignore index (`PRIMARY`), Ambushes z2, GameObjects z3 ignore index (`PRIMARY`), Caravans z4, Players z5, Players z6 " +
                    "where z2.PGUID=z5.GUID and z4.PGUID=z6.GUID and z5.Race!=z6.Race and z3.Lat between z1.Lat-"+deltaLat+" and z1.Lat+"+deltaLat+" and z3.Lng between z1.Lng-"+deltaLng+" and z1.Lng+"+deltaLng+" and z1.GUID=z2.GUID and z1.Type='Ambush' and z3.Type='Caravan' and z3.GUID=z4.GUID and z2.TTS>=0 and z2.PGUID!=z4.PGUID " +
                    "and z2.Radius>=round(6378137 * acos(cos(z1.Lat / 1e6 * PI() / 180) * cos(z3.Lat / 1e6 * PI() / 180) * cos(z1.Lng / 1e6 * PI() / 180 - z3.Lng / 1e6 * PI() / 180) + sin(z1.Lat / 1e6 * PI() / 180) * sin(z3.Lat / 1e6 * PI() / 180))) order by CGUID");
            rs=query.executeQuery();
            while (rs.next()) {
                CGUID=rs.getString("CGUID");
                if (!CGUID.equals(prevCaravan)) {
                    Start=rs.getString("Start");
                    Finish=rs.getString("Finish");
                    Speed=rs.getInt("Speed");
                    CPGUID = rs.getString("CPGUID");
                    AGUID = rs.getString("AGUID");
                    APGUID = rs.getString("APGUID");
                    ALife = rs.getInt("ALife");
                    AName = rs.getString("AName");
                    SName = rs.getString("SName");
                    FName = rs.getString("FName");
                    SLevel = rs.getInt("SLevel");
                    FLevel = rs.getInt("FLevel");
                    //Distance = rs.getInt("Distance");
                    ALat = rs.getInt("ALat");
                    ALng = rs.getInt("ALng");
                    PLat = rs.getInt("PLat");
                    PLng = rs.getInt("PLng");
                    player=new Player(CPGUID,con);
                    //Bonus = (int) (Math.sqrt(SLevel) * Math.sqrt(FLevel) * Distance*player.getPlayerUpgradeEffect1("cargo")/100);
                    ResultSet rs2;
                    if (Speed>0) {
                        query2=con.prepareStatement("select Lat,Lng from GameObjects z1 where GUID=?");
                        query2.setString(1,Start);
                        rs2=query2.executeQuery();
                        rs2.next();
                        Lat=rs2.getInt("Lat");
                        Lng=rs2.getInt("Lng");
                    }
                    else {
                        query2=con.prepareStatement("select Lat,Lng from GameObjects z1 where GUID=?");
                        query2.setString(1,Finish);
                        rs2=query2.executeQuery();
                        rs2.next();
                        Lat=rs2.getInt("Lat");
                        Lng=rs2.getInt("Lng");
                    }
                    MyUtils.Logwrite("World.handleAmbushes","SName = "+SName+", FName = "+FName+". StartLat,StartLng = "+Lat+","+Lng+". ALat,ALng = "+ALat+","+ALng);
                    Distance=(int)MyUtils.RangeCheck(Lat,Lng,ALat,ALng);
                    //Bonus=(int) (Math.sqrt(SLevel) * Math.sqrt(FLevel) * 2 * Distance*player.getPlayerUpgradeEffect1("cargo")/10);
                    Bonus=(int) ((Math.sqrt(SLevel) * Math.sqrt(FLevel) * player.getPlayerUpgradeEffect1("cargo")/100) * (5000 + Distance));
                    Caravan caravan = new Caravan(CGUID, CPGUID, SName, FName, PLat, PLng, con);
                    caravan.ambushed(APGUID);
                    Ambush ambush = new Ambush(AGUID, APGUID, AName, ALife, ALat, ALng, con);
                    ambush.caravaned(Bonus);
                    prevCaravan=CGUID;
                }
            }
            rs.close();
            query.close();
        } catch (SQLException e) {
            MyUtils.Logwrite("World.handleAmbushes", "SQL Error: " + e.toString());
        }
        MyUtils.Logwrite("World.handleAmbushes", "Finish");
    }

    public void handleFinishedCaravans() {
        PreparedStatement query;
        ResultSet rs;
        int i=0;
        MyUtils.Logwrite("World.handleFinishedCaravans", "Start");
        try {
            query= con.prepareStatement("select z0.GUID, z0.PGUID, z0.Lifetime, z0.Danger, z0.Start,z0.Finish, z0.bonus, z2.Lat as LatS, z2.Lng as LngS, z1.Lat as Lat, z1.Lng as Lng, z3.Lat as LatF, z3.Lng as LngF, z0.Speed " +
                    "from Caravans z0, GameObjects z1, GameObjects z2, GameObjects z3 " +
                    "where z0.GUID=z1.GUID and z0.Start=z2.GUID and z0.Finish=z3.GUID " +
                    "and ( ( (sign(z1.Lat-z3.Lat)*sign(z2.Lat-z3.Lat)<=0) or (sign(z1.Lat-z2.Lat)*sign(z2.Lat-z3.Lat)>=0) ) " +
                    "and ( (sign(z1.Lng-z3.Lng)*sign(z2.Lng-z3.Lng)<=0) or (sign(z1.Lng-z2.Lng)*sign(z2.Lng-z3.Lng)>=0) ) )");
            rs = query.executeQuery();
            while (rs.next()) {
                Caravan caravan = new Caravan (rs.getString("GUID"),rs.getString("PGUID"),rs.getInt("Lifetime"),rs.getInt("Danger"),rs.getString("Start"),rs.getString("Finish"),rs.getInt("bonus"),rs.getInt("Lat"),rs.getInt("Lng"),rs.getInt("LatS"),rs.getInt("LngS"),rs.getInt("LatF"),rs.getInt("LngF"),rs.getInt("Speed"),con);
                if (caravan.Lifetime>=180) {
                    caravan.Danger+=2;
                    caravan.generateAmbush();
                }
                caravan.finish();
                i++;
            }
            rs.close();
            query.close();
            con.commit();
        } catch (SQLException e) {
            MyUtils.Logwrite("World.handleFinishedCaravans", "SQL Error: " + e.toString());
        }
        MyUtils.Logwrite("World.handleFinishedCaravans", "Finish. "+Integer.toString(i)+" caravans finished.");
    }

    public static String Destroy() {
        PreparedStatement query;
        try {
            Connection con = DBUtils.ConnectDB();
            query = con.prepareStatement("truncate table Players");
            query.execute();
            query = con.prepareStatement("truncate table Caravans");
            query.execute();
            query = con.prepareStatement("truncate table Ambushes");
            query.execute();
            query = con.prepareStatement("truncate table GameObjects");
            query.execute();
            query = con.prepareStatement("truncate table PUpgrades");
            query.execute();
            query = con.prepareStatement("truncate table Cities");
            query.execute();
            query = con.prepareStatement("truncate table Connections");
            query.execute();
            query = con.prepareStatement("truncate table Messages");
            query.execute();
            query = con.prepareStatement("truncate table logs");
            query.execute();
            query = con.prepareStatement("update Fractions set Gold=0");
            query.execute();
            query.close();
            con.commit();
            con.close();
            return "World destroyed. MUHAHAHA!";
        } catch (SQLException |NamingException e) {return "Oops, God protected this world with "+e.toString();}
    }

    public static String Create() {
        PreparedStatement query;
        String Login,Password, ret="";
        //try {
            //Ростов
            Generate.newGenCity(47307347, 39589577, 47167543, 39893074); //2500
            //Кущевка
            Generate.newGenCity(46584512, 39586229, 46536827, 39680300); //332
            //Азов
            Generate.newGenCity(47122008, 39363756, 47053516, 39474306);
            //Батайск
            Generate.newGenCity(47174544, 39635410, 47063573, 39809474);
            //Таганрог
            Generate.newGenCity(47287147, 38827915, 47171044, 38958721);
            //Новочеркасск
            Generate.newGenCity(47539918, 40016670, 47371849, 40140953);
            //Шахты
            Generate.newGenCity(47775398, 40094688, 47666955, 40367557);
            //Родионовка
            Generate.newGenCity(47626411, 39690386, 47592544, 39735423);
            //Новошахтинск
            Generate.newGenCity(47851379, 39781321, 47716427, 40026110);

            //Талакан
            Generate.newGenCity(59829394, 110898403, 59820852, 110921709);
            //Краснодар
            Generate.newGenCity(45151233, 38873221, 44977501, 39112553);
            //Сочи
            Generate.newGenCity(43666979,39664046 ,43512949 ,39859716 );
            //Адлер
            Generate.newGenCity(43512949 ,39859716, 43386406, 40008288);
            //Волжский
            Generate.newGenCity(48899475 ,44714087, 48737706, 44877058);
            //Сургут
            Generate.newGenCity(61303298 ,73232971, 61230017, 73528915);
            //Павлодар
            Generate.newGenCity(52403985 ,76856997, 52231475, 77071231);
            //Москва
            Generate.newGenCity(55910412 ,37380301, 55578797, 37847906);
            //Тверь
            Generate.newGenCity(56935822 ,35717768, 56783416, 36055941);
            //Кувшиново
            Generate.newGenCity(57052705 ,34100969, 57004085, 34206815);
            //Торжок
            Generate.newGenCity(57079523 ,34923261, 57008536, 35027193);
            //Анапа
            Generate.newGenCity(44980452 ,37259552, 44867295, 37367698);
            //Ставрополь
            Generate.newGenCity(45163014 ,41851203, 44969511, 42089812);
            //Волгоград
            Generate.newGenCity(48890717 ,44378163, 48460466, 44646177);
            //Севастополь
            Generate.newGenCity(44843913 ,33378198, 44386314, 33894804);
            //Майкоп
            Generate.newGenCity(44645017 ,40051725, 44566555, 40156610);
            //Челябинск
            Generate.newGenCity(55322847 ,61229351, 55015772, 61578854);
            //Питер
            Generate.newGenCity(60093809 ,30079921, 59787361, 30558513);
            //Мстиславль
            Generate.newGenCity(54036278 ,31658804, 53998538, 31756818);
            //Симферополь
            Generate.newGenCity(45003123, 34024753, 44890315, 34192462);
            //Нижний Новгород
            Generate.newGenCity(56405287, 43676798, 56182760, 44139597);
            //Клин
            Generate.newGenCity(56379199, 36663374, 56309561, 36778387);
            //Пицунда
            Generate.newGenCity(43240125, 40271764, 43142255, 40462308);
            //Всеволжск
            Generate.newGenCity(60049576, 30586955, 59973900, 30724026);
            //Воронеж
            Generate.newGenCity(51874603, 39011551, 51482454, 39461885);


        //Упс, для общего плана надо переделать генерилку, добавить параметр расстояния между городами. Сейчас он 375 метров
            //Общий план
            //Generate.GenCity(49073865, 37655639, 46096090, 41943054, 332);
            return "Завершили генерацию городов";
       /*     Connection con = DBUtils.ConnectDB();
            query = con.prepareStatement("select Login, Password from Users");
            ResultSet rs=query.executeQuery();
            while (rs.next()){
                Login=rs.getString("Login");
                Password=rs.getString("Password");
                Player player = new Player();
                player.register(Login, Password);
            }
            con.commit();
            con.close();
            return "World created! \n"+ret;
        } catch (SQLException |NamingException e) {return "Dark Force prevents to create this world. "+e.toString()+"\n"+ret;}
        */
    }
}
