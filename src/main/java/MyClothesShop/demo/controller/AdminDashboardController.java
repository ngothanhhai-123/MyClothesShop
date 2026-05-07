package MyClothesShop.demo.controller;

import MyClothesShop.demo.dto.ChartDTO;
import MyClothesShop.demo.dto.DashboardResponse;
import MyClothesShop.demo.dto.TopProductDTO;
import MyClothesShop.demo.repository.OrderRepository;
import MyClothesShop.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // Thư viện để bắt biến trên URL
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;
    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getGeneralStats());
    }

    @GetMapping("/chart/daily")
    public ResponseEntity<List<ChartDTO>> getDailyChart(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(dashboardService.getDailyRevenueChart(year, month));
    }

    @GetMapping("/chart/monthly")
    public ResponseEntity<List<ChartDTO>> getMonthlyChart(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(dashboardService.getMonthlyRevenueChart(year));
    }

    @GetMapping("/chart/yearly")
    public ResponseEntity<List<ChartDTO>> getYearlyChart() {
        return ResponseEntity.ok(dashboardService.getYearlyRevenueChart());
    }
    

    @GetMapping("/top-selling")
    public ResponseEntity<?> getTopSellingProducts(@RequestParam(defaultValue = "all") String time) {
        // Limit lấy top 5
        Pageable topFive = PageRequest.of(0, 5);
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        LocalDate today = LocalDate.now();

        if ("week".equalsIgnoreCase(time)) {
            startDate = today.with(DayOfWeek.MONDAY).atStartOfDay();
            endDate = startDate.plusWeeks(1);
        } else if ("month".equalsIgnoreCase(time)) {
            startDate = today.withDayOfMonth(1).atStartOfDay();
            endDate = startDate.plusMonths(1);
        }

        List<TopProductDTO> topProducts = orderRepository.findTopSellingProducts(startDate, endDate, topFive);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", topProducts
        ));
    }
}
