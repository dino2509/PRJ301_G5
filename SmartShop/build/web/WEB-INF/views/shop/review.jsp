<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<h2>Rate product</h2>
<form method="post">
  <input type="hidden" name="productId" value="${pid}"/>
  <label>Rating
    <select name="rating">
      <option>5</option><option>4</option><option>3</option><option>2</option><option>1</option>
    </select>
  </label><br/>
  <label>Comment<br/><textarea name="comment" rows="4" cols="50"></textarea></label><br/>
  <button type="submit">Submit</button>
</form>
