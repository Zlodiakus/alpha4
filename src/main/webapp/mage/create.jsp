<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Generate"%>
<%
String result;
try
{


String countS=request.getParameter("COUNT");
int count=Integer.parseInt(countS);
String xS=request.getParameter("X");
String yS=request.getParameter("Y");
int x=Integer.parseInt(xS);
int y=Integer.parseInt(yS);
result=Generate.GenCity(x,y,count);
}catch (Exception e)
{
result=e.toString();
}

%>
<%=result%>