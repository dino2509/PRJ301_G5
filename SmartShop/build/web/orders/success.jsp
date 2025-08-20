<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Order placed</h2>
<p>Your order ID: <strong>${param.orderId}</strong></p>
<p><a href="${pageContext.request.contextPath}/">Back to Home</a></p>
