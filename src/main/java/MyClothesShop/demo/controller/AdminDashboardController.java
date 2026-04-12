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

    @GetMapping("/chart-new/{type}")
    public ResponseEntity<List<ChartDTO>> getNewProductChart(
            @PathVariable String type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        List<ChartDTO> data = dashboardService.getNewProductChartData(type, year, month);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/top-selling")
    public ResponseEntity<?> getTopSellingProducts() {
        // Limit lấy top 5
        Pageable topFive = PageRequest.of(0, 5);
        List<TopProductDTO> topProducts = orderRepository.findTopSellingProducts(topFive);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", topProducts
        ));
    }
}