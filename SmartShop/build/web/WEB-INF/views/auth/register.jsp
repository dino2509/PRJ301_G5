<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<%@ include file="/WEB-INF/views/_includes/canvas_open.jspf" %>
<h2>Register</h2>
<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>
<form method="post">
  <label>Username <input name="username" type="text" required/></label><br/>
  <label>Password <input name="password" type="password" required/></label><br/>
  <label>Email <input name="email" type="text"/></label><br/>
  <label>Full name <input name="full_name" type="text"/></label><br/>
  <label>Phone <input name="phone" type="text"/></label><br/>
  <label>Address <input name="address" type="text"/></label><br/>
  <button type="submit">Create account</button>
</form>
<p><a href="${pageContext.request.contextPath}/login">Back to login</a></p>
<%@ include file="/WEB-INF/views/_includes/canvas_close.jspf" %>