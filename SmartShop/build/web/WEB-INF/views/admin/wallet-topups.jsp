<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<h2>Pending wallet topups</h2>
<table border="1" cellpadding="6" cellspacing="0">
  <tr><th>ID</th><th>User</th><th>Amount</th><th>Created</th><th>Action</th></tr>
  <c:forEach var="r" items="${rows}">
    <tr>
      <td>${r[0]}</td><td>${r[1]}</td><td><fmt:formatNumber value="${r[2]}" type="currency"/></td><td>${r[3]}</td>
      <td>
        <form method="post" style="display:inline"><input type="hidden" name="txId" value="${r[0]}"/><input type="hidden" name="action" value="approve"/><button>Approve</button></form>
        <form method="post" style="display:inline"><input type="hidden" name="txId" value="${r[0]}"/><input type="hidden" name="action" value="reject"/><button>Reject</button></form>
      </td>
    </tr>
  </c:forEach>
</table>
