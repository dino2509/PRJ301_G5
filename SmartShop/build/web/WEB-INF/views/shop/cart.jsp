<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<h2>Your Cart</h2>
<c:if test="${empty items}">Cart is empty.</c:if>
<c:if test="${not empty items}">
<table border="1" cellspacing="0" cellpadding="6">
  <tr><th>Product</th><th>Price</th><th>Qty</th><th>Total</th><th>Actions</th></tr>
  <c:forEach var="it" items="${items}">
    <tr>
      <td>${it.productName}</td>
      <td>${it.unitPrice}</td>
      <td>
        <form action="${pageContext.request.contextPath}/cart/update" method="post" style="display:inline-block">
          <input type="hidden" name="id" value="${it.id}"/>
          <input type="number" name="qty" value="${it.qty}" min="1" style="width:60px"/>
          <button type="submit">Update</button>
        </form>
      </td>
      <td>${it.lineTotal}</td>
      <td><a href="${pageContext.request.contextPath}/cart/remove?id=${it.id}">Remove</a></td>
    </tr>
  </c:forEach>
  <tr><td colspan="3" align="right"><strong>Total</strong></td><td colspan="2"><strong>${total}</strong></td></tr>
</table>
<p><a href="${pageContext.request.contextPath}/checkout">Proceed to Checkout</a></p>
</c:if>
