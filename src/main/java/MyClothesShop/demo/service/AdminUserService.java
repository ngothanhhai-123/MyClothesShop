package MyClothesShop.demo.service;

import MyClothesShop.demo.dto.UserDTO;
import MyClothesShop.demo.entity.User;
import MyClothesShop.demo.entity.enums.UserStatus;
import MyClothesShop.demo.repository.OrderRepository;
import MyClothesShop.demo.repository.UserRepository;
import MyClothesShop.demo.util.StringHelper; // Nhớ import cái này sếp nhé
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final OrderRepository orderRepository;

    // 1. Lấy danh sách toàn bộ khách hàng (ĐÃ BỔ SUNG TÍNH TIỀN)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    // Bước 1: Vẫn lấy các thông tin cơ bản từ hàm cũ
                    UserDTO dto = UserDTO.fromEntity(user);

                    // Bước 2: Gọi DB tính tổng tiền các đơn COMPLETED của ông khách này
                    BigDecimal total = orderRepository.sumTotalAmountByUser(user.getUserId());

                    // Bước 3: Nhét tiền vào DTO (nếu null thì gán bằng 0đ)
                    dto.setTotalSpent(total != null ? total : BigDecimal.ZERO);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ========================================================
    // 2. Tìm kiếm khách hàng (ĐÃ TÍCH HỢP CẠO DẤU + TÍNH TIỀN)
    // ========================================================
    public List<UserDTO> searchUsers(String keyword) {
        // Cạo dấu từ khóa của Admin trước khi đưa xuống DB
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? StringHelper.removeAccent(keyword.trim())
                : "";

        // Nhớ đảm bảo hàm searchByKeyword trong UserRepository đang LIKE với cột searchName sếp nhé!
        return userRepository.searchByKeyword(finalKeyword).stream()
                .map(user -> {
                    UserDTO dto = UserDTO.fromEntity(user);
                    BigDecimal total = orderRepository.sumTotalAmountByUser(user.getUserId());
                    dto.setTotalSpent(total != null ? total : BigDecimal.ZERO);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // 3. Khóa hoặc Mở khóa tài khoản
    @Transactional
    public String toggleUserStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng mã #" + userId));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ADMIN"));
        if (isAdmin) {
            throw new RuntimeException("Không thể khóa tài khoản của Quản trị viên!");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.LOCKED);
            userRepository.save(user);

            // Gửi email thông báo khóa tài khoản
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject("Thông báo: Tài khoản của bạn đã bị khóa - MyClothesShop");
                message.setText("Xin chào " + user.getFullName() + ",\n\n"
                        + "Tài khoản của bạn trên hệ thống MyClothesShop đã bị Quản trị viên khóa do phát hiện bất thường hoặc vi phạm chính sách của cửa hàng.\n\n"
                        + "Nếu bạn cho rằng đây là một sự nhầm lẫn, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi để được giải quyết.\n\n"
                        + "Trân trọng,\nĐội ngũ MyClothesShop.");
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Lỗi khi gửi email thông báo khóa tài khoản: " + e.getMessage());
            }

            return "Đã KHÓA tài khoản của: " + user.getEmail();
        } else {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            return "Đã MỞ KHÓA tài khoản của: " + user.getEmail();
        }
    }
}