<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<%@ include file="/WEB-INF/views/_includes/canvas_open.jspf" %>
<div class="container">
  <h2>Quên mật khẩu</h2>

  <c:if test="${not empty error}">
    <p class="text-danger">${error}</p>
  </c:if>
  <c:if test="${not empty message}">
    <p class="text-success">${message}</p>
  </c:if>

  <!-- Bước 1: Nhập email để nhận mã -->
  <form method="post" action="${pageContext.request.contextPath}/forgot" class="mb-3">
    <input type="hidden" name="action" value="send"/>
    <div class="mb-2">
      <label>Email</label>
      <input type="email" name="email" class="form-control"
             value="<c:out value='${not empty param.email ? param.email : email}'/>" required />
    </div>
    <button type="submit" class="btn btn-primary">Gửi mã</button>
  </form>

  <!-- Bước 2: Nhập mã đã nhận để chuyển sang /reset -->
  <form method="post" action="${pageContext.request.contextPath}/forgot">
    <input type="hidden" name="action" value="verify"/>
    <div class="mb-2">
      <label>Mã xác nhận</label>
      <input name="code" maxlength="6" class="form-control" type="text" required />
    </div>
    <button type="submit" class="btn btn-primary">Xác nhận</button>
    <p class="form-text">Nhập đúng mã sẽ tự chuyển tới trang đặt lại mật khẩu. Sai mã sẽ có thông báo lỗi.</p>
  </form>
</div>
<%@ include file="/WEB-INF/views/_includes/canvas_close.jspf" %>