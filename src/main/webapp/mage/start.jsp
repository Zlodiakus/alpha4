<%@ page language="java" contentType="text/plain; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.World"%>
<%
//out.print("Da eb vashy mat!");
World obj=new World();
out.print("World created.");
String result=obj.StartTask();
%>
<%=result%>