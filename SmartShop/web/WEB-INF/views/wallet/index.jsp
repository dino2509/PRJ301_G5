<!-- src/main/webapp/WEB-INF/views/wallet/index.jsp -->
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Ví nội bộ</h2>

<c:if test="${not empty error}">
  <div style="color:#b91c1c;border:1px solid #b91c1c;padding:8px;margin:8px 0">${error}</div>
</c:if>
<c:if test="${not empty success}">
  <div style="color:green;border:1px solid #16a34a;padding:8px;margin:8px 0">${success}</div>
</c:if>

<p>Số dư: <strong><fmt:formatNumber value="${balance}" type="currency"/></strong></p>

<form method="post" action="${pageContext.request.contextPath}/wallet" style="margin-top:12px">
  <input type="hidden" name="action" value="topup"/>
  <label>Số tiền nạp
    <input type="number" name="amount" min="1000" step="1000"/>
  </label>
  <button type="submit">Nạp tiền</button>
</form>
