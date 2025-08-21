<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<h2>My Wallet</h2>
<p>Balance: <strong><fmt:formatNumber value="${balance}" type="currency"/></strong></p>
<form method="post" style="margin:12px 0;">
  <input type="hidden" name="action" value="topup_request"/>
  <label>Amount <input name="amount" type="number" min="10000" step="1000" required/></label>
  <button type="submit">Request topup</button>
</form>

<h3>Recent transactions</h3>
<table border="1" cellpadding="6" cellspacing="0">
  <tr><th>ID</th><th>Type</th><th>Amount</th><th>Status</th><th>Order</th><th>When</th></tr>
  <c:forEach var="t" items="${txs}">
    <tr>
      <td>${t.id}</td>
      <td>${t.type}</td>
      <td><fmt:formatNumber value="${t.amount}" type="currency"/></td>
      <td>${t.status}</td>
      <td><c:out value="${t.refOrderId}"/></td>
      <td>${t.createdAt}</td>
    </tr>
  </c:forEach>
</table>
