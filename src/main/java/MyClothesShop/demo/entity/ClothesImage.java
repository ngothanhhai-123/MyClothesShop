package MyClothesShop.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Clothes_Image")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothesImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Integer imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id", nullable = false)
    private Clothes clothes;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_thumbnail")
    private Boolean isThumbnail = false;
}