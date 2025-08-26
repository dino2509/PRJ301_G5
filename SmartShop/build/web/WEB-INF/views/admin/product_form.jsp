<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>

<c:set var="isEdit" value="${p != null && p.id > 0}"/>

<nav>
  <a href="${pageContext.request.contextPath}/admin/stats">Stats</a> |
  <strong>Products</strong> |
  <a href="${pageContext.request.contextPath}/admin/users">Users</a> |
  <a href="${pageContext.request.contextPath}/admin/orders">Orders</a>
</nav>

<%@ include file="/WEB-INF/views/_includes/canvas_open.jspf" %>

<h2>${isEdit ? 'Edit' : 'New'} Product</h2>

<form action="${pageContext.request.contextPath}/admin/products" method="post" id="pf">
  <input type="hidden" name="action" value="${isEdit ? 'update' : 'create'}"/>
  <c:if test="${isEdit}">
    <input type="hidden" name="id" value="${p.id}"/>
  </c:if>

  <!-- các field khác giữ nguyên dự án của bạn -->
  <label>Category ID
    <input type="number" name="category_id" value="${isEdit ? p.categoryId : ''}" required>
  </label><br/>

  <label>Name
    <input type="text" name="name" value="${isEdit ? p.name : ''}" required>
  </label><br/>

  <label>Brand
    <input type="text" name="brand" value="${isEdit ? p.brand : ''}">
  </label><br/>

  <label>Image URL
    <input type="text" name="image_url" value="${isEdit ? p.imageUrl : ''}">
    <!-- NÚT CHỌN TỪ ASSETS -->
    <button type="button" id="pick_asset" style="margin-left:6px;">Chọn từ assets</button>
  </label><br/>

  <!-- Giá gốc: hiển thị dạng 10.000.000, submit số sạch -->
  <label>Price
    <input id="price" class="money" type="text" name="price"
           value="${isEdit ? p.price : ''}" required>
  </label><br/>

  <!-- CHỈ THÊM 2 DÒNG -->
  <label>Giá sale
    <input id="sale_price" class="money" type="text" name="sale_price"
           value="${
             isEdit
               ? (sale_price != null ? sale_price : (sale == null ? p.price : ''))
               : ''
           }">
  </label><br/>
  <label>Sale (%)
    <input id="sale_percent" type="number" name="sale" step="0.01" min="0" max="100"
           value="${sale != null ? sale : 0}">
  </label><br/>
  <!-- /CHỈ THÊM 2 DÒNG -->

  <label>Stock
    <input type="number" name="stock" min="0" value="${isEdit ? p.stock : 0}" required>
  </label><br/>

  <label><input type="checkbox" name="active" ${isEdit && p.active ? 'checked' : ''}> Active</label><br/>

  <label>Description<br/>
    <textarea name="description" rows="4" cols="60">${isEdit ? p.description : ''}</textarea>
  </label><br/>

  <button type="submit">${isEdit ? 'Save' : 'Add'}</button>
  <a href="${pageContext.request.contextPath}/admin/products">Cancel</a>
</form>

<script>
(function(){
  // ====== MONEY FORMAT ======
  function digitsOnly(s){ return (s||'').toString().replace(/[^\d]/g,''); }
  function fmtMoney(s){
    var d = digitsOnly(s);
    return d.replace(/\B(?=(\d{3})+(?!\d))/g,'.');
  }
  function bindMoney(el){
    if(!el) return;
    el.value = fmtMoney(el.value);
    el.addEventListener('input', function(){
      var raw = digitsOnly(this.value);
      this.value = fmtMoney(raw);
      this.selectionStart = this.selectionEnd = this.value.length;
    });
  }

  var priceEl = document.getElementById('price');
  var salePriceEl = document.getElementById('sale_price');
  var salePctEl = document.getElementById('sale_percent');

  bindMoney(priceEl);
  bindMoney(salePriceEl);

  function intVal(el){ var s = digitsOnly(el && el.value); return s? parseInt(s,10) : 0; }
  function pctVal(el){
    var s = (el && el.value || '').toString().replace(',', '.').replace(/[^\d.]/g,'');
    return s? parseFloat(s) : 0;
  }
  function clampPct(x){ if (isNaN(x)) return 0; if (x<0) return 0; if (x>100) return 100; return x; }

  if (salePctEl) {
    salePctEl.addEventListener('input', function(){
      var price = intVal(priceEl);
      var pct = clampPct(pctVal(salePctEl));
      if (price > 0) {
        var sp = Math.max(0, Math.round(price * (1 - pct/100)));
        salePriceEl.value = fmtMoney(sp.toString());
      } else {
        salePriceEl.value = '';
      }
    });
  }

  if (salePriceEl) {
    salePriceEl.addEventListener('input', function(){
      var price = intVal(priceEl);
      var sp = intVal(salePriceEl);
      if (price > 0) {
        var pct = (1 - sp/price) * 100;
        pct = clampPct(pct);
        salePctEl.value = (Math.round(pct * 100) / 100).toString();
      } else {
        salePctEl.value = '0';
      }
    });
  }

  // ====== PICK FROM ASSETS ======
  function slug(s){
    return (s||'')
      .toString()
      .trim()
      .toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g,'')
      .replace(/[^a-z0-9]+/g,'-')
      .replace(/^-+|-+$/g,'');
  }
  var pickBtn = document.getElementById('pick_asset');
  var brandInput = document.querySelector('input[name="brand"]');
  var nameInput  = document.querySelector('input[name="name"]');
  var imageUrlInput = document.querySelector('input[name="image_url"]');

  if (pickBtn && imageUrlInput){
    pickBtn.addEventListener('click', function(){
      var folderDefault = 'smartphones';
      var brandDefault  = slug(brandInput ? brandInput.value : '');
      var fileDefault   = slug(nameInput ? nameInput.value : '');
      var folder = window.prompt('Thư mục trong assets/img/products (vd: smartphones, laptops, tablets):', folderDefault);
      if (folder === null) return;
      var brand = window.prompt('Tên thư mục brand (vd: apple, samsung, razer):', brandDefault);
      if (brand === null) return;
      var filename = window.prompt('Tên file không phần mở rộng (vd: iphone-15, razer-blade-16):', fileDefault);
      if (filename === null) return;
      var ext = window.prompt('Phần mở rộng (jpg/png/webp):', 'jpg');
      if (ext === null) return;

      folder = slug(folder);
      brand = slug(brand);
      filename = slug(filename);
      ext = (ext||'jpg').replace('.','').toLowerCase();

      var url = 'assets/img/products/' + folder + '/' + brand + '/' + filename + '.' + ext;
      imageUrlInput.value = url;
    });
  }

  // ====== SUBMIT CLEAN ======
  document.getElementById('pf').addEventListener('submit', function(){
    if (priceEl)     priceEl.value     = digitsOnly(priceEl.value);
    if (salePriceEl) salePriceEl.value = digitsOnly(salePriceEl.value);
  });
})();
</script>
<%@ include file="/WEB-INF/views/_includes/canvas_close.jspf" %>
