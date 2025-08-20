<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*,java.math.BigDecimal,com.smartshop.model.CartItem" %>
<%
Map<Integer, CartItem> cart = (Map<Integer, CartItem>) session.getAttribute("cart");
if (cart == null) { cart = new LinkedHashMap<>(); }
BigDecimal total = BigDecimal.ZERO;
%>
<!DOCTYPE html><html><head><title>Cart</title></head><body>
<h1>Your Cart</h1>
<table border="1" cellpadding="6">
<tr><th>ID</th><th>Name</th><th>Price</th><th>Qty</th><th>Subtotal</th><th>Action</th></tr>
<% for (CartItem it : cart.values()) { BigDecimal sub = it.getSubtotal(); total = total.add(sub); %>
<tr>
<td><%= it.getProductId() %></td>
<td><%= it.getName() %></td>
<td><%= it.getPrice() %></td>
<td>
  <form method="post" action="<%= request.getContextPath() %>/cart/update">
    <input type="hidden" name="id" value="<%= it.getProductId() %>"/>
    <input type="number" name="qty" value="<%= it.getQuantity() %>" min="1"/>
    <button type="submit">Update</button>
  </form>
</td>
<td><%= sub %></td>
<td>
  <form method="post" action="<%= request.getContextPath() %>/cart/remove">
    <input type="hidden" name="id" value="<%= it.getProductId() %>"/>
    <button type="submit">Remove</button>
  </form>
</td>
</tr>
<% } %>
<tr><td colspan="4" align="right"><strong>Total</strong></td><td colspan="2"><strong><%= total %></strong></td></tr>
</table>

<h3>Add to cart (test)</h3>
<form method="post" action="<%= request.getContextPath() %>/cart/add">
  <input type="number" name="id" placeholder="product id" required/>
  <input type="number" name="qty" value="1" min="1"/>
  <button type="submit">Add</button>
</form>
<p>Quick GET: <a href="<%= request.getContextPath() %>/cart/add?id=101&qty=2">Add #101 x2</a></p>
</body></html>
