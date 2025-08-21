<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<h2>Checkout</h2>
<form method="post">
  <label>Name <input name="name" required/></label><br/>
  <label>Phone <input name="phone" required/></label><br/>
  <label>Address <input name="address" style="width:400px" required/></label><br/>
  <label>Payment method
    <select name="payment">
      <option value="COD">COD</option>
      <option value="WALLET">Wallet balance</option>
      <option value="FAKE">Fake gateway (demo)</option>
    </select>
  </label><br/>
  <button type="submit">Place Order</button>
</form>
