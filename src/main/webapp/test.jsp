<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Caravan"%>
<%
String result="Empty test passed";
Caravan caravan=new Caravan();
result=caravan.fix();
%>
<%=result%>