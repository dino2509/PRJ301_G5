<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>${empty p.id ? 'New' : 'Edit'} Product</h2>
<form method="post" action="${pageContext.request.contextPath}/admin/products">
  <input type="hidden" name="id" value="${p.id}"/>
  <label>Category
    <select name="category_id">
      <option value="">(none)</option>
      <c:forEach var="c" items="${categories}">
        <option value="${c.id}" <c:if test="${p.categoryId == c.id}">selected</c:if>>${c.name}</option>
      </c:forEach>
    </select>
  </label><br/>
  <label>Name <input name="name" value="${p.name}" required/></label><br/>
  <label>Brand <input name="brand" value="${p.brand}"/></label><br/>
  <label>Color <input name="color" value="${p.color}"/></label><br/>
  <label>Image URL <input name="image_url" value="${p.imageUrl}" style="width:400px"/></label><br/>
  <label>Price <input type="number" step="0.01" name="price" value="${p.price}" required/></label><br/>
  <label>Stock <input type="number" name="stock" value="${p.stock}" required/></label><br/>
  <label>Active <input type="checkbox" name="active" <c:if test="${p.active}">checked</c:if> /></label><br/>
  <label>Description<br/><textarea name="description" rows="5" cols="60">${p.description}</textarea></label><br/>
  <button type="submit">Save</button>
</form>
