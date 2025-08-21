<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Products</h2>
<form method="get">
    <input name="q" value="${param.q}" placeholder="tên, mô tả" hidden=""/>
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
<c:if test="${empty products}">No products found.</c:if>
    <div>
    <c:forEach var="p" items="${products}">
        <div style="border:1px solid #ccc;padding:8px;margin:8px 0; width: 1500px; display: block; justify-self:   center">
            <table border="0">
                <tbody>
                    <tr>
                        <td>
                            <img src="${p.imageUrl}" alt="${p.name}"/>
                            
                        </td>
                        <td>
                            <a style="text-decoration: blink; font-size: 20px; font-weight: bold" href="product?id=${p.id}">${p.name}</a>
                            <p>
                                <fmt:formatNumber value="${p.price}" type="currency" minFractionDigits="0" maxFractionDigits="0"/>
                            </p>
                            <a href="${pageContext.request.contextPath}/cart/add?pid=${p.id}&qty=1">Add to cart</a>
                            <a href="${pageContext.request.contextPath}/product/review?productId=${p.id}">Rate</a>
                        </td>
                    </tr>
                </tbody>
            </table>



        </div>
    </c:forEach>
</div>
