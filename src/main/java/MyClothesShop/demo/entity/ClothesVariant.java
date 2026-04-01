package MyClothesShop.demo.entity;

import MyClothesShop.demo.entity.enums.Color;
import MyClothesShop.demo.entity.enums.Size;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Clothes_Variant")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothesVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Integer variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Enumerated(EnumType.STRING) // Bắt buộc phải là STRING để nó lưu dưới dạng chữ
    @Column(name = "color", length = 50)
    private Color color;

    @Enumerated(EnumType.STRING)
    @Column(name = "size")
    private Size size;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false; // Tính năng Soft Delete
}