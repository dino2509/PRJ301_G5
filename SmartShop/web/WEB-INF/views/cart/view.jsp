<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<div class="container">
  <h2>Giỏ hàng</h2>

  <c:set var="size" value="${empty cart ? 0 : cart.size()}"/>
  <c:choose>
    <c:when test="${size == 0}">
      <p>Giỏ hàng trống.</p>
    </c:when>
    <c:otherwise>
      <table class="table">
        <thead><tr><th>Product ID</th><th>Qty</th></tr></thead>
        <tbody>
        <c:forEach var="e" items="${cart}">
          <tr><td>${e.key}</td><td>${e.value}</td></tr>
        </c:forEach>
        </tbody>
      </table>
      <form method="post" action="${pageContext.request.contextPath}/cart/clear">
        <button class="btn btn-outline-danger" type="submit">Xóa giỏ</button>
      </form>
    </c:otherwise>
  </c:choose>
</div>
