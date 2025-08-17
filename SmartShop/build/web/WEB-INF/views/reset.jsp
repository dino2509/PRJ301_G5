<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<h2>Đặt lại mật khẩu</h2>
<p style="color:red">${msg}</p>

<form method="post" action="reset">
  <!-- Nếu người dùng vào bằng link có token, biến token sẽ có giá trị -->
  <input type="hidden" name="token" value="${token}" />

  <!-- Hoặc người dùng chỉ cần nhập MÃ 6 SỐ từ email -->
  <label>Mã xác nhận (6 số):</label>
  <input name="code" pattern="\\d{6}" maxlength="6" placeholder="123456" />

  <label>Mật khẩu mới</label>
  <input type="password" name="newPassword" minlength="6" required />
  <label>Xác nhận mật khẩu</label>
  <input type="password" name="confirm" minlength="6" required />
  <button>Đổi mật khẩu</button>
</form>

<p>Mẹo: Nếu không có token trong link, chỉ cần nhập MÃ 6 SỐ đã nhận.</p>
<%@ include file="footer.jspf" %>
