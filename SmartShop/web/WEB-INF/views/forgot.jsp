<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<h2>Quên mật khẩu</h2>
<p style="color:green">${msg}</p>
<c:if test="${not empty devResetLink}">
  <p><b>Dev:</b> SMTP chưa cấu hình. Dùng liên kết này để đặt lại: <a href="${devResetLink}">${devResetLink}</a></p>
</c:if>
<form method="post" action="forgot">
  <input type="email" name="email" placeholder="Nhập email đã đăng ký" required />
  <button>Gửi liên kết đặt lại</button>
</form>
<%@ include file="footer.jspf" %>
