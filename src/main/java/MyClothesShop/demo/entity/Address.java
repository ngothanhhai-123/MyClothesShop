package MyClothesShop.demo.entity;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Table(name = "address")
@Data
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "phone_number")
    private String phoneNumber;

    // THÊM MỚI: 3 trường Tỉnh/Thành, Quận/Huyện, Phường/Xã
    @Column(name = "province")
    private String province;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    // Sửa tên biến thành addressDetail cho chuẩn logic (chỉ lưu số nhà, ngõ)
    // Vẫn giữ name = "shipping_address" để không bị lỗi với bảng cũ trong MySQL
    @Column(name = "shipping_address")
    private String addressDetail;

    @Column(name = "is_default")
    private Boolean isDefault;

}