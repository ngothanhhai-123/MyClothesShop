// ==========================================
// FILE: js/common.js (Dùng chung cho toàn web)
// ==========================================

// 1. Hàm tải Header / Footer
function loadComponent(id, file, callback) {
    fetch(file)
        .then(response => response.text())
        .then(data => {
            const element = document.getElementById(id);
            if (element) {
                element.innerHTML = data;
                if (callback) callback(); 
            }
        })
        .catch(err => console.error(`Lỗi tải ${file}:`, err));
}

// 2. Hàm gọi API và vẽ Menu động
function fetchCategories() {
    axios.get('http://localhost:8080/api/categories')
        .then(response => {
            const allCategories = response.data;
            const navMenu = document.getElementById('dynamic-nav-menu');
            if (!navMenu) return; // Nếu trang nào không có menu thì bỏ qua
            
            // --- SỬA Ở ĐÂY: Bơm thêm link HOT và Tất Cả Sản Phẩm vào chuỗi HTML gốc ---
            let htmlContent = `
                <li><a href="category.html?type=hot" class="nav-link" style="color: #e74c3c; font-weight: 800;">Sản Phẩm HOT</a></li>
                <li><a href="category.html" class="nav-link">Tất Cả Sản Phẩm</a></li>
            `;
            
            // Lọc danh mục Cha
            const parentCategories = allCategories.filter(cat => !cat.parentId && !cat.parent);
            
            parentCategories.forEach(parent => {
                // Lọc danh mục Con
                const childCategories = allCategories.filter(cat => 
                    (cat.parentId === parent.categoryId) || 
                    (cat.parent && cat.parent.categoryId === parent.categoryId)
                );
                
                if (childCategories.length > 0) {
                    htmlContent += `
                        <li class="my-dropdown">
                            <a href="category.html?id=${parent.categoryId}" class="nav-link">
                                ${parent.name} <i class="fa-solid fa-angle-down" style="font-size: 11px; margin-left: 4px;"></i>
                            </a>
                            <ul class="my-dropdown-menu">`;
                    childCategories.forEach(child => {
                        htmlContent += `<li><a href="category.html?id=${child.categoryId}">${child.name}</a></li>`;
                    });
                    htmlContent += `</ul></li>`;
                } else {
                    htmlContent += `<li><a href="category.html?id=${parent.categoryId}" class="nav-link">${parent.name}</a></li>`;
                }
            });
            
            // Render toàn bộ ra màn hình
            navMenu.innerHTML = htmlContent;
        })
        .catch(err => console.error("Lỗi tải Danh mục", err));
}

// 3. TỰ ĐỘNG CHẠY KHI MỞ BẤT KỲ TRANG NÀO
document.addEventListener("DOMContentLoaded", function() {
    // Tự động nhúng Header (kèm gọi Menu)
    if(document.getElementById('header-placeholder')) {
        loadComponent('header-placeholder', 'components/header.html', fetchCategories);
    }
    
    // Tự động nhúng Footer
    if(document.getElementById('footer-placeholder')) {
        loadComponent('footer-placeholder', 'components/footer.html');
    }
});
// ======================================================
// XỬ LÝ TÌM KIẾM TRÊN HEADER CHUYỂN HƯỚNG SANG SEARCH.HTML
// ======================================================

// Hàm chạy khi bấm vào hình kính lúp
function executeHeaderSearch() {
    const searchInput = document.getElementById('header-search-input');
    if (searchInput) {
        const keyword = searchInput.value.trim();
        // Cứ có chữ hoặc để trống đều bế sang trang search.html hết
        window.location.href = `search.html?keyword=${encodeURIComponent(keyword)}`;
    }
}

// Hàm chạy khi khách đang gõ mà lười bấm chuột, gõ Enter luôn
function handleHeaderSearch(event) {
    if (event.key === 'Enter') {
        event.preventDefault(); // Ngăn trình duyệt tự load lại trang
        executeHeaderSearch();
    }
}
// ==========================================
// HỆ THỐNG THÔNG BÁO XỊN SÒ (TOAST NOTIFICATION)
// ==========================================
function showNotification(message, isSuccess = true) {
    // Tạo 1 thẻ div chứa thông báo
    const toast = document.createElement('div');
    toast.innerText = message;
    
    // Gắn CSS trực tiếp để nó nổi lên ở góc trên bên phải
    const bgColor = isSuccess ? '#000000' : '#e74c3c'; // Đen nếu thành công, Đỏ nếu lỗi
    toast.style.cssText = `
        position: fixed; 
        top: 20px; 
        right: 20px; 
        background-color: ${bgColor}; 
        color: #fff; 
        padding: 15px 25px; 
        border-radius: 4px; 
        font-family: 'Inter', sans-serif;
        font-weight: 500;
        z-index: 99999; 
        box-shadow: 0 4px 12px rgba(0,0,0,0.15); 
        transition: all 0.4s ease; 
        opacity: 0; 
        transform: translateY(-20px);
    `;

    document.body.appendChild(toast);

    // Hiệu ứng trượt xuống và hiện ra
    setTimeout(() => {
        toast.style.opacity = "1";
        toast.style.transform = "translateY(0)";
    }, 10);

    // Tự động bay màu sau 3 giây
    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform = "translateY(-20px)";
        // Xóa hẳn thẻ div khỏi HTML sau khi làm mờ xong
        setTimeout(() => toast.remove(), 400); 
    }, 3000);
}