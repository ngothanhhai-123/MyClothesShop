package MyClothesShop.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TopProductDTO {
    private Integer clothesId;
    private String productName;
    private String imageUrl;
    private Long totalSold; // Bắt buộc phải là Long vì hàm SUM() của JPA trả về Long

    // Tự định nghĩa Constructor đúng chuẩn 4 tham số để Hibernate map dữ liệu
    public TopProductDTO(Integer clothesId, String productName, String imageUrl, Long totalSold) {
        this.clothesId = clothesId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.totalSold = totalSold;
    }
}