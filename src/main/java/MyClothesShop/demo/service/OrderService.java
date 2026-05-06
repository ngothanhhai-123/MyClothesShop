package MyClothesShop.demo.service;

import MyClothesShop.demo.dto.CheckoutResponse;
import MyClothesShop.demo.dto.OrderDetailResponse;
import MyClothesShop.demo.dto.OrderItemDTO;
import MyClothesShop.demo.dto.OrderResponse;
import MyClothesShop.demo.entity.enums.OrderStatus;
import MyClothesShop.demo.dto.CheckoutRequest;
import MyClothesShop.demo.entity.*;
import MyClothesShop.demo.entity.enums.PaymentMethod;
import MyClothesShop.demo.entity.enums.PaymentStatus;
import MyClothesShop.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final ClothesVariantRepository variantRepository;
    private final ClothesImageRepository clothesImageRepository;
    @Transactional
    public String checkout(String email, CheckoutRequest request, PaymentMethod paymentMethod) {

        // 1. Tìm User bằng email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // 2. Kiểm tra xem Web có gửi danh sách sản phẩm lên không
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trên web đang trống, không thể đặt hàng!");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Tạo Đơn hàng (Lưu trước để lấy mã Order ID)
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO); // Tạm set 0, lát cộng xong sẽ update
        Order savedOrder = orderRepository.save(order);

        // 4. Xử lý từng sản phẩm được gửi lên từ Web (ĐÃ SỬA DÙNG ENUM)
        for (CheckoutRequest.OrderItemRequest itemReq : request.getItems()) {

            // Lấy TẤT CẢ biến thể của cái Áo này từ Database dựa vào clothesId
            List<ClothesVariant> variants = variantRepository.findByClothes_ClothesId(itemReq.getClothesId());

            // Lọc ra ĐÚNG cái Biến thể có Màu và Size khách chọn (So sánh Enum bằng dấu ==)
            ClothesVariant variant = variants.stream()
                    .filter(v -> v.getColor() == itemReq.getColor() && v.getSize() == itemReq.getSize())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Sản phẩm '" + itemReq.getName() + "' không có phân loại " + itemReq.getColor() + " - " + itemReq.getSize()));

            if (variant.getStockQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + variant.getClothes().getName() + "' (Size " + variant.getSize() + ") đã hết hàng hoặc không đủ số lượng!");
            }

            // Trừ tồn kho
            variant.setStockQuantity(variant.getStockQuantity() - itemReq.getQuantity());
            variantRepository.save(variant);

            // Cộng tiền
            BigDecimal itemTotal = variant.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // Lưu chi tiết vào bảng OrderDetail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(savedOrder);
            orderDetail.setVariant(variant);
            orderDetail.setProductName(variant.getClothes().getName());
            orderDetail.setColor(variant.getColor());
            orderDetail.setSize(variant.getSize());
            orderDetail.setQuantity(itemReq.getQuantity());
            orderDetail.setPrice(variant.getPrice());
            orderDetailRepository.save(orderDetail);
        }

        // Cập nhật lại tổng tiền chuẩn xác cho đơn hàng
        savedOrder.setTotalAmount(totalAmount);
        orderRepository.save(savedOrder);

        // 5. Lưu thông tin Thanh Toán
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.COD);
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.UNPAID);
        paymentRepository.save(payment);

        // 6. Tự động dọn dẹp Giỏ hàng cũ trong Database (để đồng bộ)
        cartRepository.findByUser_UserId(user.getUserId()).ifPresent(cart -> {
            List<CartDetail> cartDetails = cartDetailRepository.findByCart_CartId(cart.getCartId());
            if (!cartDetails.isEmpty()) {
                cartDetailRepository.deleteAll(cartDetails);
            }
        });

        // 7. Tạo mã QR Thanh toán (Nếu khách chọn QR)
        if (paymentMethod == PaymentMethod.QR) {
            String bankId = "ICB";
            String accountNo = "0812629922";
            String accountName = "Ngo Thanh Hai";
            String description = "Thanh toan don hang " + savedOrder.getOrderId();
            String qrLink = String.format("https://img.vietqr.io/image/%s-%s-compact2.jpg?amount=%s&addInfo=%s&accountName=%s",
                    bankId, accountNo, totalAmount.toBigInteger().toString(), description.replace(" ", "%20"), accountName.replace(" ", "%20"));
            return "Đặt hàng thành công! Link ảnh mã QR thanh toán của bạn: " + qrLink;
        }

        return "Đặt hàng thành công! Mã đơn hàng của bạn là: #" + savedOrder.getOrderId();
    }

    // =====================================================
    // CHECKOUT TỪ GIỎ HÀNG TRONG DATABASE (KHÔNG TIN FRONTEND)
    // Backend tự lấy giỏ hàng, tự tính giá, tự validate tồn kho
    // =====================================================
    @Transactional
    public CheckoutResponse checkoutFromCart(String email, String shippingAddress, String note, PaymentMethod paymentMethod) {

        // 1. Tìm User bằng email (từ JWT Token)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // 2. Lấy giỏ hàng từ Database
        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống hoặc chưa có giỏ hàng!"));

        List<CartDetail> cartDetails = cartDetailRepository.findByCart_CartId(cart.getCartId());
        if (cartDetails.isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        // 3. Validate địa chỉ giao hàng
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập địa chỉ giao hàng!");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 4. Tạo đơn hàng (lưu trước để lấy mã Order ID)
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress.trim());
        order.setNote(note);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        // 5. Duyệt từng sản phẩm trong giỏ hàng DB, validate và tạo OrderDetail
        for (CartDetail cartItem : cartDetails) {
            ClothesVariant variant = cartItem.getVariant();

            // Validate tồn kho (Backend tự kiểm tra, không tin Frontend)
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm '" + variant.getClothes().getName()
                        + "' (Size " + variant.getSize() + ", Màu " + variant.getColor()
                        + ") chỉ còn " + variant.getStockQuantity() + " sản phẩm trong kho!");
            }

            // Trừ tồn kho
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            variantRepository.save(variant);

            // Tính tiền từ giá trong DB (KHÔNG dùng giá Frontend gửi lên)
            BigDecimal itemTotal = variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // Lưu chi tiết đơn hàng
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(savedOrder);
            orderDetail.setVariant(variant);
            orderDetail.setProductName(variant.getClothes().getName());
            orderDetail.setColor(variant.getColor());
            orderDetail.setSize(variant.getSize());
            orderDetail.setQuantity(cartItem.getQuantity());
            orderDetail.setPrice(variant.getPrice()); // Giá gốc từ DB
            orderDetailRepository.save(orderDetail);
        }

        // 6. Cập nhật tổng tiền chuẩn xác
        savedOrder.setTotalAmount(totalAmount);
        orderRepository.save(savedOrder);

        // 7. Lưu thông tin thanh toán
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.COD);
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.UNPAID);
        paymentRepository.save(payment);

        // 8. Xóa giỏ hàng sau khi đặt hàng thành công
        cartDetailRepository.deleteAll(cartDetails);

        // 9. Trả kết quả DTO chuyên nghiệp
        CheckoutResponse response = CheckoutResponse.builder()
                .orderId(savedOrder.getOrderId())
                .message("Đặt hàng thành công! Mã đơn hàng của bạn là: #" + savedOrder.getOrderId())
                .build();

        if (paymentMethod == PaymentMethod.QR) {
            String bankId = "ICB";
            String accountNo = "0812629922";
            String accountName = "Ngo Thanh Hai";
            String description = "Thanh toan don hang " + savedOrder.getOrderId();
            String qrLink = String.format("https://img.vietqr.io/image/%s-%s-compact2.jpg?amount=%s&addInfo=%s&accountName=%s",
                    bankId, accountNo, totalAmount.toBigInteger().toString(), description.replace(" ", "%20"), accountName.replace(" ", "%20"));
            
            response.setQrLink(qrLink);
            response.setMessage("Đặt hàng thành công! Mã đơn hàng của bạn là: #" + savedOrder.getOrderId() + ". Vui lòng quét mã QR bên dưới để thanh toán.");
        }

        return response;
    }

    @Transactional
    public String updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng mã #" + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) throw new RuntimeException("Đơn hàng này đã bị hủy, không thể cập nhật trạng thái!");
        if (order.getStatus() == OrderStatus.COMPLETED) throw new RuntimeException("Đơn hàng này đã hoàn thành, không thể sửa đổi!");
        order.setStatus(newStatus);
        orderRepository.save(order);
        return "Cập nhật trạng thái đơn hàng #" + orderId + " thành " + newStatus + " thành công!";
    }

    public List<OrderResponse> getMyOrders(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));
        return orderRepository.findByUser_UserIdOrderByOrderDateDesc(user.getUserId())
                .stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByOrderDateDesc()
                .stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.setOrderId(order.getOrderId());
        if (order.getUser() != null) {
            dto.setCustomerName(order.getUser().getFullName());
            dto.setCustomerEmail(order.getUser().getEmail());
        } else {
            dto.setCustomerName("Khách vãng lai");
        }
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setOrderDate(order.getOrderDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setNote(order.getNote());
        return dto;
    }

    public OrderDetailResponse getOrderDetails(Integer orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng mã #" + orderId));
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(orderId);

        List<OrderItemDTO> itemDTOs = details.stream().map(detail -> {
            OrderItemDTO item = new OrderItemDTO();
            item.setVariantId(detail.getVariant().getVariantId());
            item.setProductName(detail.getProductName());
            item.setColor(detail.getColor());
            item.setSize(detail.getSize());
            item.setQuantity(detail.getQuantity());
            item.setPrice(detail.getPrice());
            item.setSubTotal(detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));

            if (detail.getVariant() != null && detail.getVariant().getClothes() != null) {
                Integer clothesId = detail.getVariant().getClothes().getClothesId();

                // Gọi DB để tìm tất cả ảnh của cái Áo này
                List<ClothesImage> images = clothesImageRepository.findByClothes_ClothesId(clothesId);

                if (images != null && !images.isEmpty()) {
                    item.setImageUrl(images.get(0).getImageUrl()); // Lấy ảnh đầu tiên
                } else {
                    item.setImageUrl("");
                }
            }

            return item;
        }).collect(Collectors.toList());

        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrderInfo(mapToOrderResponse(order));
        response.setItems(itemDTOs);
        return response;
    }
}