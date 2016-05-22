<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Generate"%>
<%

String result="null";
result=main.Generate.genInvites();
%>
<%=result%>