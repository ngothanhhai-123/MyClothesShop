package MyClothesShop.demo.controller;

import MyClothesShop.demo.dto.CreateClothesRequest;
import MyClothesShop.demo.entity.enums.Color;
import MyClothesShop.demo.entity.enums.Size;
import MyClothesShop.demo.dto.ClothesResponse;
import MyClothesShop.demo.service.ClothesService;
import MyClothesShop.demo.dto.UpdateClothesRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/clothes")
@RequiredArgsConstructor
public class ClothesController {

    private final ClothesService clothesService;

    @GetMapping
    public ResponseEntity<List<ClothesResponse>> getAllClothes() {
        return ResponseEntity.ok(clothesService.getAllActiveClothes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClothesById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(clothesService.getClothesById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search-and-filter")
    public ResponseEntity<List<ClothesResponse>> searchAndFilterClothes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<Color> colors,
            @RequestParam(required = false) List<Size> sizes
    ) {
        return ResponseEntity.ok(clothesService.searchAndFilterClothes(keyword, categoryId, minPrice, maxPrice, colors, sizes));
    }

    @PostMapping(value = "/admin/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createClothes(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            CreateClothesRequest request = mapper.readValue(dataJson, CreateClothesRequest.class);

            return ResponseEntity.ok(clothesService.createClothes(request, images));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }

    @PutMapping(value = "/admin/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateClothes(
            @PathVariable Integer id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            UpdateClothesRequest request = mapper.readValue(dataJson, UpdateClothesRequest.class);

            return ResponseEntity.ok(clothesService.updateClothes(id, request, images));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<?> deleteClothes(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(clothesService.deleteClothes(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}