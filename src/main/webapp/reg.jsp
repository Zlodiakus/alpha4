<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Player"%>
<%
String Login=request.getParameter("Login");
String Password=request.getParameter("Password");
String EMail=request.getParameter("EMail");
String Invite=request.getParameter("Invite");
Player player=new Player();
String result=player.userRegister(Login,Password, EMail,Invite);
%>
<%=result%>