<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<nav>
  <strong>Stats</strong> |
  <a href="${pageContext.request.contextPath}/admin/products">Products</a> |
  <a href="${pageContext.request.contextPath}/admin/users">Users</a> |
  <a href="${pageContext.request.contextPath}/admin/orders">Orders</a>
</nav>
<h2>Admin · Statistics</h2>
<h3>Revenue by day</h3>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>Date</th><th>Revenue</th></tr>
  <c:forEach var="e" items="${revDay}"><tr><td>${e.key}</td><td>${e.value}</td></tr></c:forEach>
</table>
<h3>Revenue by month</h3>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>Month</th><th>Revenue</th></tr>
  <c:forEach var="e" items="${revMonth}"><tr><td>${e.key}</td><td>${e.value}</td></tr></c:forEach>
</table>
<h3>Best selling products</h3>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>Product ID</th><th>Quantity</th></tr>
  <c:forEach var="r" items="${best}"><tr><td>${r[0]}</td><td>${r[1]}</td></tr></c:forEach>
</table>
<h3>Top rated products</h3>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>Product ID</th><th>Avg rating</th><th>Reviews</th></tr>
  <c:forEach var="r" items="${top}"><tr><td>${r[0]}</td><td>${r[1]}</td><td>${r[2]}</td></tr></c:forEach>
</table>
<h3>Products by category</h3>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>Category</th><th>Count</th></tr>
  <c:forEach var="r" items="${pcat}"><tr><td>${r[0]}</td><td>${r[1]}</td></tr></c:forEach>
</table>
<h3>New customers</h3>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>Date</th><th>New users</th></tr>
  <c:forEach var="r" items="${newCus}"><tr><td>${r[0]}</td><td>${r[1]}</td></tr></c:forEach>
</table>
