(function () {
  "use strict";

  var elements = document.querySelectorAll(".reveal");
  var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (!elements.length || reduceMotion || !("IntersectionObserver" in window)) return;

  document.documentElement.classList.add("motion-ready");
  var observer = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("is-visible");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.12, rootMargin: "0px 0px -5% 0px" });

  elements.forEach(function (element) { observer.observe(element); });
})();