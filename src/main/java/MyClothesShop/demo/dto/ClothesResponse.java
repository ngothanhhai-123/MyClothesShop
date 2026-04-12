package MyClothesShop.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClothesResponse {
    private Integer clothesId;
    private String name;
    private String description;
    private Integer categoryId;    // Thêm dòng này
    private String categoryName;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private Boolean isNewProduct;
    private BigDecimal price;      // Thêm dòng này để hết lỗi setPrice
    private List<String> imageUrls;
    private List<VariantDTO> variants;
}