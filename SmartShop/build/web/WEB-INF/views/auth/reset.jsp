<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<%@ include file="/WEB-INF/views/_includes/canvas_open.jspf" %>
<div class="container">
  <h2>Đặt lại mật khẩu</h2>
  <p style="color:green">${msg}</p>

  <c:if test="${not empty error}">
    <p class="text-danger">${error}</p>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/reset">
    <!-- bắt buộc gửi lại cả token và email -->
    <input type="hidden" name="token" value="${token}"/>
    <input type="hidden" name="email" value="${resetEmail}"/>

    <div class="mb-2">
      <label>Email tài khoản</label>
      <input type="email" value="${resetEmail}" class="form-control" disabled />
    </div>

    <div class="mb-2">
      <label>Mật khẩu mới</label>
      <input type="password" name="new_password" class="form-control" required />
    </div>
    <div class="mb-3">
      <label>Nhập lại mật khẩu</label>
      <input type="password" name="confirm_password" class="form-control" required />
    </div>
    <button type="submit" class="btn btn-primary">Cập nhật</button>
  </form>
</div>
<%@ include file="/WEB-INF/views/_includes/canvas_close.jspf" %>