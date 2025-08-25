<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<nav>
  <a href="${pageContext.request.contextPath}/admin/stats">Stats</a> |
  <a href="${pageContext.request.contextPath}/admin/products">Products</a> |
  <a href="${pageContext.request.contextPath}/admin/users">Users</a> |
  <strong>Orders</strong>
</nav>
<h2>Admin · Orders</h2>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>ID</th><th>User</th><th>Status</th><th>Total</th><th>Created</th><th>Actions</th></tr>
  <c:forEach var="o" items="${orders}">
    <tr>
      <td>${o[0]}</td><td>${o[1]}</td><td>${o[2]}</td><td>${o[3]}</td><td>${o[4]}</td>
      <td>
        <form method="post" style="display:inline-block">
          <input type="hidden" name="action" value="status"/>
          <input type="hidden" name="id" value="${o[0]}"/>
          <select name="status">
            <option ${o[2]=='PENDING'?'selected':''}>PENDING</option>
            <option ${o[2]=='PROCESSING'?'selected':''}>PROCESSING</option>
            <option ${o[2]=='SHIPPED'?'selected':''}>SHIPPED</option>
            <option ${o[2]=='DELIVERED'?'selected':''}>DELIVERED</option>
            <option ${o[2]=='CANCELED'?'selected':''}>CANCELED</option>
          </select>
          <button>Update</button>
        </form>
        <form method="post" style="display:inline-block" onsubmit="return confirm('Delete unpaid order?')">
          <input type="hidden" name="action" value="delete"/>
          <input type="hidden" name="id" value="${o[0]}"/>
          <button>Delete</button>
        </form>
      </td>
    </tr>
  </c:forEach>
</table>
