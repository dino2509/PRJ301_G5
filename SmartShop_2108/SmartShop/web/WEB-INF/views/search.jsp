<%@page contentType="text/html; charset=UTF-8" %>
<%@taglib prefix="c" uri="http://jakarta.apache.org/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/views/header.jsp"/>
<h2>Kết quả tìm kiếm</h2>

<form action="${pageContext.request.contextPath}/search" method="get" style="margin:8px 0">
  <input type="text" name="q" value="${fn:escapeXml(param.q)}" placeholder="Tìm sản phẩm..." />
  <input type="number" name="min" placeholder="Giá từ" value="${param.min}"/>
  <input type="number" name="max" placeholder="Giá đến" value="${param.max}"/>
  <select name="sort">
    <option value="newest" ${param.sort=='newest'?'selected':''}>Mới nhất</option>
    <option value="price_asc" ${param.sort=='price_asc'?'selected':''}>Giá tăng</option>
    <option value="price_desc" ${param.sort=='price_desc'?'selected':''}>Giá giảm</option>
    <option value="sold" ${param.sort=='sold'?'selected':''}>Bán chạy</option>
  </select>
  <button type="submit">Lọc</button>
</form>

<c:if test="${empty items}">
  <p>Không tìm thấy sản phẩm.</p>
</c:if>
<div class="grid">
  <c:forEach var="p" items="${items}">
    <div class="card">
      <a href="${pageContext.request.contextPath}/product?id=${p.id}">
        <img src="<c:url value='/${p.imageUrl}'/>" alt="${p.name}" onerror="this.src='<c:url value='/assets/img/noimg.png'/>'">
        <div class="name">${p.name}</div>
        <div class="price"><fmt:formatNumber value="${p.price}" type="currency"/></div>
      </a>
    </div>
  </c:forEach>
</div>

<c:if test="${pages>1}">
  <div class="paging">
    <c:forEach var="i" begin="1" end="${pages}">
      <a href="?q=${fn:escapeXml(param.q)}&sort=${param.sort}&page=${i}" class="${i==page?'active':''}">${i}</a>
    </c:forEach>
  </div>
</c:if>

<jsp:include page="/WEB-INF/views/footer.jsp"/>
