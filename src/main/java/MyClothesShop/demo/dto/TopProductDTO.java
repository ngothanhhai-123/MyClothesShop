package MyClothesShop.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class TopProductDTO {
    private Integer clothesId;
    private String productName;
    private String imageUrl;
    private Long totalSold; // Bắt buộc phải là Long vì hàm SUM() của JPA trả về Long
    private BigDecimal totalRevenue;

    // Tự định nghĩa Constructor đúng chuẩn để Hibernate map dữ liệu
    public TopProductDTO(Integer clothesId, String productName, String imageUrl, Long totalSold) {
        this.clothesId = clothesId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.totalSold = totalSold;
        this.totalRevenue = BigDecimal.ZERO;
    }

    public TopProductDTO(Integer clothesId, String productName, String imageUrl, Long totalSold, BigDecimal totalRevenue) {
        this.clothesId = clothesId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.totalSold = totalSold;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }
}
