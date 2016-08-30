<%@ page language="java" contentType="text/html; charset=UTF8"
    pageEncoding="UTF8"%>
    <%@page import="main.Rating"%>
<%
String token=Rating.getFullRate();
%>
<html>
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
    <link rel="stylesheet" href="bootstrap.min.css">
    <title>Рейтинг</title>
    <style>
        body {
            margin: 15px;
        }

        td.r {
            text-align: right;
        }
    </style>
</head>
<body>
<div class="col-lg-7">
    <table class="table table-striped table-bordered">
        <thead>
        <tr>
            <th>№</th>
            <th>Имя</th>
            <th>Уровень</th>
            <th>Опыт</th>
            <th>Золото</th>
            <th>Корованов</th>
            <th>Доход<br>в час</th>
            <th>Награблено</th>
            <th>Награда за <br>уничтожение засад</th>
        </tr>
        </thead>
        <tbody>
<%=token%>
        </tbody>
    </table>
</div>
</body>
</html>
