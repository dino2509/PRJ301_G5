<!-- src/main/webapp/WEB-INF/views/wallet/index.jsp -->
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<!DOCTYPE html>
<html lang="vi"><head>
<meta charset="UTF-8"><title>Ví</title>
<style>
  .msg-ok{color:#0a0}
  .msg-err{color:#a00}
  .card{border:1px solid #e5e7eb;padding:12px;border-radius:8px;margin:8px 0}
  .btn{padding:8px 12px;border:1px solid #cbd5e1;background:#f8fafc;cursor:pointer}
  .btn.primary{background:#1976d2;color:#fff;border:none}
  .field{display:block;margin:8px 0}
  .input{padding:8px;width:240px}
</style>
</head><body>

<h2>Ví của tôi</h2>
<p>Số dư: <strong><fmt:formatNumber value="${balance}" type="currency"/></strong></p>

<c:if test="${not empty walletSuccess}"><p class="msg-ok">${walletSuccess}</p></c:if>
<c:if test="${not empty walletError}"><p class="msg-err">${walletError}</p></c:if>

<div class="card">
  <h3>Nạp tiền</h3>
  <form method="post" action="${pageContext.request.contextPath}/wallet" style="margin-bottom:8px">
    <input type="hidden" name="action" value="init_topup">
    <label class="field">Số tiền
      <input class="input" type="number" name="amount" min="10000" step="10000" required>
    </label>
    <button class="btn primary" type="submit">Gửi mã xác nhận tới admin</button>
  </form>

  <c:if test="${not empty pendingTopupId}">
    <div class="card" style="background:#f8fafc">
      <p>Đang chờ xác nhận. Số tiền: <strong><fmt:formatNumber value="${pendingTopupAmount}" type="currency"/></strong></p>
      <p>Hết hạn: <code>${pendingTopupExpiresFmt}</code></p>
      <form method="post" action="${pageContext.request.contextPath}/wallet">
        <input type="hidden" name="action" value="confirm_topup">
        <input type="hidden" name="reqId" value="${pendingTopupId}">
        <label class="field">Nhập mã xác nhận từ admin:
          <input class="input" type="text" name="code" maxlength="16" required>
        </label>
        <button class="btn primary" type="submit">Xác nhận nạp</button>
      </form>
    </div>
  </c:if>
</div>

<p><a class="btn" href="${pageContext.request.contextPath}/transaction_history">Xem lịch sử giao dịch</a></p>

</body></html>