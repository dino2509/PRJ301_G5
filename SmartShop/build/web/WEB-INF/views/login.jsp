<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<h2>Đăng nhập</h2>
<p style="color:red">${error}</p>
<form method="post" action="login">
  <input name="username" value="${param.username}" placeholder="username" required />
  <input name="password" type="password" placeholder="password" required />
  <button>Đăng nhập</button>
</form>
<p><a href="register">Đăng ký</a> • <a href="forgot">Quên mật khẩu</a></p>
<%@ include file="footer.jspf" %>
