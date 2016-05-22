<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.DBUtils"%>
<%
int cnt=DBUtils.getConCount();
%>
<%=cnt%>