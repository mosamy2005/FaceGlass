package com.glass.face

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.*

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var fileCb: ValueCallback<Array<Uri>>? = null
    private val FILE_REQ = 7

    private val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0b1020")

        web = WebView(this)
        setContentView(web)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = UA
            allowFileAccess = false
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val h = r.url.host ?: ""
                val inside = h.endsWith("facebook.com") || h.endsWith("fb.com") ||
                        h.endsWith("fbcdn.net") || h.endsWith("messenger.com")
                if (inside) return false
                startActivity(Intent(Intent.ACTION_VIEW, r.url))
                return true
            }

            override fun onPageFinished(v: WebView, url: String) {
                v.evaluateJavascript(GLASS_JS, null)
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams
            ): Boolean {
                fileCb?.onReceiveValue(null)
                fileCb = cb
                return try {
                    startActivityForResult(p.createIntent(), FILE_REQ); true
                } catch (e: Exception) {
                    fileCb = null; false
                }
            }
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://m.facebook.com/")
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == FILE_REQ) {
            fileCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data))
            fileCb = null
        } else super.onActivityResult(req, res, data)
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        web.saveState(out)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onPause() { super.onPause(); CookieManager.getInstance().flush() }

    // شريط Liquid Glass يتحقن فوق صفحة فيسبوك (backdrop-filter حقيقي)
    private val GLASS_JS = """
(function(){
  if(window.__glass) return; window.__glass = 1;
  var items = [
    ['/', 'M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z'],
    ['/friends/', 'M9 4.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7zM2.5 20c0-3.6 2.9-6 6.5-6s6.5 2.4 6.5 6M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.3c2 .7 3.5 2.5 3.5 5.7'],
    ['/watch/', 'M7 5h10a4 4 0 0 1 4 4v6a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a4 4 0 0 1 4-4zM10.5 9.5v5l4-2.5z'],
    ['/notifications/', 'M6 9a6 6 0 0 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9zM10 20a2 2 0 0 0 4 0'],
    ['/menu/bookmarks/', 'M4 7h16M4 12h16M4 17h16']
  ];
  var NS = 'http://www.w3.org/2000/svg';
  var nav = document.createElement('div');
  nav.id = '__glassnav';
  nav.style.cssText = 'position:fixed;left:50%;transform:translateX(-50%);bottom:14px;' +
    'width:min(400px,calc(100% - 24px));height:64px;border-radius:32px;display:flex;' +
    'align-items:center;justify-content:space-around;padding:0 6px;z-index:2147483000;' +
    'background:linear-gradient(135deg,rgba(40,55,100,.5),rgba(20,30,60,.34));' +
    'backdrop-filter:blur(24px) saturate(180%);-webkit-backdrop-filter:blur(24px) saturate(180%);' +
    'border:1px solid rgba(255,255,255,.4);' +
    'box-shadow:inset 0 1.5px 0 rgba(255,255,255,.55),0 10px 30px rgba(0,0,0,.3)';
  var btns = [];
  items.forEach(function(it){
    var b = document.createElement('div');
    b.style.cssText = 'flex:1;height:48px;margin:0 2px;border-radius:24px;display:flex;' +
      'align-items:center;justify-content:center;transition:background .25s';
    var s = document.createElementNS(NS, 'svg');
    s.setAttribute('viewBox', '0 0 24 24');
    s.style.cssText = 'width:25px;height:25px;fill:none;stroke:#fff;stroke-width:2;' +
      'stroke-linecap:round;stroke-linejoin:round';
    var p = document.createElementNS(NS, 'path');
    p.setAttribute('d', it[1]);
    s.appendChild(p); b.appendChild(s);
    b.addEventListener('click', function(){ location.href = 'https://m.facebook.com' + it[0]; });
    nav.appendChild(b); btns.push(b);
  });
  function sync(){
    if(!document.getElementById('__glassnav') && document.body){
      document.body.appendChild(nav);
      document.body.style.paddingBottom = '90px';
    }
    var path = location.pathname;
    items.forEach(function(it, i){
      var on = it[0] === '/' ? (path === '/' || path.indexOf('/home') === 0)
                             : path.indexOf(it[0].split('/')[1] ? '/' + it[0].split('/')[1] : '/x') === 0;
      btns[i].style.background = on
        ? 'linear-gradient(180deg,rgba(255,255,255,.38),rgba(255,255,255,.14))' : 'transparent';
      btns[i].style.boxShadow = on ? 'inset 0 1.5px 0 rgba(255,255,255,.6)' : 'none';
    });
  }
  sync(); setInterval(sync, 700);
})();
""".trimIndent()
}
