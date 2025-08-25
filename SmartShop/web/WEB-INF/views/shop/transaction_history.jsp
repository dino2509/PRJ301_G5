<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Lịch sử giao dịch</h2>
<c:if test="${not empty txError}"><p style="color:#a00">${txError}</p></c:if>

<h3>Ví (nạp/rút/chi ví)</h3>
<table style="width:100%;border-collapse:collapse">
  <thead><tr><th>ID</th><th>Số tiền</th><th>Loại</th><th>Trạng thái</th><th>Ref</th><th>Ghi chú</th><th>Thời gian</th></tr></thead>
  <tbody>
  <c:forEach var="r" items="${walletTx}">
    <tr>
      <td>${r.id}</td>
      <td><fmt:formatNumber value="${r.amount}" type="currency"/></td>
      <td>${r.type}</td>
      <td>${r.status}</td>
      <td>${r.ref_id}</td>
      <td>${r.note}</td>
      <td><fmt:formatDate value="${r.created_at}" pattern="HH:mm dd/MM/yyyy"/></td>
    </tr>
  </c:forEach>
  <c:if test="${empty walletTx}"><tr><td colspan="7">Chưa có giao dịch ví.</td></tr></c:if>
  </tbody>
</table>

<h3 style="margin-top:20px">Mua hàng</h3>
<table style="width:100%;border-collapse:collapse">
  <thead><tr><th>Mã</th><th>Số tiền</th><th>Phương thức</th><th>Loại</th><th>Trạng thái</th><th>Thời gian</th></tr></thead>
  <tbody>
  <c:forEach var="r" items="${orderTx}">
    <tr>
      <td>${r.code}</td>
      <td><fmt:formatNumber value="${r.amount}" type="currency"/></td>
      <td>${r.method}</td>
      <td>${r.type}</td>
      <td>${r.status}</td>
      <td><fmt:formatDate value="${r.created_at}" pattern="HH:mm dd/MM/yyyy"/></td>
    </tr>
  </c:forEach>
  <c:if test="${empty orderTx}"><tr><td colspan="6">Chưa có giao dịch mua hàng.</td></tr></c:if>
  </tbody>
</table>

<p style="margin-top:16px">
  <a href="${pageContext.request.contextPath}/wallet">Về ví</a> |
  <a href="${pageContext.request.contextPath}/cart">Về giỏ hàng</a>
</p>
