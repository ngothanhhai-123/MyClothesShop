package MyClothesShop.demo.entity;

import MyClothesShop.demo.util.StringHelper;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Clothes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clothes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothes_id")
    private Integer clothesId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "search_name")
    private String searchName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false; // Tính năng Soft Delete

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL)
    private List<ClothesVariant> variants;

    @PrePersist
    protected void onCreate() {
        // 1. Lưu thời gian tạo
        this.createdAt = LocalDateTime.now();

        // 2. Lưu luôn tên không dấu lúc mới tạo sản phẩm
        if (this.name != null) {
            this.searchName = StringHelper.removeAccent(this.name);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Chỉ chạy khi sếp cập nhật (sửa) tên sản phẩm
        if (this.name != null) {
            this.searchName = StringHelper.removeAccent(this.name);
        }
    }
}