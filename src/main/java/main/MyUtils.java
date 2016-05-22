package main;
//This code was freely adapted from http://www.movable-type.co.uk/scripts/latlong-vincenty.html

import javax.naming.NamingException;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Utility Functions
 */
public class MyUtils {
	/**
	 * Count distance in meters
	 * @param lat1 Latitude of start point
	 * @param lng1 Longtitude of start point
	 * @param lat2 Latitude of end point
	 * @param lng2 Longtitude of end point
	 * @return Distance in meters
	 */
	public static double distVincenty(int lat1, int lng1, int lat2, int lng2) {
		return (double) Math.round(6378137 * Math.acos(Math.cos(lat1 / 1e6 * Math.PI / 180) *
				Math.cos(lat2 / 1e6 * Math.PI / 180) * Math.cos(lng1 / 1e6 * Math.PI / 180 - lng2 / 1e6 * Math.PI / 180) +
				Math.sin(lat1 / 1e6 * Math.PI / 180) * Math.sin(lat2 / 1e6 * Math.PI / 180)));
	}

	public static double RangeCheck(int lat1, int lng1, int lat2, int lng2) {
		return (double) Math.round(6378137 * Math.acos(Math.cos(lat1 / 1e6 * Math.PI / 180) *
				Math.cos(lat2 / 1e6 * Math.PI / 180) * Math.cos(lng1 / 1e6 * Math.PI / 180 - lng2 / 1e6 * Math.PI / 180) +
				Math.sin(lat1 / 1e6 * Math.PI / 180) * Math.sin(lat2 / 1e6 * Math.PI / 180)));
	}

