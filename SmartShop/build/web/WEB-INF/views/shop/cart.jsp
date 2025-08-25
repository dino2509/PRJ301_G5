<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="vi_VN" scope="session"/>
<%@ include file="/WEB-INF/views/_includes/header.jspf" %>
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
  .field > input, .field > textarea{padding:8px}
  .danger{color:#a00}
  .btn{padding:8px 12px;cursor:pointer}
  .btn.primary{background:#1976d2;color:#fff;border:none}
  .btn.light{background:#f1f5f9;border:1px solid #cbd5e1}
  .locked{background:#f8fafc}
  .note{margin:8px 0;padding:8px;border:1px dashed #cbd5e1;background:#f8fafc}
</style>
</head>
<body>

<h2>Giỏ hàng</h2>

<c:if test="${not empty cartSuccess}"><p style="color:#0a0">${cartSuccess}</p></c:if>
<c:if test="${not empty cartError}"><p style="color:#a00">${cartError}</p></c:if>

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
        <th></th>
        <th>Sản phẩm</th>
        <th style="width:200px">Đơn giá</th>
        <th style="width:160px">Số lượng</th>
        <th>Thành tiền</th>
        <th style="width:80px"></th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="it" items="${cartItems}">
        <tr class="row-item" data-pid="${it.product.id}" data-price="${it.product.price}">
          <td style="text-align:center"><input type="checkbox" class="chkOne"></td>
          <td><img src="${it.product.imageUrl}" alt="${it.product.name}" style="width:64px;height:64px;object-fit:cover;border:1px solid #ccc"></td>
          <td><strong>${it.product.name}</strong></td>
          <td class="unit-price"><fmt:formatNumber value="${it.product.price}" type="currency"/></td>
          <td>
            <div class="qty-box">
              <button class="btn light qty-minus" type="button">−</button>
              <input class="qty-input" type="number" min="1" step="1" value="${it.qty}" style="width:30px">
              <button class="btn light qty-plus" type="button">+</button>
            </div>
          </td>
          <td class="line-total"></td>
          <td><button type="button" class="btn danger btn-remove">Xoá</button></td>
        </tr>
      </c:forEach>
    </tbody>
    <tfoot>
      <tr>
        <td colspan="4"><strong>Tạm tính các mục đã chọn:</strong></td>
        <td class="right"><strong id="subtotal">0</strong></td>
        <td></td>
      </tr>
    </tfoot>
  </table>

  <div style="margin:12px 0">
    <button id="btnCheckout" class="btn primary" type="button">Thanh toán</button>
  </div>

  <!-- Khu thông tin giao hàng + PTTT -->
  <div id="checkoutPane" class="<c:if test='${empty pendingCheckoutId}'>hidden</c:if>">
    <h3>Thông tin giao hàng</h3>

    <label class="field">
      <input type="checkbox" id="useAcc"> Dùng thông tin trong tài khoản
    </label>

    <label class="field">Họ và tên
      <input style="width:300px" type="text" id="fullName" name="fullName" data-acc="${accFullName!=null?accFullName:''}">
    </label>
    <label class="field">Số điện thoại
      <input style="width:300px" type="tel" id="phone" name="phone" data-acc="${accPhone!=null?accPhone:''}">
    </label>
    <label class="field">Email
      <input style="width:300px" type="email" id="email" name="email" data-acc="${accEmail!=null?accEmail:''}">
    </label>
    <label class="field">Địa chỉ
      <textarea style="width:300px" id="address" name="address" data-acc="${accAddress!=null?accAddress:''}"></textarea>
    </label>

    <h3>Phương thức thanh toán</h3>
    <label class="field"><input type="radio" name="pm" value="COD" checked> COD</label>
    <label class="field">
      <input type="radio" name="pm" value="WALLET"> Ví
      <span class="muted">| Số dư: <strong><fmt:formatNumber value="${walletBalance}" type="currency"/></strong></span>
      <a class="btn light" href="${pageContext.request.contextPath}/wallet" style="margin-left:8px">Nạp</a>
      <a class="btn light" href="${pageContext.request.contextPath}/transaction_history" style="margin-left:8px">Lịch sử ví</a>
    </label>

    <div id="walletWarn" class="note danger hidden">
      Số dư ví không đủ. Thiếu <span id="needAmt"></span>. Vui lòng nạp thêm hoặc chọn COD.
    </div>

    <div class="right" style="margin-top:8px">
      <strong>Tổng thanh toán: <span id="grandTotal">0</span></strong>
    </div>

    <input type="hidden" name="selectedIds" id="selectedIds">

    <div id="placeArea">
      <p class="note">Bấm “Đặt hàng” sẽ gửi mã xác nhận tới email của bạn.</p>
      <button id="btnPlace" class="btn primary" type="submit" formaction="${pageContext.request.contextPath}/cart" onclick="setAction('place_order')">Đặt hàng</button>
    </div>

    <c:if test="${not empty pendingCheckoutId}">
      <div id="confirmArea" class="note">
        <p>Đang chờ xác nhận. Tổng: <strong>${pendingTotalFmt}</strong>. Hết hạn: <code>${pendingExpiresFmt}</code>.</p>
        <input type="hidden" name="reqId" value="${pendingCheckoutId}">
        <label>Nhập mã xác nhận: <input type="text" name="code" maxlength="16"></label>
        <button class="btn primary" type="submit" formaction="${pageContext.request.contextPath}/cart" onclick="setAction('confirm_order')">Xác nhận</button>
        <button class="btn light" type="submit" formaction="${pageContext.request.contextPath}/cart" onclick="setAction('cancel_order')">Huỷ</button>
      </div>
    </c:if>
  </div>
</form>

  </c:otherwise>
</c:choose>

<script>
(function(){
  const fmt = new Intl.NumberFormat('vi-VN', { style:'currency', currency:'VND' });
  const HAS_PENDING = ${empty pendingCheckoutId ? "false" : "true"};
  const WALLET_BAL = parseFloat('${walletBalance != null ? walletBalance : 0}');
  const $ = s => document.querySelector(s);
  const $$ = s => Array.from(document.querySelectorAll(s));

  window.setAction = function(act){
    const frm = document.getElementById('cartForm');
    frm.querySelector('input[name="action"]').value = act;
  };

  function lineSubtotal(tr){
    const price = parseFloat(tr.dataset.price);
    const qty = parseInt(tr.querySelector('.qty-input').value || '1', 10);
    const val = price * qty;
    tr.querySelector('.line-total').textContent = fmt.format(val);
    return {val};
  }

  function checkWallet(sum){
    const useWallet = document.querySelector('input[name="pm"][value="WALLET"]').checked;
    const warn = $('#walletWarn');
    const need = $('#needAmt');
    const btnPlace = $('#btnPlace');
    if (useWallet){
      if (sum > WALLET_BAL + 0.0001){
        warn.classList.remove('hidden');
        need.textContent = fmt.format(sum - WALLET_BAL);
        btnPlace.disabled = true;
        btnPlace.title = 'Số dư không đủ';
      } else {
        warn.classList.add('hidden');
        btnPlace.disabled = false;
        btnPlace.title = '';
      }
    } else {
      warn.classList.add('hidden');
      btnPlace.disabled = false;
      btnPlace.title = '';
    }
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
    $('#btnCheckout').disabled = ids.length === 0 && !HAS_PENDING;
    if (!HAS_PENDING && ids.length === 0) $('#checkoutPane').classList.add('hidden');
    if (HAS_PENDING) $('#checkoutPane').classList.remove('hidden');

    checkWallet(sum);
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
    fetch(form.action, {method:'POST', body:new FormData(form)}).then(()=>{ refreshAll(); form.remove(); });
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
      const name = tr.querySelector('td:nth-child(3) strong')?.textContent || 'sản phẩm';
      if (confirm('Xoá "'+name+'"?')) {
        const form = document.createElement('form');
        form.method = 'post'; form.action='${pageContext.request.contextPath}/cart';
        form.innerHTML = '<input name="action" value="remove"><input name="id" value="'+pid+'">';
        document.body.appendChild(form); form.submit();
      }
    });
  });

  // show pane when click "Thanh toán"
  $('#btnCheckout')?.addEventListener('click', ()=>{
    const has = $$('.chkOne').some(c=>c.checked);
    if (!has) { alert('Hãy chọn ít nhất 1 sản phẩm.'); return; }
    $('#checkoutPane').classList.remove('hidden');
    autoApplyAcc();
    window.scrollTo({top: document.body.scrollHeight, behavior:'smooth'});
    refreshAll(); // tính lại và kiểm tra ví ngay khi mở pane
  });

  // payment method change -> recheck wallet
  $$('input[name="pm"]').forEach(r => r.addEventListener('change', refreshAll));

  // auto-fill account info
  const fields = ['fullName','phone','email','address'].map(id=>$('#'+id));
  function getAcc(el){ return el?.dataset?.acc ?? el?.getAttribute('data-acc') ?? ''; }
  function setRO(el, ro){ if (!el) return; el.readOnly = ro; if (ro) el.classList.add('locked'); else el.classList.remove('locked'); }
  function lockFields(lock){
    fields.forEach(el=>{
      if (!el) return;
      if (lock){ el.value = getAcc(el); setRO(el, true); }
      else { setRO(el, false); }
    });
  }
  function autoApplyAcc(){
    const hasAcc = fields.some(f=>getAcc(f));
    if (hasAcc){ const cb = $('#useAcc'); cb.checked = true; lockFields(true); }
  }
  $('#useAcc')?.addEventListener('change', e=>lockFields(e.target.checked));
  autoApplyAcc(); // áp dụng ngay khi load
})();
</script>

</body>
</html>
