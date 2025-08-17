<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<p>${dbStatus}</p>

<h2>Sản phẩm nổi bật</h2>
<div class="grid">
<c:forEach var="p" items="${featured}">
  <div class="card">
    <img src="${p.imageUrl}" alt="${p.name}"/>
    <a href="product?id=${p.id}">${p.name}</a>
    <div>${p.price}</div>
  </div>
</c:forEach>
</div>

<h2>Mới cập nhật</h2>
<div class="grid">
<c:forEach var="p" items="${newest}">
  <div class="card">
    <img src="${p.imageUrl}" alt="${p.name}"/>
    <a href="product?id=${p.id}">${p.name}</a>
    <div>${p.price}</div>
  </div>
</c:forEach>
</div>

<h2>Bán chạy</h2>
<div class="grid">
<c:forEach var="p" items="${bestseller}">
  <div class="card">
    <img src="${p.imageUrl}" alt="${p.name}"/>
    <a href="product?id=${p.id}">${p.name}</a>
    <div>${p.price}</div>
  </div>
</c:forEach>
</div>

<h2>Tất cả</h2>
<form method="get">
  <input name="q" value="${param.q}" placeholder="tên, mô tả"/>
  <input name="min" value="${param.min}" placeholder="giá từ"/>
  <input name="max" value="${param.max}" placeholder="giá đến"/>
  <select name="sort">
    <option value="">Mặc định</option>
    <option value="price_asc" ${param.sort=='price_asc'?'selected':''}>Giá tăng</option>
    <option value="price_desc" ${param.sort=='price_desc'?'selected':''}>Giá giảm</option>
    <option value="newest" ${param.sort=='newest'?'selected':''}>Mới nhất</option>
    <option value="rating" ${param.sort=='rating'?'selected':''}>Đánh giá cao</option>
  </select>
  <button>Lọc</button>
</form>
<div class="grid">
<c:forEach var="p" items="${products}">
  <div class="card">
    <img src="${p.imageUrl}" alt="${p.name}"/>
    <a href="product?id=${p.id}">${p.name}</a>
    <div>${p.price}</div>
  </div>
</c:forEach>
</div>
<c:set var="pages" value="${(total + size - 1) / size}"/>
<div class="pagination">
  <c:forEach var="i" begin="1" end="${pages}">
    <a href="?page=${i}&q=${param.q}&min=${param.min}&max=${param.max}&sort=${param.sort}" class="${i==page?'active':''}">${i}</a>
  </c:forEach>
</div>
<%@ include file="footer.jspf" %>
