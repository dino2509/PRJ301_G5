<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<div class="product-detail">
  <img src="${p.imageUrl}" alt="${p.name}"/>
  <div>
    <h2>${p.name}</h2>
    <div>Giá: ${p.price}</div>
    <div>Màu: ${p.color} | Hãng: ${p.brand}</div>
    <p>${p.description}</p>
    <form action="cart" method="post">
      <input type="hidden" name="action" value="add"/>
      <input type="hidden" name="id" value="${p.id}"/>
      <button>Thêm vào giỏ</button>
    </form>
  </div>
</div>
<h3>Gợi ý tương tự</h3>
<div class="grid">
<c:forEach var="x" items="${related}">
  <div class="card">
    <img src="${x.imageUrl}" alt="${x.name}"/>
    <a href="product?id=${x.id}">${x.name}</a>
    <div>${x.price}</div>
  </div>
</c:forEach>
</div>
<%@ include file="footer.jspf" %>
