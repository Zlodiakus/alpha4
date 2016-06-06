<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Generate"%>
<%
//String countS=request.getParameter("count");
//int count=Integer.parseInt(countS);
String result="test";
main.Generate.genKvant();
%>
<%=result%>