	public static double distVincentyOld(double lat1, double lon1, double lat2, double lon2) {
		double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563; // WGS-84 ellipsoid params
		double L = Math.toRadians(lon2 - lon1);
		double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
		double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
	    double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
	    double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

	    double sinLambda, cosLambda, sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM;
	    double lambda = L, lambdaP, iterLimit = 100;
	    do {
	        sinLambda = Math.sin(lambda);
	        cosLambda = Math.cos(lambda);
	        sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda)
	                + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
	        if (sinSigma == 0)
	            return 0; // co-incident points
	        cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
	        sigma = Math.atan2(sinSigma, cosSigma);
	        sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
	        cosSqAlpha = 1 - sinAlpha * sinAlpha;
	        cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
	        if (Double.isNaN(cos2SigmaM))
	            cos2SigmaM = 0; // equatorial line: cosSqAlpha=0 (§6)
	        double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
	        lambdaP = lambda;
	        lambda = L + (1 - C) * f * sinAlpha
	                * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
	    } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);

	    if (iterLimit == 0)
	        return Double.NaN; // formula failed to converge

	    double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
	    double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
	    double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
	    double deltaSigma = B
	            * sinSigma
	            * (cos2SigmaM + B
	                    / 4
	                    * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
	                            * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
		return b * A * (sigma - deltaSigma);


	}

	/**
	 * Пересечение отрезка и окружности
	 *
	 * @param x1 Начало отрезка
	 * @param y1 Начало отрезка
	 * @param x2 Конец отрезка
	 * @param y2 Конец отрезка
	 * @param xC Центр Окружности
	 * @param yC Центр Окружности
	 * @param R  Радиус окружности
	 * @return true если пересекает
	 * <p/>
	 * todo: Доделать для сферических координат координат.
	 */
	public static boolean commonSectionCircle(double x1, double y1, double x2, double y2,
											  double xC, double yC, double R) {
		x1 -= xC;
		y1 -= yC;
		x2 -= xC;
		y2 -= yC;

		double dx = x2 - x1;
		double dy = y2 - y1;

		//составляем коэффициенты квадратного уравнения на пересечение прямой и окружности.
		//если на отрезке [0..1] есть отрицательные значения, значит отрезок пересекает окружность
		double a = dx * dx + dy * dy;
		double b = 2. * (x1 * dx + y1 * dy);
		double c = x1 * x1 + y1 * y1 - R * R;

		//а теперь проверяем, есть ли на отрезке [0..1] решения
		if (-b < 0)
			return (c < 0);
		if (-b < (2. * a))
			return ((4. * a * c - b * b) < 0);

		return (a + b + c < 0);
	}

	public static ArrayList<Point> createCitiesOnMap(int width, int height, int citycount)
	{
		ArrayList<Point> cityarr = new ArrayList<>();

		double i;
		double j;
		//double size_square=Math.sqrt((width*height)/citycount);
		double size_i = width / Math.sqrt(citycount);
		double size_j = height / Math.sqrt(citycount);
		for (i = 0; i < width; i += size_i)
			for (j = 0; j < height; j += size_j)
			{
				cityarr.add(new Point((int) (size_i/8 + Math.random() * (size_i*3/4) + i), (int) (size_j/8 + Math.random() * (size_j*3/4) + j)));
			}
		return cityarr;
	}

	public static ArrayList<Point> newCreateCitiesOnMap(int Lat1, int Lng1, int Lat2, int Lng2)
	{
		boolean stop;
		int Lat=(int)((Lat1+Lat2)/2000000);
		int delta_lat=(int)(1000000*Math.asin((180/3.1415926)*125/(6378137))); //это 125 метров
		int delta_lng=(int)(1000000*Math.asin((180/3.1415926)*125/(6378137*Math.cos(Lat*3.1415926/180)))); //и это 125 метров
		//int delta_lat2=(int)(1000000*Math.asin((180/3.1415926)*250/(2*6378137))); //это 250 метров
		//int delta_lng2=(int)(1000000*Math.asin((180/3.1415926)*250/(2*6378137*Math.cos(Lat*3.1415926/180)))); //и это 250 метров
		int delta_lat_rand=(int)(1000000*Math.asin((180/3.1415926)*200/(6378137))); //это 200 метров
		int delta_lng_rand=(int)(1000000*Math.asin((180/3.1415926)*200/(6378137*Math.cos(Lat*3.1415926/180)))); //и это 150 метров
		Random random=new Random();
		ArrayList<Point> cityarr = new ArrayList<>();

		int tempLat=Lat1;
		int tempLng=Lng1;
		int newX=Lng1;
		int newY=Lat1;
		stop=false;
		while (tempLat<Lat2) {
			while (tempLng < Lng2) {
				if (Math.random()*100>=25) {
					newY=(int) (tempLat + delta_lat + Math.random() * delta_lat_rand);
					newX=(int) (tempLng + delta_lng + Math.random() * delta_lng_rand);
					for (Point ttt : cityarr) {
						if ( (Math.abs(ttt.getX()-newY)<2*delta_lat ) && (Math.abs(ttt.getY()-newX)<2*delta_lng) )
							if (MyUtils.RangeCheck((int)ttt.getY(),(int)ttt.getX(),newX,newY)<230) stop=true;
					}
					if (stop) stop=false;
					else
					cityarr.add(new Point(newY, newX));
				}
				tempLng = newX+delta_lng;
			}
			tempLng=Lng1;
			tempLat=newY+delta_lat;
		}
		return cityarr;
	}

    public static String getJSONError(String errortype, String errormessage) {
        return "{Result:" + '"' + "Error" + '"' + ",Code:" + '"' + errortype + '"' + ",Message:" + '"' + errormessage + '"' + "}";
    }
	public static String getJSONSuccess(String message) {
		return "{Result:" + '"' + "Success" + '"' + ",Message:" + '"' + message + '"' + "}";
	}

	//private static DateFormat DTMS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.S");

	public static void Logwrite(String str1,String str2) {
//Connects:"+Integer.toString(DBUtils.getConCount())
		Date date = new Date();
		DateFormat DTMS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.S");
		Connection con;
		PreparedStatement query;
		try {
			con = DBUtils.ConnectDB();
			query=con.prepareStatement("insert into logs(module,text,logtime,Con) values (?,?,?,?)");
//			query=con.prepareStatement("insert into logs(module,text,logtime) values (?,?,CONVERT_TZ(NOW(),'+00:00','+08:00'))");
			query.setString(1,str1);
			query.setString(2,str2);
			query.setString(3,DTMS.format(date));
			query.setInt(4,DBUtils.getConCount());
			query.execute();
			con.commit();
			query.close();
			con.close();
		} catch (SQLException | NamingException e) {
			//DBUtils.closeAllConnection();
			try {
				con = DBUtils.ConnectDB();
				query=con.prepareStatement("insert into logs(module,text,logtime) values (?,?,?)");
//				query=con.prepareStatement("insert into logs(module,text,logtime) values (?,?,CONVERT_TZ(NOW(),'+00:00','+08:00'))");
				query.setString(1,str1);
				query.setString(2,"Connections dropped on "+str2+". Error: "+e.toString());
				query.setString(3,DTMS.format(date));
				query.execute();
				con.commit();
				query.close();
				con.close();
			} catch (SQLException | NamingException e1) {
			}
		}

	}

	public static void Logwrite(String str1,String str2, long val) {
		Date date = new Date();
		DateFormat DTMS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.S");
		Connection con;
		String result;
		PreparedStatement query;
		try {
			con = DBUtils.ConnectDB();
			query=con.prepareStatement("insert into logs(module,text,logtime, VAL, Con) values (?,?,?,?,?)");
//			query=con.prepareStatement("insert into logs(module,text,logtime, VAL) values (?,?,CONVERT_TZ(NOW(),'+00:00','+08:00'),?)");
			query.setString(1,str1);
			query.setString(2, str2);
			query.setString(3,DTMS.format(date));
			query.setLong(4,val);
			query.setInt(5,DBUtils.getConCount());
			query.execute();
			con.commit();
			query.close();
			con.close();
		} catch (SQLException | NamingException e) {
			//DBUtils.closeAllConnection();
			try {
				con = DBUtils.ConnectDB();
				query=con.prepareStatement("insert into logs(module,text,logtime) values (?,?,?)");
//				query=con.prepareStatement("insert into logs(module,text,logtime) values (?,?,CONVERT_TZ(NOW(),'+00:00','+08:00'))");
				query.setString(1,str1);
				query.setString(2,"Connections dropped on "+str2+". Error: "+e.toString());
				query.setString(3,DTMS.format(date));
				query.execute();
				con.commit();
				query.close();
				con.close();
			} catch (SQLException | NamingException e1) {
			}
		}
	}


	public static void Message(String PGUID, String Message, int Type, int State, int Lat, int Lng) {
		Connection con;
		PreparedStatement query;
		String MGUID= UUID.randomUUID().toString();
		try {
			con = DBUtils.ConnectDB();
			query=con.prepareStatement("insert into Messages(GUID,PGUID,Message,Type,State,Lat,Lng,Time) values (?,?,?,?,?,?,?,NOW())");
			query.setString(1,MGUID);
			query.setString(2,PGUID);
			query.setString(3,Message);
			query.setInt(4,Type);
			query.setInt(5,State);
			query.setInt(6,Lat);
			query.setInt(7,Lng);
			query.execute();
			con.commit();
			query.close();
			con.close();
		} catch (SQLException | NamingException e) {
			Logwrite("MyUtils.Message","SQL Error: "+e.toString());
		}
	}


}
