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
  .card{border:1px solid #e5e7eb;padding:12px;border-radius:8px;margin:8px 0;background:#fff}
  .btn{padding:10px 14px;border:1px solid #cbd5e1;background:#f8fafc;cursor:pointer;border-radius:10px}
  .btn.primary{background:#1f6feb;color:#fff;border:none}
  .field{display:block;margin:8px 0}
  .input{padding:10px;width:260px;border-radius:10px;border:1px solid #e5e7eb}
</style>
</head><body>

<h2>Ví của tôi</h2>
<p>Số dư:
  <strong>
    <fmt:formatNumber value="${balance}" type="number" maxFractionDigits="0" groupingUsed="true"/> ₫
  </strong>
</p>

<c:if test="${not empty walletSuccess}"><p class="msg-ok">${walletSuccess}</p></c:if>
<c:if test="${not empty walletError}"><p class="msg-err">${walletError}</p></c:if>

<div class="card">
  <h3>Nạp tiền</h3>
  <form id="topupForm" method="post" action="${pageContext.request.contextPath}/wallet" style="margin-bottom:8px">
    <input type="hidden" name="action" value="init_topup">
    <!-- input hiển thị có dấu chấm; input ẩn gửi số thuần -->
    <label class="field">Số tiền
      <input class="input" type="text" inputmode="numeric" autocomplete="off"
             id="amountVnd" name="amount_view" placeholder="vd: 1.000.000">
      <input type="hidden" id="amountRaw" name="amount">
    </label>
    <button class="btn primary" type="submit">Gửi mã xác nhận tới admin</button>
  </form>

  <c:if test="${not empty pendingTopupId}">
    <div class="card" style="background:#f8fafc">
      <p>Đang chờ xác nhận. Số tiền:
        <strong>
          <fmt:formatNumber value="${pendingTopupAmount}" type="number" maxFractionDigits="0" groupingUsed="true"/> ₫
        </strong>
      </p>
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

<script>
(function(){
  const view = document.getElementById('amountVnd');
  const raw  = document.getElementById('amountRaw');
  const form = document.getElementById('topupForm');

  function onlyDigits(s){ return (s||'').replace(/\D/g,''); }
  function formatDots(s){
    if(!s) return '';
    return s.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  }

  if(view && raw){
    view.addEventListener('input', function(){
      const digits = onlyDigits(view.value);
      view.value = formatDots(digits);
      raw.value  = digits;
    });
    view.addEventListener('blur', function(){
      const digits = onlyDigits(view.value);
      view.value = formatDots(digits);
      raw.value  = digits;
    });
    form && form.addEventListener('submit', function(){
      // đảm bảo gửi số thuần
      raw.value = onlyDigits(view.value);
    });
  }
})();
</script>

</body></html>
