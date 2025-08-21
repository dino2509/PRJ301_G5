

(function(){
  function tick(box){
    var iso = box.getAttribute('data-end');
    if(!iso) return;
    var end = new Date(iso).getTime();
    function update(){
      var now = Date.now();
      var diff = Math.max(0, Math.floor((end - now)/1000));
      var d = Math.floor(diff/86400); var r = diff%86400;
      var h = Math.floor(r/3600); r%=3600;
      var m = Math.floor(r/60); var s = r%60;
      var t = (d<10?'0':'')+d+' ngày '
            + (h<10?'0':'')+h+':' + (m<10?'0':'')+m+':' + (s<10?'0':'')+s;
      var span = box.querySelector('.remain-text');
      if(span) span.textContent = diff===0 ? 'Hết hạn' : t;
      if(diff===0) clearInterval(timer);
    }
    update();
    var timer = setInterval(update, 1000);
  }
  document.addEventListener('DOMContentLoaded', function(){
    document.querySelectorAll('.price-box').forEach(tick);
  });
})();
