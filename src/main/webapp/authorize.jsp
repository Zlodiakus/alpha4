<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="google.Authorize"%>
<%
String result=Authorize.create(request);
%>
<%=result%>