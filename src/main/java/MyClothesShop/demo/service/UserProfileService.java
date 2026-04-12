package MyClothesShop.demo.service;

import MyClothesShop.demo.dto.ChangePasswordRequest;
import MyClothesShop.demo.dto.UpdateProfileRequest;
import MyClothesShop.demo.dto.UserProfileResponse;
import MyClothesShop.demo.entity.Address;
import MyClothesShop.demo.entity.User;
import MyClothesShop.demo.repository.AddressRepository;
import MyClothesShop.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AddressRepository addressRepository;

    // 1. API: Xem thông tin cá nhân
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        List<Address> addresses = addressRepository.findByUser_UserId(user.getUserId());
        String fullAddress = "";

        if (!addresses.isEmpty()) {
            Address addr = addresses.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                    .findFirst()
                    .orElse(addresses.get(0));

            // Nối chuỗi thông minh: Số nhà, Phường, Quận, Tỉnh
            StringBuilder sb = new StringBuilder();
            if (addr.getAddressDetail() != null) sb.append(addr.getAddressDetail());
            if (addr.getWard() != null) sb.append(sb.length() > 0 ? ", " : "").append(addr.getWard());
            if (addr.getDistrict() != null) sb.append(sb.length() > 0 ? ", " : "").append(addr.getDistrict());
            if (addr.getProvince() != null) sb.append(sb.length() > 0 ? ", " : "").append(addr.getProvince());

            fullAddress = sb.toString();
        }

        UserProfileResponse response = new UserProfileResponse();
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(fullAddress);

        return response;
    }

    // 2. API: Cập nhật thông tin cá nhân và lưu Địa chỉ
    @Transactional
    public String updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);

        // Chỉ xử lý địa chỉ nếu có ít nhất 1 trường địa chỉ được gửi lên
        if (request.getProvince() != null || request.getAddressDetail() != null) {
            List<Address> addresses = addressRepository.findByUser_UserId(user.getUserId());
            Address addr = addresses.isEmpty() ? new Address() :
                    addresses.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                            .findFirst().orElse(addresses.get(0));

            if (addresses.isEmpty()) addr.setUser(user);

            // Ghi đè các trường mới
            addr.setProvince(request.getProvince());
            addr.setDistrict(request.getDistrict());
            addr.setWard(request.getWard());
            addr.setAddressDetail(request.getAddressDetail());

            addr.setRecipientName(user.getFullName());
            addr.setPhoneNumber(user.getPhoneNumber());
            addr.setIsDefault(true);

            addressRepository.save(addr);
        }
        return "Cập nhật thành công!";
    }

    // 3. API: Đổi mật khẩu
    @Transactional
    public String changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp!");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Đổi mật khẩu thành công! Lần đăng nhập sau hãy dùng mật khẩu mới nhé.";
    }
}