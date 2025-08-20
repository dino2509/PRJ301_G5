<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Products</h2>
<c:if test="${empty products}">No products found.</c:if>
<div>
  <c:forEach var="p" items="${products}">
    <div style="border:1px solid #ccc;padding:8px;margin:8px 0;">
      <h3>${p.name}</h3>
      <p>${p.price}</p>
      <a href="${pageContext.request.contextPath}/cart/add?pid=${p.id}&qty=1">Add to cart</a>
      <a href="${pageContext.request.contextPath}/product/review?productId=${p.id}">Rate</a>
    </div>
  </c:forEach>
</div>
