package MyClothesShop.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {
    private Integer orderId;
    private String message;
    private String qrLink;
    private String amount;
    private String bankAccount;
    private String accountName;
    private String description;
}
