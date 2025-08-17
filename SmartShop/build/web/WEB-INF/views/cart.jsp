<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<h2>Giỏ hàng</h2>
<c:set var="sum" value="0"/>
<table>
  <tr><th>Sản phẩm</th><th>SL</th><th>Giá</th><th>Tạm tính</th><th></th></tr>
  <c:forEach var="e" items="${sessionScope.cart}">
    <c:set var="item" value="${e.value}"/>
    <tr>
      <td>${item.product.name}</td>
      <td>
        <form action="cart" method="post" style="display:inline">
          <input type="hidden" name="action" value="update"/>
          <input type="hidden" name="id" value="${item.product.id}"/>
          <input name="qty" value="${item.quantity}" size="2"/>
          <button>Cập nhật</button>
        </form>
      </td>
      <td>${item.product.price}</td>
      <td>${item.subtotal}</td>
      <td>
        <form action="cart" method="post" style="display:inline">
          <input type="hidden" name="action" value="remove"/>
          <input type="hidden" name="id" value="${item.product.id}"/>
          <button>Xóa</button>
        </form>
      </td>
    </tr>
    <c:set var="sum" value="${sum + item.subtotal}"/>
  </c:forEach>
</table>
<h3>Tổng: ${sum}</h3>
<%@ include file="footer.jspf" %>
