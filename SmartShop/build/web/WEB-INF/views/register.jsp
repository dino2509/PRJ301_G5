<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<h2>Đăng ký</h2>
<p style="color:red">${error}</p>
<form method="post" action="register">
  <input name="username" placeholder="username" required />
  <input name="fullName" placeholder="Họ tên"/>
  <input name="email" type="email" placeholder="Email" required />
  <input name="phone" placeholder="Số điện thoại"/>
  <input name="password" type="password" placeholder="Mật khẩu" required minlength="6"/>
  <input name="confirm"  type="password" placeholder="Xác nhận mật khẩu" required minlength="6"/>
  <button>Tạo tài khoản</button>
</form>
<%@ include file="footer.jspf" %>
