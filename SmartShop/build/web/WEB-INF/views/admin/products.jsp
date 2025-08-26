<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<nav>
  <a href="${pageContext.request.contextPath}/admin/stats">Stats</a> |
  <strong>Products</strong> |
  <a href="${pageContext.request.contextPath}/admin/users">Users</a> |
  <a href="${pageContext.request.contextPath}/admin/orders">Orders</a>
</nav>

<h2>Admin · Products</h2>
<p><a href="${pageContext.request.contextPath}/admin/products?action=new">+ New Product</a></p>

<!-- Thanh thao tác hàng loạt -->
<form id="bulkForm" action="${pageContext.request.contextPath}/admin/products" method="post" style="margin:10px 0;">
  <input type="hidden" name="action" value="bulk_pricing"/>
  <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap;padding:8px;border:1px solid #ddd;border-radius:8px;">
    <strong>Áp dụng cho mục đã chọn:</strong>

    <label style="display:flex;gap:6px;align-items:center;">
      <input type="radio" name="mode" value="minus" checked>
      Giảm giá:
      <input type="text" name="amount" id="amount" placeholder="100.000" style="width:140px;">₫
    </label>

    <span>hoặc</span>

    <label style="display:flex;gap:6px;align-items:center;">
      <input type="radio" name="mode" value="percent">
      Sale:
      <input type="number" step="0.01" min="0" max="100" name="percent" id="percent" placeholder="10" style="width:90px;">
      %
    </label>

    <button type="submit" id="applyBtn">Áp dụng</button>
  </div>
  <div id="bulkIds" style="display:none;"></div>
</form>

<table class="table" id="productTable">
  <thead>
    <tr>
      <th><input type="checkbox" id="chkAll"></th>

      <th data-key="id"><button type="button" class="sort" style="width:40px">ID <span class="arrow"></span></button></th>
      <th data-key="name"><button type="button" class="sort">Name <span class="arrow"></span></button></th>
      <th data-key="brand"><button type="button" class="sort">Brand <span class="arrow"></span></button></th>
      <th data-key="type"><button type="button" class="sort">Type <span class="arrow"></span></button></th>
      <th data-key="price"><button type="button" class="sort">Price <span class="arrow"></span></button></th>
      <th data-key="saleprice"><button type="button" class="sort">Giá sale <span class="arrow"></span></button></th>
      <th data-key="sale"><button type="button" class="sort" style="width:55px">Sale <span class="arrow"></span></button></th>
      <th data-key="stock"><button type="button" class="sort" style="width:65px">Stock <span class="arrow"></span></button></th>

      <th>Active</th>
      <th>Actions</th>
      <th></th>
    </tr>
  </thead>
  <tbody>
  <c:forEach var="p" items="${list}">
    <c:set var="sp" value="${not empty salePrices and salePrices[p.id] ne null ? salePrices[p.id] : p.price}"/>
    <c:set var="sPct" value="${not empty sales and sales[p.id] ne null ? sales[p.id] : 0}"/>
    <tr
      data-id="${p.id}"
      data-name="${p.name}"
      data-brand="${p.brand}"
      data-type="${categoryNames[p.categoryId]}"
      data-price="${p.price}"
      data-saleprice="${sp}"
      data-sale="${sPct}"
      data-stock="${p.stock}"
    >
      <td><input type="checkbox" class="rowchk" data-id="${p.id}"></td>

      <td>${p.id}</td>
      <td>${p.name}</td>
      <td>${p.brand}</td>
      <td><c:out value="${categoryNames[p.categoryId]}"/></td>
      <td><fmt:formatNumber value="${p.price}" type="currency"/></td>

      <td>
        <fmt:formatNumber value="${sp}" type="currency"/>
      </td>

      <td>
        <fmt:formatNumber value="${sPct}" maxFractionDigits="2"/>%
      </td>

      <td>${p.stock}</td>
      <td>${p.active}</td>
      <td>
        <a href="${pageContext.request.contextPath}/admin/products?action=edit&id=${p.id}">Edit</a>
      </td>
      <td style="width:30px">
          <form action="${pageContext.request.contextPath}/admin/products" method="post" style="display:inline">
          <input type="hidden" name="action" value="delete"/>
          <input type="hidden" name="id" value="${p.id}"/>
          <button style="margin-right:0px" type="submit" onclick="return confirm('Delete #${p.id}?')">❌</button>
        </form>
      </td>
    </tr>
  </c:forEach>
  </tbody>
