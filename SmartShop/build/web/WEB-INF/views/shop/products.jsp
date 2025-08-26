<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Products</h2>

<c:if test="${empty products}">
  <p>No products found.</p>
</c:if>

<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:16px;">
  <c:forEach var="p" items="${products}">
    <div style="border:1px solid #ddd;border-radius:8px;padding:12px;">
      <div style="aspect-ratio:1.2/1;background:#f7f7f7;border-radius:6px;display:flex;align-items:center;justify-content:center;margin-bottom:8px;">
        <img src="<c:out value='${p.imageUrl}'/>" alt="<c:out value='${p.name}'/>" style="max-width:100%;max-height:100%;object-fit:contain;">
      </div>
      <h3 style="margin:6px 0 10px 0;font-size:1.05rem;"><c:out value="${p.name}"/></h3>

      <jsp:include page="/WEB-INF/views/_includes/price.jspf">
        <jsp:param name="price" value="${p.price}" />
        <jsp:param name="sale" value="${p.sale}" />
        <jsp:param name="salePrice" value="${p.salePrice}" />
      </jsp:include>

      <div style="margin-top:10px;display:flex;gap:8px;">
        <a href="${pageContext.request.contextPath}/product?id=${p.id}">View</a>
        <a href="${pageContext.request.contextPath}/cart/add?pid=${p.id}&qty=1">Add to cart</a>
      </div>
    </div>
  </c:forEach>
</div>
