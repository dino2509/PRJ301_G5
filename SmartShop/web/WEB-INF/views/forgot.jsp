<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>

<h2>Quên mật khẩu</h2>
<p style="color:green">${msg}</p>
<c:if test="${not empty devResetLink}">
  <p><b>Dev:</b> SMTP chưa cấu hình. Link đặt lại: <a href="${devResetLink}">${devResetLink}</a></p>
</c:if>

<h3>1) Nhận mã & liên kết qua email</h3>
<form method="post" action="forgot" autocomplete="off">
  <input type="hidden" name="action" value="request"/>
  <!-- Giữ lại email đã nhập sau khi bấm "Gửi email" -->
  <input type="email" name="email" placeholder="Nhập email đã đăng ký"
         value="${param.email}" required />
  <button>Gửi email</button>
</form>
  <br>
  
<form method="post" action="forgot" autocomplete="one-time-code">
  <input type="hidden" name="action" value="verify"/>
  <!-- Dùng pattern [0-9]{6} thay vì \\d{6} -->
  <input name="code" inputmode="numeric" pattern="[0-9]{6}" maxlength="6"
         placeholder="123456" required />
  <button>Tiếp tục</button>
</form>

<%@ include file="footer.jspf" %>
