<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
<h2>Admin Dashboard</h2>
<ul>
  <li>Products: <strong>${productsCount}</strong></li>
  <li>Users: <strong>${usersCount}</strong></li>
  <li>Orders: <strong>${ordersCount}</strong></li>
  <li>Pending wallet topups: <strong>${pendingTopups}</strong> — <a href="${pageContext.request.contextPath}/admin/wallet-topups">review</a></li>
</ul>
<nav>
  <a href="${pageContext.request.contextPath}/admin/wallet-topups">Wallet topups</a> |
  <a href="${pageContext.request.contextPath}/admin/stats">Stats</a>
</nav>
