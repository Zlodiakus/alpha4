<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Player"%>
<%

String result="null";
Player player=new Player();
result=player.getInvite("1","2");
%>
<%=result%>