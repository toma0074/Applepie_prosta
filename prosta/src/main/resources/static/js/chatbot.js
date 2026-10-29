(function () {
  var root = document.getElementById('chatbot');
  if (!root) {
    return;
  }
  var api = root.dataset.api;
  var win = document.getElementById('chatbot-window');
  var messages = document.getElementById('chatbot-messages');
  var form = document.getElementById('chatbot-form');
  var input = document.getElementById('chatbot-input');
  var greeted = false;

  function scrollBottom() {
    messages.scrollTop = messages.scrollHeight;
  }

  function addMessage(text, who) {
    var div = document.createElement('div');
    div.className = 'chatbot-msg ' + who;
    // 入力文・応答文をHTMLとして解釈せず、文字列のまま表示する。
    div.textContent = text;
    messages.appendChild(div);
    scrollBottom();
  }

  function addLinks(links) {
    if (!links || !links.length) {
      return;
    }
    var box = document.createElement('div');
    box.className = 'chatbot-links';
    links.forEach(function (l) {
      var a = document.createElement('a');
      a.href = l.url;
      a.textContent = l.label;
      box.appendChild(a);
    });
    messages.appendChild(box);
    scrollBottom();
  }

  function send(text) {
    text = text.trim();
    if (!text) {
      return;
    }
    addMessage(text, 'user');
    fetch(api, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text })
    })
      .then(function (res) {
        if (!res.ok) {
          throw new Error('status ' + res.status);
        }
        return res.json();
      })
      .then(function (data) {
        addMessage(data.text, 'bot');
        addLinks(data.links);
      })
      .catch(function () {
        addMessage('エラーが発生しました。しばらくしてからもう一度お試しください。', 'bot');
      });
  }

  function toggle(open) {
    win.hidden = !open;
    if (open) {
      if (!greeted) {
        greeted = true;
        addMessage('こんにちは！PROSTAアシスタントです。\n学習の状況や使い方を聞いてください。', 'bot');
      }
      input.focus();
    }
  }

  document.getElementById('chatbot-toggle').addEventListener('click', function () {
    toggle(win.hidden);
  });
  document.getElementById('chatbot-close').addEventListener('click', function () {
    toggle(false);
  });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    send(input.value);
    input.value = '';
  });

  root.querySelectorAll('.chatbot-quick button').forEach(function (btn) {
    btn.addEventListener('click', function () {
      send(btn.dataset.q);
    });
  });
})();
