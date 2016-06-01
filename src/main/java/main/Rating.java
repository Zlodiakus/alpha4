package main;

import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Shadilan
 */
public class Rating {
    public static String getFullRate(){
        Connection con = null;
        String ccolor;
        int i=0;
        //String result="<table><tr><td>№</td><td>Имя</td><td>Уровень</td><td>Золото</td><td>Опыт</td><td>Корованов</td><td>Сумарная дистанция</td><td>Максимальная дистанция</td><td>Средняя дистанция</td></tr>";
        String result="";
        try {
            con=DBUtils.ConnectDB();
            //PreparedStatement stmt=con.prepareStatement("select  p.Name ,p.Level ,p.Gold ,p.Exp ,count(c.Distance) cor_count,sum(c.Distance) sum_distance,Max(c.Distance) max_distance,round(avg(c.Distance)) avg_distance, (select Name from Fractions where Id=p.Race) from Players p left join Caravans c on (p.GUID=c.PGUID and c.Finish is not null) WHERE p.Name !=  'Elf' group by p.Name,p.Level,p.Gold,p.Exp order by Exp desc");
            PreparedStatement stmt=con.prepareStatement("select  p.Name ,p.Level, p.Exp, p.Gold,count(c.Distance) cor_count,sum(c.Distance) sum_distance,Max(c.Distance) max_distance,round(avg(c.Distance)) avg_distance, p.Race, sum(c.profit) profit, s.ambushed, s.paladined from Players p left join Caravans c on (p.GUID=c.PGUID and c.Finish is not null) left join Stats s on (p.GUID=s.PGUID) WHERE p.Name !=  'Elf' group by p.Name,p.Level,p.Gold,p.Exp order by Exp desc");
            ResultSet rs=stmt.executeQuery();
            rs.beforeFirst();
            while (rs.next()){
                i=i+1;
                ccolor="primary";
                if (rs.getInt(9)==1) {
                    ccolor="info";
                }
                if (rs.getInt(9)==2) {
                    ccolor="danger";
                }
                if (rs.getInt(9)==3) {
                    ccolor="warning";
                }
                result=result+"<tr class="+ccolor+"><td>"+i+"</td><td><b>"+rs.getString(1)+"</b></td><td>"+rs.getInt(2)+"</td><td>"+rs.getInt(3)+"</td><td>"+rs.getInt(4)+"</td><td>"+rs.getInt(5)+"</td><td>"+rs.getInt(6)+"</td><td>"+rs.getInt(7)+"</td><td>"+rs.getInt(8)+"</td><td>"+rs.getInt(10)+"</td><td>"+rs.getInt(11)+"</td><td>"+rs.getInt(12)+"</td></tr>";
            }
            //result=result+"</table>";
            con.close();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            try {
                if (con!=null && !con.isClosed()) con.close();
                e.printStackTrace();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return result;

    }
}
