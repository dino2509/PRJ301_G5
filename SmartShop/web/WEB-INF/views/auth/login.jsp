<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<h2>Login</h2>
<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>
<form method="post">
  <label>Username <input name="username" required/></label><br/>
  <label>Password <input name="password" type="password" required/></label><br/>
  <button type="submit">Login</button>
</form>
<p>
  <a href="${pageContext.request.contextPath}/register">Register</a> |
  <a href="${pageContext.request.contextPath}/forgot">Forgot password?</a>
</p>
