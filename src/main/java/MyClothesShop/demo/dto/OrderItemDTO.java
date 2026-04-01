package MyClothesShop.demo.dto;

import MyClothesShop.demo.entity.enums.Color;
import MyClothesShop.demo.entity.enums.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Integer variantId;
    private String productName;
    private Color color;
    private Size size;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subTotal; // Thành tiền của món này (giá x số lượng)
    private String imageUrl;
}