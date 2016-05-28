<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Authorize"%>
<%
String result=Authorize.create(request);
%>
<%=result%>