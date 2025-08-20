<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<nav>
  <a href="${pageContext.request.contextPath}/admin/stats">Stats</a> |
  <strong>Products</strong> |
  <a href="${pageContext.request.contextPath}/admin/users">Users</a> |
  <a href="${pageContext.request.contextPath}/admin/orders">Orders</a>
</nav>
<h2>Admin · Products</h2>
<p><a href="${pageContext.request.contextPath}/admin/products?action=new">+ New Product</a></p>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>ID</th><th>Name</th><th>Price</th><th>Stock</th><th>Active</th><th>Actions</th></tr>
  <c:forEach var="p" items="${list}">
    <tr>
      <td>${p.id}</td><td>${p.name}</td><td>${p.price}</td><td>${p.stock}</td><td>${p.active}</td>
      <td>
        <a href="${pageContext.request.contextPath}/admin/products?action=edit&id=${p.id}">Edit</a>
        <a href="${pageContext.request.contextPath}/admin/products?action=delete&id=${p.id}" onclick="return confirm('Delete?')">Delete</a>
      </td>
    </tr>
  </c:forEach>
</table>
