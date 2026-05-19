class RtvsStudioApp {
  private readonly statusEl: HTMLElement | null;

  public constructor() {
    this.statusEl = document.getElementById('status');
  }

  public bootstrap(): void {
    if (this.statusEl) {
      this.statusEl.textContent = 'RTVS source tree is present.';
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  new RtvsStudioApp().bootstrap();
});
