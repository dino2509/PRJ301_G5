<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>Giỏ hàng</title>
<style>
  table{width:100%;border-collapse:collapse}
  th,td{padding:8px;border-bottom:1px solid #eee;vertical-align:middle}
  .qty-box{display:inline-flex;align-items:center}
  .qty-box button{width:28px;height:28px}
  .qty-input{width:56px;text-align:center;margin:0 6px}
  .right{text-align:right}
  .hidden{display:none}
  .muted{color:#777}
  .row-item.selected{background:#f7fbff}
  .field{display:block;margin:8px 0}
  .field > input, .field > textarea, .field > select{width:100%;padding:8px}
  .danger{color:#a00}
  .btn{padding:8px 12px;cursor:pointer}
  .btn.primary{background:#1976d2;color:#fff;border:none}
  .btn.light{background:#f1f5f9;border:1px solid #cbd5e1}
</style>
</head>
<body>

<h2>Giỏ hàng</h2>

<c:choose>
  <c:when test="${empty cartItems}">
    <p class="muted">Giỏ hàng trống.</p>
  </c:when>
  <c:otherwise>

<form id="cartForm" method="post" action="${pageContext.request.contextPath}/cart">
  <input type="hidden" name="action" value="update">
  <table>
    <thead>
      <tr>
        <th style="width:40px"><input type="checkbox" id="chkAll"></th>
        <th>Sản phẩm</th>
        <th class="right">Đơn giá</th>
        <th style="width:160px">Số lượng</th>
        <th class="right">Thành tiền</th>
        <th style="width:80px"></th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="it" items="${cartItems}">
        <tr class="row-item" data-pid="${it.product.id}" data-price="${it.product.price}">
          <td style="text-align:center"><input type="checkbox" class="chkOne"></td>
          <td><strong>${it.product.name}</strong></td>
          <td class="right unit-price"><fmt:formatNumber value="${it.product.price}" type="currency"/></td>
          <td>
            <div class="qty-box">
              <button class="btn light qty-minus" type="button">−</button>
              <input class="qty-input" type="number" min="1" step="1" value="${it.qty}">
              <button class="btn light qty-plus" type="button">+</button>
            </div>
          </td>
          <td class="right line-total"></td>
          <td class="right"><button type="button" class="btn danger btn-remove">Xoá</button></td>
        </tr>
      </c:forEach>
    </tbody>
    <tfoot>
      <tr>
        <td colspan="4" class="right"><strong>Tạm tính các mục đã chọn:</strong></td>
        <td class="right"><strong id="subtotal">0</strong></td>
        <td></td>
      </tr>
    </tfoot>
  </table>

  <div style="margin:12px 0">
    <button id="btnCheckout" class="btn primary" type="button">Thanh toán</button>
  </div>

  <!-- Khu thông tin giao hàng + PTTT -->
  <div id="checkoutPane" class="hidden">
    <h3>Thông tin giao hàng</h3>

    <label class="field">
      <input type="checkbox" id="useAcc"> Dùng thông tin trong tài khoản
    </label>

    <label class="field">Họ và tên
      <input type="text" id="fullName" name="fullName" data-acc="${accFullName!=null?accFullName:''}">
    </label>
    <label class="field">Số điện thoại
      <input type="tel" id="phone" name="phone" data-acc="${accPhone!=null?accPhone:''}">
    </label>
    <label class="field">Email
      <input type="email" id="email" name="email" data-acc="${accEmail!=null?accEmail:''}">
    </label>
    <label class="field">Địa chỉ
      <textarea id="address" name="address" data-acc="${accAddress!=null?accAddress:''}"></textarea>
    </label>

    <h3>Phương thức thanh toán</h3>
    <label class="field"><input type="radio" name="pm" value="COD" checked> COD</label>
    <label class="field"><input type="radio" name="pm" value="WALLET"> Ví</label>
    <label class="field"><input type="radio" name="pm" value="GATEWAY"> Cổng giả lập</label>

    <div class="right" style="margin-top:8px">
      <strong>Tổng thanh toán: <span id="grandTotal">0</span></strong>
    </div>

    <input type="hidden" name="selectedIds" id="selectedIds">
    <button class="btn primary" type="submit" formaction="${pageContext.request.contextPath}/checkout">Đặt hàng</button>
  </div>
</form>

  </c:otherwise>
</c:choose>

<script>
(function(){
  const fmt = new Intl.NumberFormat('vi-VN', { style:'currency', currency:'VND' });
  const $ = s => document.querySelector(s);
  const $$ = s => Array.from(document.querySelectorAll(s));

  function lineSubtotal(tr){
    const price = parseFloat(tr.dataset.price);
    const qty = parseInt(tr.querySelector('.qty-input').value || '1', 10);
    const val = price * qty;
    tr.querySelector('.line-total').textContent = fmt.format(val);
    return {val};
  }
  function refreshAll(){
    let sum = 0;
    $$('.row-item').forEach(tr=>{
      const {val} = lineSubtotal(tr);
      if (tr.querySelector('.chkOne').checked) sum += val;
      tr.classList.toggle('selected', tr.querySelector('.chkOne').checked);
    });
    $('#subtotal').textContent = fmt.format(sum);
    $('#grandTotal').textContent = fmt.format(sum);
    const ids = $$('.row-item').filter(tr=>tr.querySelector('.chkOne').checked).map(tr=>tr.dataset.pid);
    $('#selectedIds').value = ids.join(',');
    $('#btnCheckout').disabled = ids.length === 0;
  }

  // init lines
  $$('.row-item').forEach(tr=>lineSubtotal(tr));
  refreshAll();

  // qty +/- and change
  function sendUpdate(pid, qty){
    const form = document.createElement('form');
    form.method = 'post'; form.action = '${pageContext.request.contextPath}/cart';
    form.innerHTML = '<input name="action" value="update"><input name="id" value="'+pid+'"><input name="qty" value="'+qty+'">';
    document.body.appendChild(form);
    fetch(form.action, {method:'POST', body:new FormData(form)})
      .then(()=>{ refreshAll(); form.remove(); });
  }
  $$('.qty-minus').forEach(btn=>{
    btn.addEventListener('click', e=>{
      const tr = e.target.closest('.row-item');
      const input = tr.querySelector('.qty-input');
      let v = Math.max(1, parseInt(input.value||'1',10)-1);
      input.value = v; sendUpdate(tr.dataset.pid, v);
    });
  });
  $$('.qty-plus').forEach(btn=>{
    btn.addEventListener('click', e=>{
      const tr = e.target.closest('.row-item');
      const input = tr.querySelector('.qty-input');
      let v = Math.max(1, parseInt(input.value||'1',10)+1);
      input.value = v; sendUpdate(tr.dataset.pid, v);
    });
  });
  $$('.qty-input').forEach(inp=>{
    inp.addEventListener('change', e=>{
      const tr = e.target.closest('.row-item');
      let v = Math.max(1, parseInt(e.target.value||'1',10));
      e.target.value = v; sendUpdate(tr.dataset.pid, v);
    });
  });

  // select all / one
  $('#chkAll')?.addEventListener('change', e=>{
    $$('.chkOne').forEach(c=>c.checked = e.target.checked);
    refreshAll();
  });
  $$('.chkOne').forEach(c=>c.addEventListener('change', refreshAll));

  // remove confirm
  $$('.btn-remove').forEach(b=>{
    b.addEventListener('click', e=>{
      const tr = e.target.closest('.row-item');
      const pid = tr.dataset.pid;
      const name = tr.querySelector('td:nth-child(2) strong')?.textContent || 'sản phẩm';
      if (confirm('Xoá "'+name+'"?')) {
        const form = document.createElement('form');
        form.method = 'post'; form.action='${pageContext.request.contextPath}/cart';
        form.innerHTML = '<input name="action" value="remove"><input name="id" value="'+pid+'">';
        document.body.appendChild(form); form.submit();
      }
    });
  });

  // show checkout pane only if selected
  $('#btnCheckout')?.addEventListener('click', ()=>{
    const has = $$('.chkOne').some(c=>c.checked);
    if (!has) { alert('Hãy chọn ít nhất 1 sản phẩm.'); return; }
    $('#checkoutPane').classList.remove('hidden');
    window.scrollTo({top: document.body.scrollHeight, behavior:'smooth'});
  });

  // use account info: auto-điền và khóa
  const ids = ['fullName','phone','email','address'];
  const fields = ids.map(id=>$('#'+id));
  function getAcc(el){ return el?.dataset?.acc ?? ''; }
  function lockFields(lock){
    fields.forEach(el=>{
      if (!el) return;
      if (lock){
        el.value = getAcc(el);
        el.readOnly = true;
        el.classList.add('muted');
      }else{
        el.readOnly = false;
        el.classList.remove('muted');
      }
    });
  }
  $('#useAcc')?.addEventListener('change', e=>lockFields(e.target.checked));
  // nếu người dùng tick rồi mới điền -> vẫn chạy; nếu muốn auto khi có data:
  // if (fields.some(f=>getAcc(f))) { $('#useAcc').checked=true; lockFields(true); }
})();
</script>

</body>
</html>
