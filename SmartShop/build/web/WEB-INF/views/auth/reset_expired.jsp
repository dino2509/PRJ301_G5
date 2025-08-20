<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<div class="container">
  <h2>Liên kết không còn hiệu lực</h2>
  <p>Liên kết đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng.</p>
  <a href="${pageContext.request.contextPath}/forgot" class="btn btn-primary">Gửi lại liên kết</a>
</div>
