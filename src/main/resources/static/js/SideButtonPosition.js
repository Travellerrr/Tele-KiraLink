
const toggleBtn = document.getElementById('toggle-btn');
let isDragging = false, startY = 0, currentTop = 20;

toggleBtn.style.top = `${currentTop}px`;

toggleBtn.addEventListener('mousedown', startDrag);
document.addEventListener('mousemove', drag);
document.addEventListener('mouseup', endDrag);

toggleBtn.addEventListener('touchstart', e => startDrag(e.touches[0]));
document.addEventListener('touchmove', e => drag(e.touches[0]));
document.addEventListener('touchend', endDrag);

function startDrag(e) {
    isDragging = true;
    startY = e.clientY;
    toggleBtn.classList.add('dragging');
    toggleBtn.style.transition = 'none';
}

function drag(e) {
    if (!isDragging) return;
    const deltaY = e.clientY - startY;
    startY = e.clientY;

    const maxTop = sidebar.getBoundingClientRect().height - toggleBtn.offsetHeight - 20;
    currentTop = Math.max(20, Math.min(maxTop, currentTop + deltaY));

    updateButtonPosition();
}

function endDrag() {
    isDragging = false;
    toggleBtn.classList.remove('dragging');
    toggleBtn.style.transition = 'top 0.2s ease';
    updateButtonPosition();
}

function updateButtonPosition() {
    toggleBtn.style.top = `${currentTop}px`;
    sidebar.scrollTop = currentTop - 20;
    savePosition();
}

sidebar.addEventListener('scroll', () => {
    if (!isDragging) {
        currentTop = sidebar.scrollTop + 20;
        updateButtonPosition();
    }
});

toggleBtn.addEventListener('dblclick', () => {
    currentTop = 20;
    updateButtonPosition();
});

function savePosition() {
    localStorage.setItem('toggleBtnPosition', currentTop);
}

function loadPosition() {
    const saved = localStorage.getItem('toggleBtnPosition');
    if (saved) currentTop = parseInt(saved);
    updateButtonPosition();
}

document.addEventListener('DOMContentLoaded', loadPosition);