<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="header.jspf" %>
<h2>Tài khoản</h2>
<p style="color:green">${msg}</p>

<h3>Cập nhật thông tin</h3>
<form method="post" action="profile/update">
  <input name="fullName" value="${sessionScope.auth.fullName}" placeholder="Họ tên"/>
  <input name="email" value="${sessionScope.auth.email}" type="email" placeholder="Email"/>
  <input name="phone" value="${sessionScope.auth.phone}" placeholder="Số điện thoại"/>
  <button>Lưu</button>
</form>

<h3>Đổi mật khẩu</h3>
<form method="post" action="change-password">
  <input name="oldPassword" type="password" placeholder="Mật khẩu cũ" required/>
  <input name="newPassword" type="password" placeholder="Mật khẩu mới" minlength="6" required/>
  <input name="confirm" type="password" placeholder="Xác nhận mật khẩu" minlength="6" required/>
  <button>Đổi mật khẩu</button>
</form>
<%@ include file="footer.jspf" %>