</table>

<style>
  #productTable th button.sort{ all:unset; cursor:pointer; font-weight:600; }
  #productTable th .arrow{ margin-left:4px; visibility:hidden; }
  #productTable th.active .arrow{ visibility:visible; }
</style>

<script>
(function(){
  // chọn tất cả
  var chkAll = document.getElementById('chkAll');
  if (chkAll){
    chkAll.addEventListener('change', function(){
      document.querySelectorAll('.rowchk').forEach(function(c){ c.checked = chkAll.checked; });
    });
  }

  // định dạng tiền cho ô amount
  function digitsOnly(s){ return (s||'').toString().replace(/[^\d]/g,''); }
  function fmtMoney(s){ var d = digitsOnly(s); return d.replace(/\B(?=(\d{3})+(?!\d))/g,'.'); }
  var amount = document.getElementById('amount');
  if (amount){
    amount.addEventListener('input', function(){ this.value = fmtMoney(this.value); });
  }

  // thêm ids vào form trước khi submit
  document.getElementById('bulkForm').addEventListener('submit', function(e){
    var box = document.getElementById('bulkIds');
    box.innerHTML = '';
    var any = false;
    document.querySelectorAll('.rowchk:checked').forEach(function(c){
      any = true;
      var inp = document.createElement('input');
      inp.type = 'hidden';
      inp.name = 'ids';
      inp.value = c.getAttribute('data-id');
      box.appendChild(inp);
    });
    if (!any) { e.preventDefault(); alert('Hãy chọn ít nhất một sản phẩm.'); return false; }
    if (amount) amount.value = digitsOnly(amount.value);
  });

  // ===== Sort table client-side =====
  var current = { key: null, dir: 1 };
  var table = document.getElementById('productTable');
  var tbody = table.querySelector('tbody');

  function getVal(tr, key){
    var v = tr.dataset[key] || '';
    if (key === 'name' || key === 'brand' || key === 'type') return v.toString().toLowerCase();
    var n = parseFloat(v);
    return isNaN(n) ? 0 : n;
  }

  function applyArrows(){
    table.querySelectorAll('th[data-key]').forEach(function(th){
      th.classList.remove('active');
      var sp = th.querySelector('.arrow'); if (sp){ sp.textContent=''; }
    });
    if (!current.key) return;
    var th = table.querySelector('th[data-key="'+current.key+'"]');
    if (th){
      th.classList.add('active');
      var sp = th.querySelector('.arrow');
      if (sp){ sp.textContent = current.dir === 1 ? '▲' : '▼'; }
    }
  }

  function sortBy(key, dir){
    var rows = Array.prototype.slice.call(tbody.querySelectorAll('tr'));
    rows.sort(function(a,b){
      var va = getVal(a, key), vb = getVal(b, key);
      if (typeof va === 'string' || typeof vb === 'string'){
        return va.localeCompare(vb) * dir;
      }
      return (va - vb) * dir;
    });
    rows.forEach(function(r){ tbody.appendChild(r); });
    current.key = key; current.dir = dir;
    applyArrows();
  }

  table.querySelectorAll('th[data-key] button.sort').forEach(function(btn){
    btn.addEventListener('click', function(){
      var key = this.parentElement.getAttribute('data-key');
      var dir = (current.key === key && current.dir === 1) ? -1 : 1;
      sortBy(key, dir);
    });
  });

  // mặc định không sort; mũi tên ẩn
  applyArrows();
})();
</script>
