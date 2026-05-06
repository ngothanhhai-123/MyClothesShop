package MyClothesShop.demo.repository;

import MyClothesShop.demo.dto.TopProductDTO;
import MyClothesShop.demo.entity.Order;
import MyClothesShop.demo.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // ==================================================
    // KHU VỰC 1: CÁC HÀM TỔNG CỦA CỬA HÀNG (GIỮ NGUYÊN)
    // ==================================================

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal sumTotalRevenue();

    long countByStatus(OrderStatus status);

    @Query("SELECT DAY(o.orderDate), SUM(o.totalAmount) FROM Order o " +
            "WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status = 'COMPLETED' " +
            "GROUP BY DAY(o.orderDate) ORDER BY DAY(o.orderDate)")
    List<Object[]> getRevenueByDay(@Param("year") int year, @Param("month") int month);

    @Query("SELECT MONTH(o.orderDate), SUM(o.totalAmount) FROM Order o " +
            "WHERE YEAR(o.orderDate) = :year AND o.status = 'COMPLETED' " +
            "GROUP BY MONTH(o.orderDate) ORDER BY MONTH(o.orderDate)")
    List<Object[]> getRevenueByMonth(@Param("year") int year);

    @Query("SELECT YEAR(o.orderDate), SUM(o.totalAmount) FROM Order o " +
            "WHERE o.status = 'COMPLETED' " +
            "GROUP BY YEAR(o.orderDate) ORDER BY YEAR(o.orderDate)")
    List<Object[]> getRevenueByYear();

    List<Order> findByUser_UserIdOrderByOrderDateDesc(Integer userId);

    List<Order> findAllByOrderByOrderDateDesc();

    @Query("SELECT new MyClothesShop.demo.dto.TopProductDTO(c.clothesId, c.name, ci.imageUrl, SUM(od.quantity), SUM(od.price * od.quantity)) " +
            "FROM Order o " +
            "JOIN o.orderDetails od " +
            "JOIN od.variant cv " +
            "JOIN cv.clothes c " +
            "LEFT JOIN ClothesImage ci ON c.clothesId = ci.clothes.clothesId AND ci.isThumbnail = true " +
            "WHERE o.status = 'COMPLETED' " +
            "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate < :endDate) " +
            "GROUP BY c.clothesId, c.name, ci.imageUrl " +
            "ORDER BY SUM(od.quantity) DESC")
    List<TopProductDTO> findTopSellingProducts(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    @Query("SELECT new MyClothesShop.demo.dto.TopProductDTO(c.clothesId, c.name, ci.imageUrl, SUM(od.quantity), SUM(od.price * od.quantity)) " +
            "FROM Order o " +
            "JOIN o.orderDetails od " +
            "JOIN od.variant cv " +
            "JOIN cv.clothes c " +
            "LEFT JOIN ClothesImage ci ON c.clothesId = ci.clothes.clothesId AND ci.isThumbnail = true " +
            "WHERE o.status = 'COMPLETED' " +
            "GROUP BY c.clothesId, c.name, ci.imageUrl " +
            "HAVING SUM(od.quantity) >= 1 " +
            "ORDER BY SUM(od.quantity) DESC")
    List<TopProductDTO> findAllHotProducts();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.userId = :userId AND o.status = 'COMPLETED'")
    BigDecimal sumTotalAmountByUser(@Param("userId") Integer userId);


    // ==================================================
    // KHU VỰC 2: CÁC HÀM THỐNG KÊ SẢN PHẨM MỚI (ĐÃ ĐỔI SANG CURRENT_DATE)
    // ==================================================

    // 0. Tính tổng cục tiền của Sản phẩm mới (Cho cái hộp vuông ở trên cùng)
    @Query("SELECT SUM(od.price * od.quantity) FROM Order o " +
            "JOIN o.orderDetails od JOIN od.variant cv JOIN cv.clothes c " +
            "WHERE o.status = 'COMPLETED' " +
            "AND FUNCTION('DATEDIFF', CURRENT_DATE, c.createdAt) <= 7")
    BigDecimal sumNewProductRevenue();

    // 1. Biểu đồ theo NGÀY (Lấy các Ngày trong 1 Tháng cụ thể)
    @Query("SELECT DAY(o.orderDate), SUM(od.price * od.quantity) FROM Order o " +
            "JOIN o.orderDetails od JOIN od.variant cv JOIN cv.clothes c " +
            "WHERE o.status = 'COMPLETED' AND YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month " +
            "AND FUNCTION('DATEDIFF', CURRENT_DATE, c.createdAt) <= 7 " +
            "GROUP BY DAY(o.orderDate) ORDER BY DAY(o.orderDate)")
    List<Object[]> getNewProductRevenueByDay(@Param("year") int year, @Param("month") int month);

    // 2. Biểu đồ theo THÁNG (Lấy các Tháng trong 1 Năm cụ thể)
    @Query("SELECT MONTH(o.orderDate), SUM(od.price * od.quantity) FROM Order o " +
            "JOIN o.orderDetails od JOIN od.variant cv JOIN cv.clothes c " +
            "WHERE o.status = 'COMPLETED' AND YEAR(o.orderDate) = :year " +
            "AND FUNCTION('DATEDIFF', CURRENT_DATE, c.createdAt) <= 7 " +
            "GROUP BY MONTH(o.orderDate) ORDER BY MONTH(o.orderDate)")
    List<Object[]> getNewProductRevenueByMonth(@Param("year") int year);

    // 3. Biểu đồ theo NĂM (Lấy Doanh thu của tất cả các Năm)
    @Query("SELECT YEAR(o.orderDate), SUM(od.price * od.quantity) FROM Order o " +
            "JOIN o.orderDetails od JOIN od.variant cv JOIN cv.clothes c " +
            "WHERE o.status = 'COMPLETED' " +
            "AND FUNCTION('DATEDIFF', CURRENT_DATE, c.createdAt) <= 7 " +
            "GROUP BY YEAR(o.orderDate) ORDER BY YEAR(o.orderDate)")
    List<Object[]> getNewProductRevenueByYear();
}
