<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Generate"%>
<%
String result;
try
{
String countS=request.getParameter("COUNT");
String Lat1S=request.getParameter("Lat1");
String Lat2S=request.getParameter("Lat2");
String Lng1S=request.getParameter("Lng1");
String Lng2S=request.getParameter("Lng2");
int LAT1=Integer.parseInt(Lat1S);
int LNG1=Integer.parseInt(Lng1S);
int LAT2=Integer.parseInt(Lat2S);
int LNG2=Integer.parseInt(Lng2S);
result=Generate.newGenCity(LAT1,LNG1,LAT2,LNG2);
} catch (Exception e)
{
result=e.toString();
}

%>

<%=result%>