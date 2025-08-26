<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<%@ include file="/WEB-INF/views/_includes/canvas_open.jspf" %>
<h2>Change password</h2>
<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>
<c:if test="${not empty msg}"><div style="color:green">${msg}</div></c:if>
<form method="post">
  <label>Old password <input name="old_password" type="password" required/></label><br/>
  <label>New password <input name="new_password" type="password" required/></label><br/>
  <button type="submit">Change</button>
</form>
<%@ include file="/WEB-INF/views/_includes/canvas_open.jspf" %>