(function () {
  class RtvsStudioApp {
    constructor() {
      this.statusEl = document.getElementById('status');
    }

    bootstrap() {
      if (this.statusEl) {
        this.statusEl.textContent = 'RTVS source tree is present.';
      }
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    new RtvsStudioApp().bootstrap();
  });
})();
