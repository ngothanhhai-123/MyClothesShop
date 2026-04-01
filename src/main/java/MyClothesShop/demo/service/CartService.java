package MyClothesShop.demo.service;

import MyClothesShop.demo.dto.CartItemDTO;
import MyClothesShop.demo.dto.CartResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import MyClothesShop.demo.dto.AddToCartRequest;
import MyClothesShop.demo.entity.Cart;
import MyClothesShop.demo.entity.CartDetail;
import MyClothesShop.demo.entity.ClothesVariant;
import MyClothesShop.demo.entity.User;
import MyClothesShop.demo.repository.CartDetailRepository;
import MyClothesShop.demo.repository.CartRepository;
import MyClothesShop.demo.repository.ClothesVariantRepository;
import MyClothesShop.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final UserRepository userRepository;
    private final ClothesVariantRepository variantRepository;

    @Transactional
    // 1. Thêm tham số 'String email' vào hàm
    public String addToCart(String email, AddToCartRequest request) {

        // 2. Sửa lại cách tìm User: dùng findByEmail thay vì findById
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        // 2. Kiểm tra Biến thể (Áo màu gì, size gì) có tồn tại và đủ tồn kho không
        ClothesVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Kho không đủ hàng! Chỉ còn " + variant.getStockQuantity() + " chiếc.");
        }

        // 3. Tìm giỏ hàng của khách, nếu khách chưa có giỏ thì tạo mới luôn
        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        // 4. Kiểm tra xem sản phẩm này đã có trong giỏ hàng chưa
        Optional<CartDetail> existingDetail = cartDetailRepository.findByCart_CartId(cart.getCartId())
                .stream()
                .filter(detail -> detail.getVariant().getVariantId().equals(variant.getVariantId()))
                .findFirst();

        if (existingDetail.isPresent()) {
            // Nếu khách đã từng thêm áo này vào giỏ -> Cộng dồn số lượng lên
            CartDetail detail = existingDetail.get();
            int newQuantity = detail.getQuantity() + request.getQuantity();

            // Kiểm tra lại tồn kho sau khi cộng dồn
            if (variant.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Tổng số lượng trong giỏ vượt quá tồn kho!");
            }
            detail.setQuantity(newQuantity);
            cartDetailRepository.save(detail);
        } else {
            // Nếu áo chưa có trong giỏ -> Tạo dòng chi tiết mới
            CartDetail newDetail = new CartDetail();
            newDetail.setCart(cart);
            newDetail.setVariant(variant);
            newDetail.setQuantity(request.getQuantity());
            cartDetailRepository.save(newDetail);
        }

        return "Thêm thành công " + request.getQuantity() + " sản phẩm vào giỏ hàng!";
    }
    // Đổi tên hàm và nhận vào email thay vì userId
    public CartResponse getCartByEmail(String email) {
        // 1. Tìm User từ Database bằng email (Lấy từ Token)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Rút cái userId ra để xài cho các dòng code bên dưới
        Integer userId = user.getUserId();

        // 2. Lấy giỏ hàng của User (Logic cũ của bạn, không đổi 1 chữ nào)
        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống hoặc người dùng chưa có giỏ hàng!"));

        // 3. Lấy danh sách các món đồ trong giỏ
        List<CartDetail> details = cartDetailRepository.findByCart_CartId(cart.getCartId());

        CartResponse response = new CartResponse();
        response.setCartId(cart.getCartId());
        response.setUserId(userId);

        BigDecimal totalCartPrice = BigDecimal.ZERO;
        List<CartItemDTO> itemDTOs = new ArrayList<>();

        // 4. Duyệt qua từng món đồ, map dữ liệu và tính tiền
        for (CartDetail detail : details) {
            CartItemDTO item = new CartItemDTO();
            item.setCartDetailId(detail.getCartDetailId());
            item.setVariantId(detail.getVariant().getVariantId());

            // Lấy tên áo từ bảng Clothes thông qua Variant
            item.setProductName(detail.getVariant().getClothes().getName());
            item.setColor(detail.getVariant().getColor());
            item.setSize(detail.getVariant().getSize());
            item.setPrice(detail.getVariant().getPrice());
            item.setQuantity(detail.getQuantity());

            // Tính thành tiền cho món đồ này: Giá * Số lượng
            BigDecimal itemTotal = detail.getVariant().getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            item.setItemTotal(itemTotal);

            // Cộng dồn vào tổng tiền cả giỏ
            totalCartPrice = totalCartPrice.add(itemTotal);

            itemDTOs.add(item);
        }

        response.setItems(itemDTOs);
        response.setTotalCartPrice(totalCartPrice);

        return response;
    }
    // 3. API CẬP NHẬT SỐ LƯỢNG SẢN PHẨM TRONG GIỎ
    @Transactional
    public String updateCartItem(String email, Integer variantId, Integer newQuantity) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));

        // Tìm các món trong giỏ
        List<CartDetail> cartDetails = cartDetailRepository.findByCart_CartId(cart.getCartId());

        CartDetail targetItem = cartDetails.stream()
                .filter(item -> item.getVariant().getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng!"));

        // Nếu khách giảm số lượng về 0 -> Tự động xóa luôn khỏi giỏ
        if (newQuantity <= 0) {
            cartDetailRepository.delete(targetItem);
            return "Đã xóa sản phẩm khỏi giỏ hàng!";
        }

        // Kiểm tra xem kho còn đủ hàng không
        if (targetItem.getVariant().getStockQuantity() < newQuantity) {
            throw new RuntimeException("Số lượng tồn kho không đủ! Chỉ còn " + targetItem.getVariant().getStockQuantity() + " sản phẩm.");
        }

        targetItem.setQuantity(newQuantity);
        cartDetailRepository.save(targetItem);
        return "Cập nhật số lượng thành công!";
    }

    // 4. API XÓA HẲN MỘT SẢN PHẨM KHỎI GIỎ
    @Transactional
    public String removeCartItem(String email, Integer variantId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));

        List<CartDetail> cartDetails = cartDetailRepository.findByCart_CartId(cart.getCartId());

        CartDetail targetItem = cartDetails.stream()
                .filter(item -> item.getVariant().getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng!"));

        cartDetailRepository.delete(targetItem);
        return "Đã xóa sản phẩm khỏi giỏ hàng!";
    }
}