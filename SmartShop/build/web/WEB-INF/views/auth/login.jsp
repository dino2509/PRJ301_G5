<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<%@ include file="/WEB-INF/views/_includes/canvas_open.jspf" %>
<h2>Login</h2>
<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>
<form method="post">
  <label>Username <input name="username" type="text" required/></label><br/>
  <label>Password <input name="password" type="password" required/></label><br/>
  <button type="submit">Login</button>
</form>
<p>
  <a href="${pageContext.request.contextPath}/register">Register</a> |
  <a href="${pageContext.request.contextPath}/forgot" style="text-align: right">Forgot password?</a>
</p>
<%@ include file="/WEB-INF/views/_includes/canvas_close.jspf" %>