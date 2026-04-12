package MyClothesShop.demo.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phoneNumber;

    // Nâng cấp: Nhận 4 trường tách biệt thay vì 1 chuỗi gộp
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
}