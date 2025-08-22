<!-- src/main/webapp/WEB-INF/views/shop/checkout.jsp -->
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Checkout</h2>

<c:if test="${not empty error}">
  <div style="color:#b91c1c;border:1px solid #b91c1c;padding:8px;margin:8px 0">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/checkout" style="max-width:560px">
  <label style="display:block;margin:8px 0">
    <input type="checkbox" name="useAccount" value="1" checked>
    Dùng thông tin trong tài khoản
  </label>

  <div style="display:grid;grid-template-columns:1fr;gap:10px;margin:12px 0">
    <label>Họ và tên
      <input type="text" name="fullName" value="${acc_fullName}" style="width:100%" />
    </label>
    <label>Số điện thoại
      <input type="text" name="phone" value="${acc_phone}" style="width:100%" />
    </label>
    <label>Email
      <input type="email" name="email" value="${acc_email}" style="width:100%" />
    </label>
    <label>Địa chỉ giao hàng
      <textarea name="address" rows="3" style="width:100%">${acc_address}</textarea>
    </label>
  </div>

  <fieldset style="margin:12px 0">
    <legend>Phương thức thanh toán</legend>
    <label><input type="radio" name="paymentMethod" value="COD"> Thanh toán khi nhận hàng (COD)</label><br/>
    <label><input type="radio" name="paymentMethod" value="WALLET"> Ví nội bộ</label><br/>
    <label><input type="radio" name="paymentMethod" value="GATEWAY"> Cổng thanh toán giả lập</label>
  </fieldset>

  <button type="submit">Đặt hàng</button>
</form>
