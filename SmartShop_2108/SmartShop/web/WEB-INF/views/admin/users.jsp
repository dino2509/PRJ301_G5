<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<nav>
  <a href="${pageContext.request.contextPath}/admin/stats">Stats</a> |
  <a href="${pageContext.request.contextPath}/admin/products">Products</a> |
  <strong>Users</strong> |
  <a href="${pageContext.request.contextPath}/admin/orders">Orders</a>
</nav>
<h2>Admin · Users</h2>
<form method="get"><input name="q" value="${param.q}" placeholder="Search users"/><button>Search</button></form>
<h3>Create</h3>
<form method="post">
  <input type="hidden" name="action" value="create"/>
  <input name="username" placeholder="username" required/>
  <input name="email" placeholder="email">
  <input name="full_name" placeholder="full name">
  <input name="phone" placeholder="phone">
  <input name="password" placeholder="password" required>
  <select name="role"><option value="USER">USER</option><option value="ADMIN">ADMIN</option></select>
  <button type="submit">Add</button>
</form>
<h3>List</h3>
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>ID</th><th>Username</th><th>Email</th><th>Name</th><th>Phone</th><th>Status</th><th>Actions</th></tr>
  <c:forEach var="u" items="${list}">
    <tr>
      <td>${u.id}</td><td>${u.username}</td><td>${u.email}</td><td>${u.fullName}</td><td>${u.phone}</td><td>${u.status}</td>
      <td>
        <form method="post" style="display:inline-block">
          <input type="hidden" name="action" value="update"/>
          <input type="hidden" name="id" value="${u.id}"/>
          <input name="email" value="${u.email}"/>
          <input name="full_name" value="${u.fullName}"/>
          <input name="phone" value="${u.phone}"/>
          <select name="status"><option ${u.status=='ACTIVE'?'selected':''}>ACTIVE</option><option ${u.status=='LOCKED'?'selected':''}>LOCKED</option></select>
          <button>Save</button>
        </form>
        <form method="post" style="display:inline-block" onsubmit="return confirm('Delete?')">
          <input type="hidden" name="action" value="delete"/>
          <input type="hidden" name="id" value="${u.id}"/>
          <button>Delete</button>
        </form>
      </td>
    </tr>
  </c:forEach>
</table>
