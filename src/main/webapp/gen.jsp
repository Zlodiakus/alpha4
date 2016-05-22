<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Generate"%>
<%
String countS=request.getParameter("count");
int count=Integer.parseInt(countS);
String result="test";
result=main.Generate.GenCity(47314330, 39576530,47184754, 39916420, count);
%>
<%=result%>