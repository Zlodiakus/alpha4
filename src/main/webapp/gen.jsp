<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Generate"%>
<%
String result="test";
Generate generate = new Generate();
result=generate.genKvant();
%>
<%=result%>