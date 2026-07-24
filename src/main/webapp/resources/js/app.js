/* Kimwanyi SACCO Interactive Production JavaScript */

document.addEventListener('DOMContentLoaded', () => {
    initSidebarToggle();
    initModalHandlers();
    initTableSearchFilters();
});

function initSidebarToggle() {
    const toggleBtn = document.getElementById('sidebarToggleBtn');
    const sidebar = document.querySelector('.app-sidebar');
    
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('show');
        });
    }
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
    }
}

function initModalHandlers() {
    document.querySelectorAll('.modal-backdrop').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.classList.remove('show');
            }
        });
    });
}

function initTableSearchFilters() {
    const searchInputs = document.querySelectorAll('.table-search-input');
    
    searchInputs.forEach(input => {
        const targetTableId = input.getAttribute('data-table');
        const table = document.getElementById(targetTableId);
        
        if (table) {
            input.addEventListener('input', () => {
                const query = input.value.toLowerCase().trim();
                const rows = table.querySelectorAll('tbody tr');
                
                rows.forEach(row => {
                    const text = row.textContent.toLowerCase();
                    if (text.includes(query)) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                });
            });
        }
    });
}

// Toast notification display helper
function showToast(message, type = 'info') {
    const toastContainer = document.getElementById('toastContainer') || createToastContainer();
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.style.cssText = `
        background: #ffffff;
        border-left: 4px solid ${type === 'success' ? '#059669' : type === 'danger' ? '#dc2626' : '#2563eb'};
        box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1);
        padding: 14px 20px;
        border-radius: 8px;
        margin-bottom: 10px;
        font-size: 14px;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 12px;
        animation: slideUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    `;
    
    toast.innerHTML = `<span>${message}</span>`;
    toastContainer.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.style.cssText = `
        position: fixed;
        bottom: 24px;
        right: 24px;
        z-index: 9999;
    `;
    document.body.appendChild(container);
    return container;
}

// Force revalidation from server when navigating via browser back/forward buttons
window.addEventListener('pageshow', function (event) {
    if (event.persisted || (window.performance && window.performance.navigation && window.performance.navigation.type === 2)) {
        window.location.reload();
    }
});

