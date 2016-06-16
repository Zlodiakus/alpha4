package main;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Well on 19.02.2016.
 */
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class BackGround {
    @Schedule(hour="*", minute="*", second="0", persistent=false)
    public void WorldMove() throws SQLException, NamingException {
        MyUtils.Logwrite("BackGround","Started");
        World world = new World();
        world.moveFast();
        world.close();
        MyUtils.Logwrite("BackGround","Finished");

    }
    @Schedule(hour="*", minute="0", second="10", persistent=false)
    public void WorldMoveHour() throws SQLException, NamingException {
        MyUtils.Logwrite("BackGround.Hour","Started");
        World world = new World();
        world.moveHour();
        world.close();
        MyUtils.Logwrite("BackGround.Hour","Finished");
    }
}
