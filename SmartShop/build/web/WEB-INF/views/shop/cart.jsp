<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.math.BigDecimal,com.smartshop.model.CartItem,com.smartshop.util.ElFns" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>

<c:set var="accObj"
       value="${requestScope.accObj != null ? requestScope.accObj
               : (sessionScope.authUser != null ? sessionScope.authUser
               : (sessionScope.user != null ? sessionScope.user
               : sessionScope.account))}" />

<c:if test="${empty accFullName and accObj != null}">
  <c:set var="accFullName" value="${accObj.fullName}" />
</c:if>
<c:if test="${empty accPhone and accObj != null}">
  <c:set var="accPhone" value="${accObj.phone}" />
</c:if>
<c:if test="${empty accEmail and accObj != null}">
  <c:set var="accEmail" value="${accObj.email}" />
</c:if>
<c:if test="${empty accAddress and accObj != null}">
  <c:set var="accAddress" value="${accObj.address}" />
</c:if>

<%
    Map<Integer, CartItem> cart = (Map<Integer, CartItem>) session.getAttribute("cart");
    if (cart == null) { cart = new LinkedHashMap<>(); }
    BigDecimal total = BigDecimal.ZERO;
    for (CartItem it : cart.values()) { total = total.add(it.getSubtotal()); }

    // Thông tin account do CartServlet set
    String accFullName = (String) request.getAttribute("accFullName");
    String accPhone    = (String) request.getAttribute("accPhone");
    String accEmail    = (String) request.getAttribute("accEmail");
    String accAddress  = (String) request.getAttribute("accAddress");

    String error = (String) request.getAttribute("error");
    String message = (String) request.getAttribute("message");
