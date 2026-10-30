(function () {
  "use strict";

  document.querySelectorAll("[data-password-toggle]").forEach(function (toggle) {
    toggle.addEventListener("click", function () {
      var input = document.getElementById(toggle.getAttribute("aria-controls"));
      if (!input) return;

      var visible = input.type === "password";
      input.type = visible ? "text" : "password";
      toggle.setAttribute("aria-pressed", visible ? "true" : "false");
      toggle.setAttribute("aria-label", visible ? "パスワードを隠す" : "パスワードを表示");
    });
  });
})();