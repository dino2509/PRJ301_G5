<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<h2>Đặt lại mật khẩu</h2>
<p style="color:red">${msg}</p>
<form method="post" action="reset">
  <label>Mật khẩu mới</label>
  <input type="password" name="newPassword" minlength="6" required />
  <label>Xác nhận mật khẩu</label>
  <input type="password" name="confirm" minlength="6" required />
  <button>Đổi mật khẩu</button>
</form>
<%@ include file="footer.jspf" %>
