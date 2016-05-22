<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.DBUtils"%>
<%
String result=DBUtils.closeAllConnection();
%>
<%=result%>