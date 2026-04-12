package MyClothesShop.demo.service;

import MyClothesShop.demo.dto.ChartDTO;
import MyClothesShop.demo.dto.DashboardResponse;
import MyClothesShop.demo.entity.enums.OrderStatus;
import MyClothesShop.demo.repository.ClothesRepository;
import MyClothesShop.demo.repository.OrderRepository;
import MyClothesShop.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ClothesRepository clothesRepository;
    private final UserRepository userRepository;

    public DashboardResponse getGeneralStats() {
        DashboardResponse stats = new DashboardResponse();

        BigDecimal revenue = orderRepository.sumTotalRevenue();
        stats.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        stats.setTotalOrders(orderRepository.count());
        stats.setPendingOrders(orderRepository.countByStatus(OrderStatus.PENDING));
        stats.setTotalProducts(clothesRepository.countByIsDeletedFalse());
        stats.setTotalCustomers((int) userRepository.countCustomers());

        return stats;
    }

    private List<ChartDTO> mapToChartDTO(List<Object[]> results) {
        List<ChartDTO> chartData = new ArrayList<>();
        for (Object[] row : results) {
            String label = row[0].toString();
            BigDecimal value = new BigDecimal(row[1].toString());
            chartData.add(new ChartDTO(label, value));
        }
        return chartData;
    }

    public List<ChartDTO> getDailyRevenueChart(Integer year, Integer month) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        int targetMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        return mapToChartDTO(orderRepository.getRevenueByDay(targetYear, targetMonth));
    }

    public List<ChartDTO> getMonthlyRevenueChart(Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return mapToChartDTO(orderRepository.getRevenueByMonth(targetYear));
    }

    public List<ChartDTO> getYearlyRevenueChart() {
        return mapToChartDTO(orderRepository.getRevenueByYear());
    }

    public List<ChartDTO> getNewProductChartData(String type, Integer year, Integer month) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        int targetMonth = (month != null) ? month : LocalDate.now().getMonthValue();

        if ("yearly".equalsIgnoreCase(type)) {
            // Xem theo năm -> Lấy doanh thu của TẤT CẢ CÁC NĂM (Không truyền tham số)
            return mapToChartDTO(orderRepository.getNewProductRevenueByYear());
        }
        else if ("monthly".equalsIgnoreCase(type)) {
            // Xem theo tháng -> Lấy 12 tháng của NĂM CỤ THỂ (Chỉ truyền Năm)
            return mapToChartDTO(orderRepository.getNewProductRevenueByMonth(targetYear));
        }
        else {
            // Xem theo ngày -> Lấy các ngày của THÁNG CỤ THỂ (Truyền Năm và Tháng)
            return mapToChartDTO(orderRepository.getNewProductRevenueByDay(targetYear, targetMonth));
        }
    }
}