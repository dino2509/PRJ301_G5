<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<div class="container">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <h2>Tài khoản của tôi</h2>
            <c:if test="${sessionScope.isAdmin}">
          <a href="${pageContext.request.contextPath}/admin/stats">Admin</a>
        </c:if>
  </div>

  <c:if test="${not empty msg}">
    <div class="alert alert-success">${msg}</div>
  </c:if>
  <c:if test="${not empty error}">
    <div class="alert alert-danger">${error}</div>
  </c:if>

  <form method="post" action="${pageContext.request.contextPath}/account">
    <div class="mb-2">
      <label>Username</label>
      <input type="text" class="form-control" value="${user.username}" disabled />
    </div>
    <div class="mb-2">
      <label>Họ tên</label>
      <input type="text" name="full_name" class="form-control" value="${user.fullName}" />
    </div>
    <div class="mb-2">
      <label>Email</label>
      <input type="email" name="email" class="form-control" value="${user.email}" />
    </div>
    <div class="mb-3">
      <label>Phone</label>
      <input type="text" name="phone" class="form-control" value="${user.phone}" />
    </div>
    
    <button type="submit" class="btn btn-primary">Lưu thay đổi</button>
    
    <a href="${pageContext.request.contextPath}/account/password" class="btn btn-link">Đổi mật khẩu</a>
  </form>
  <a href="${pageContext.request.contextPath}/logout">Logout</a>
</div>
