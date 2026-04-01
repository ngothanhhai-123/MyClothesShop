package MyClothesShop.demo.dto;

import MyClothesShop.demo.entity.enums.Color;
import MyClothesShop.demo.entity.enums.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantDTO {
    private Integer variantId;
    private Color color;
    private Size size;
    private BigDecimal price;
    private Integer stockQuantity;
}