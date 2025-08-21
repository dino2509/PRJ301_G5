<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<h2>Fake Payment</h2>
<p>Order #${orderId}</p>
<p>Amount: <strong><fmt:formatNumber value="${amount}" type="currency"/></strong></p>
<form method="post">
  <input type="hidden" name="orderId" value="${orderId}"/>
  <button name="action" value="success">Thanh toán thành công</button>
  <button name="action" value="fail">Hủy</button>
</form>
