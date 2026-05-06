package MyClothesShop.demo.controller;

import MyClothesShop.demo.dto.CheckoutRequest;
import MyClothesShop.demo.dto.CheckoutResponse;
import MyClothesShop.demo.dto.OrderDetailResponse;
import MyClothesShop.demo.dto.OrderResponse;
import MyClothesShop.demo.entity.enums.OrderStatus;
import MyClothesShop.demo.entity.enums.PaymentMethod; // Bắt buộc phải import cái này
import MyClothesShop.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ĐÃ NÂNG CẤP: Thêm @RequestParam PaymentMethod để Swagger vẽ Dropdown
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestBody CheckoutRequest request,
            @RequestParam PaymentMethod paymentMethod, // Đưa ra đây
            Principal principal) {
        try {
            // 1. Tự động lấy Email khách hàng từ Token
            String email = principal.getName();

            // 2. Truyền ĐỦ 3 THAM SỐ xuống Service
            String message = orderService.checkout(email, request, paymentMethod);

            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =====================================================
    // API CHECKOUT MỚI: Lấy giỏ hàng từ DB, Backend tự tính giá
    // Frontend KHÔNG gửi danh sách items, chỉ gửi thông tin giao hàng
    // =====================================================
    @PostMapping("/checkout-cart")
    public ResponseEntity<?> checkoutFromCart(
            @RequestParam(required = false) String shippingAddress,
            @RequestParam(required = false) String note,
            @RequestParam PaymentMethod paymentMethod,
            Principal principal) {
        try {
            String email = principal.getName();
            CheckoutResponse result = orderService.checkoutFromCart(email, shippingAddress, note, paymentMethod);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API Cập nhật trạng thái đơn hàng (Dành cho ADMIN) -> GIỮ NGUYÊN
    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestParam OrderStatus status) {
        try {
            String message = orderService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Khách hàng gọi API này để xem đơn của mình (Nhận Email từ Token)
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Principal principal) {
        // Lấy email từ token của người đang đăng nhập
        String email = principal.getName();
        return ResponseEntity.ok(orderService.getMyOrders(email));
    }

    // Admin gọi API này để xem toàn bộ đơn
    @GetMapping("/admin")
    public ResponseEntity<List<OrderResponse>> getAllOrdersForAdmin() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    // Xem chi tiết một đơn hàng cụ thể
    @GetMapping("/{orderId}/details")
    public ResponseEntity<OrderDetailResponse> getOrderDetails(@PathVariable Integer orderId) {
        return ResponseEntity.ok(orderService.getOrderDetails(orderId));
    }
}