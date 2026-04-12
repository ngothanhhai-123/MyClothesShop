// Cấu hình URL mặc định của Backend
axios.defaults.baseURL = 'http://localhost:8080';

// Tạo "Trạm kiểm soát" trước khi gửi bất kỳ Request nào đi
axios.interceptors.request.use(function (config) {
    // 1. Lấy Token bạn đã cất trong kho của trình duyệt (lúc đăng nhập)
    const token = localStorage.getItem('jwt_token'); 
    
    // 2. Nếu có token, tự động kẹp vào cái Header có tên là "Authorization"
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config; // Cho xe qua trạm
}, function (error) {
    return Promise.reject(error);
});