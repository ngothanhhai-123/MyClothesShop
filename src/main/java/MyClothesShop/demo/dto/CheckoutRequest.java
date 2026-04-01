package MyClothesShop.demo.dto;

import MyClothesShop.demo.entity.enums.Color;
import MyClothesShop.demo.entity.enums.PaymentMethod;
import MyClothesShop.demo.entity.enums.Size;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequest {
    private String shippingAddress;
    private String note;
    private PaymentMethod paymentMethod;

    // THÊM DÒNG NÀY ĐỂ HỨNG GIỎ HÀNG TỪ WEB
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Integer clothesId; // Đổi variantId thành clothesId
        private String name;
        private Color color;      // Thêm biến hứng Màu
        private Size size;       // Thêm biến hứng Size
        private Integer quantity;
    }
}