%>
<!DOCTYPE html>
<html>
<head>
  <title>Cart</title>
  <style>
    .grid { display:grid; grid-template-columns: 1.5fr 1fr; gap:16px; align-items:start; }
    .paybox { border:1px solid #444; border-radius:8px; padding:12px; position:sticky; top:12px; }
    .muted { color:#aaa; font-size:90%; }
    table.cart1 { width:100%; border-collapse:collapse; display:flex;align-items:center;gap:10px;color:#fff;
      padding:10px 16px;border-radius:5px;border:0px solid #2a2d33}
    table.cart1 th, table.cart1 td { border:1px solid #333; padding:6px; vertical-align:middle; text-align:center}
    .right { text-align:right; }
    .btn { padding:6px 10px; }
    .field { display:block; margin:8px 0; }
    input[readonly], textarea[readonly]{ background:#111; color:#bbb; }
    .row { display:flex; gap:8px; }
    .row .col { flex:1; }
  </style>
</head>
<body>
<h1>Your Cart</h1>

<div class="grid">

  <!-- Cột trái: danh sách sản phẩm -->
  <div>
    <% if (cart.isEmpty()) { %>
      <p>Giỏ hàng trống.</p>
    <% } else { %>
      <table class="cart1">
        <tr>
          <th>ID</th><th>Sản phẩm</th><th>Tên sản phẩm</th><th>Đơn giá</th><th>Số lượng</th><th>Subtotal</th><th>Action</th>
        </tr>
        <% for (CartItem it : cart.values()) { %>
          <tr>
            <td><%= it.getProductId() %></td>
            <td>
              <% if (it.getImageUrl()!=null) { %>
                <img src="<%= it.getImageUrl() %>" alt="img" width="60"/>
              <% } %>
            </td>
            <td><a style="text-decoration: blink; font-size: 16px; font-weight: bold" href="product?id=<%= it.getProductId() %>"><%= it.getName() %></a></td>
            <td><%= ElFns.vnd(it.getPrice()) %></td>
            <td>
              <form method="post" action="<%= request.getContextPath() %>/cart/update">
                <input type="hidden" name="id" value="<%= it.getProductId() %>"/>
                <input style="width: 40px" type="number" name="qty" value="<%= it.getQuantity() %>" min="1"/>
                <button class="btn" type="submit">Update</button>
              </form>
            </td>
            <td><%= ElFns.vnd(it.getSubtotal()) %></td>
            <td>
              <form method="post" action="<%= request.getContextPath() %>/cart/remove" onsubmit="return confirm('Xoá sản phẩm này?')">
                <input type="hidden" name="id" value="<%= it.getProductId() %>"/>
                <button class="btn" type="submit">Remove</button>
              </form>
            </td>
          </tr>
        <% } %>
      </table>
    <% } %>
  </div>

  <!-- Cột phải: panel thanh toán + form thông tin -->
  <div class="paybox" id="paybox">
    <h3>Thanh toán</h3>

    <% if (message != null) { %>
      <div style="color:#22c55e;border:1px solid #22c55e;padding:8px;margin:8px 0"><%= message %></div>
    <% } %>
    <% if (error != null) { %>
      <div style="color:#f87171;border:1px solid #f87171;padding:8px;margin:8px 0"><%= error %></div>
    <% } %>

    <p><strong>Tổng thanh toán:</strong> <%= ElFns.vnd(total) %></p>

    <form method="post" action="<%= request.getContextPath() %>/cart" id="orderForm">
      <input type="hidden" name="action" value="placeOrder"/>

      <!-- Chọn nguồn dữ liệu -->
      <div class="field">
        <label><input type="radio" name="useAccount" id="useAcc1" value="1" checked> Dùng thông tin trong tài khoản</label>
        <label style="margin-left:12px"><input type="radio" name="useAccount" id="useAcc0" value="0"> Tuỳ chỉnh</label>
      </div>

      <!-- Thông tin giao hàng -->
<label class="field">Họ và tên
  <input type="text" name="fullName" id="fullName"
         value="${empty accFullName ? '' : accFullName}"
         data-acc="${empty accFullName ? '' : accFullName}">
</label>

<div class="row">
  <label class="field col">Số điện thoại
    <input type="text" name="phone" id="phone"
           value="${empty accPhone ? '' : accPhone}"
           data-acc="${empty accPhone ? '' : accPhone}">
  </label>
  <label class="field col">Email
    <input type="email" name="email" id="email"
           value="${empty accEmail ? '' : accEmail}"
           data-acc="${empty accEmail ? '' : accEmail}">
  </label>
</div>

<label class="field">Địa chỉ giao hàng
  <input type="text" name="address" id="address"
         value="${empty accAddress ? '' : accAddress}"
         data-acc="${empty accAddress ? '' : accAddress}">
</label>

      <!-- Phương thức thanh toán -->
      <div class="field">
        <strong>Phương thức thanh toán</strong><br>
        <label><input type="radio" name="pm" value="COD" checked> COD</label>
        <label style="margin-left:12px"><input type="radio" name="pm" value="WALLET"> Ví nội bộ</label>
        <label style="margin-left:12px"><input type="radio" name="pm" value="GATEWAY"> Cổng giả lập</label>
      </div>

      <button class="btn" type="submit" <%= cart.isEmpty() ? "disabled" : "" %>>Đặt hàng</button>
    </form>
  </div>
</div>

<script>
(function(){
  const radios = document.querySelectorAll('input[name="useAccount"]');
  const fields = ['fullName','phone','email','address']
                  .map(id => document.getElementById(id));

  function usingAccount(){
    const sel = document.querySelector('input[name="useAccount"]:checked');
    return sel && sel.value === '1';
  }
  function applyMode(){
    const lock = usingAccount();
    fields.forEach(el => {
      if(!el) return;
      el.readOnly = lock;
      if(lock){
        const acc = el.dataset.acc || '';
        el.value = acc;
      }
    });
  }
  radios.forEach(r => r.addEventListener('change', applyMode));
  applyMode();
})();
</script>

</body>
</html>
