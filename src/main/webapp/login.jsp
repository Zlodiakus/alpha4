<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Player"%>
<%
String Login=request.getParameter("Login");
String Password=request.getParameter("Password");

Player obj=new Player();
String token=obj.GetToken(Login, Password);
%>
<%=token%>