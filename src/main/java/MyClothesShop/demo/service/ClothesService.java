package MyClothesShop.demo.service;

import MyClothesShop.demo.dto.*;
import MyClothesShop.demo.entity.*;
import MyClothesShop.demo.entity.enums.Color;
import MyClothesShop.demo.entity.enums.Size;
import MyClothesShop.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// BỔ SUNG 2 THƯ VIỆN NÀY ĐỂ XỬ LÝ CHỮ TIẾNG VIỆT
import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ClothesService {

    private final ClothesRepository clothesRepository;
    private final CategoryRepository categoryRepository;
    private final ClothesVariantRepository clothesVariantRepository;
    private final ClothesImageRepository clothesImageRepository;
    private final OrderRepository orderRepository;

    private String removeAccent(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        temp = pattern.matcher(temp).replaceAll("");
        return temp.replace("đ", "d").replace("Đ", "D").toLowerCase();
    }

    public List<ClothesResponse> getAllActiveClothes() {
        List<Clothes> clothesList = clothesRepository.findByIsDeletedFalse();
        return clothesList.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ClothesResponse getClothesById(Integer id) {
        Clothes clothes = clothesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        if (clothes.getIsDeleted()) throw new RuntimeException("Sản phẩm này đã ngừng kinh doanh!");
        return mapToResponse(clothes);
    }

    private ClothesResponse mapToResponse(Clothes clothes) {
        ClothesResponse response = new ClothesResponse();
        response.setClothesId(clothes.getClothesId());
        response.setName(clothes.getName());
        response.setDescription(clothes.getDescription());

        response.setCreatedAt(clothes.getCreatedAt());

        if (clothes.getCreatedAt() != null) {
            boolean isNew = clothes.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
            response.setIsNewProduct(isNew);
        }
        if (clothes.getCategory() != null) {
            response.setCategoryName(clothes.getCategory().getName());
            response.setCategoryId(clothes.getCategory().getCategoryId());
        }

        List<ClothesImage> images = clothesImageRepository.findByClothes_ClothesId(clothes.getClothesId());
        if (!images.isEmpty()) {
            response.setImageUrls(images.stream().map(ClothesImage::getImageUrl).collect(Collectors.toList()));
        }

        if (clothes.getVariants() != null) {
            List<VariantDTO> variantDTOs = clothes.getVariants().stream()
                    .filter(v -> !v.getIsDeleted())
                    .map(v -> {
                        VariantDTO dto = new VariantDTO();
                        dto.setVariantId(v.getVariantId());
                        dto.setColor(v.getColor());
                        dto.setSize(v.getSize());
                        dto.setPrice(v.getPrice());
                        dto.setStockQuantity(v.getStockQuantity());
                        return dto;
                    }).collect(Collectors.toList());
            response.setVariants(variantDTOs);

            if (!variantDTOs.isEmpty()) {
                response.setPrice(variantDTOs.get(0).getPrice());
            } else {
                response.setPrice(BigDecimal.ZERO);
            }
        }
        return response;
    }

    private String saveImageLocally(MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), path);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu ảnh: " + e.getMessage());
        }
    }

    @Transactional
    public String createClothes(CreateClothesRequest request, List<MultipartFile> images) {
        Clothes clothes = new Clothes();
        clothes.setName(request.getName());
        clothes.setDescription(request.getDescription());
        clothes.setIsDeleted(false);
        clothes.setPrice(request.getPrice());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
            clothes.setCategory(category);
        }

        Clothes savedClothes = clothesRepository.save(clothes);

        if (images != null && !images.isEmpty()) {
            boolean isFirst = true;
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    String fileName = saveImageLocally(file);
                    ClothesImage image = new ClothesImage(null, savedClothes, fileName, isFirst);
                    isFirst = false;
                    clothesImageRepository.save(image);
                }
            }
        }

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (var vReq : request.getVariants()) {
                ClothesVariant variant = new ClothesVariant();
                variant.setClothes(savedClothes);
                variant.setColor(vReq.getColor());
                variant.setSize(vReq.getSize());
                variant.setPrice(request.getPrice());
                variant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);
                variant.setIsDeleted(false);
                variant.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                clothesVariantRepository.save(variant);
            }
        }
        return "Thêm sản phẩm thành công!";
    }

    @Transactional
    public String updateClothes(Integer clothesId, UpdateClothesRequest request, List<MultipartFile> images) {
        Clothes clothes = clothesRepository.findById(clothesId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        if (request.getName() != null) clothes.setName(request.getName());
        if (request.getDescription() != null) clothes.setDescription(request.getDescription());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow();
            clothes.setCategory(category);
        }
        if (request.getPrice() != null) {
            clothes.setPrice(request.getPrice());
        }

        clothes = clothesRepository.save(clothes);

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ClothesVariant> existingVariants = clothes.getVariants();

            if (existingVariants != null) {
                for (ClothesVariant v : existingVariants) {
                    v.setIsDeleted(true);
                    clothesVariantRepository.save(v);
                }
            }

            for (var vReq : request.getVariants()) {
                ClothesVariant matchedVariant = null;

                if (existingVariants != null) {
                    for (ClothesVariant v : existingVariants) {
                        if (v.getColor().equals(vReq.getColor()) && v.getSize().equals(vReq.getSize())) {
                            matchedVariant = v;
                            break;
                        }
                    }
                }

                if (matchedVariant != null) {
                    matchedVariant.setStockQuantity(vReq.getStockQuantity());
                    matchedVariant.setPrice(request.getPrice() != null ? request.getPrice() : clothes.getPrice());
                    matchedVariant.setIsDeleted(false);
                    clothesVariantRepository.save(matchedVariant);
                } else {
                    ClothesVariant newVariant = new ClothesVariant();
                    newVariant.setClothes(clothes);
                    newVariant.setColor(vReq.getColor());
                    newVariant.setSize(vReq.getSize());
                    newVariant.setPrice(request.getPrice() != null ? request.getPrice() : clothes.getPrice());
                    newVariant.setStockQuantity(vReq.getStockQuantity() != null ? vReq.getStockQuantity() : 0);
                    newVariant.setIsDeleted(false);
                    newVariant.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    clothesVariantRepository.save(newVariant);
                }
            }
        } else if (request.getPrice() != null && clothes.getVariants() != null) {
            for (ClothesVariant v : clothes.getVariants()) {
                if (!v.getIsDeleted()) {
                    v.setPrice(request.getPrice());
                    clothesVariantRepository.save(v);
                }
            }
        }

        if (images != null && !images.isEmpty()) {
            List<ClothesImage> oldImages = clothesImageRepository.findByClothes_ClothesId(clothesId);
            clothesImageRepository.deleteAll(oldImages);

            boolean isFirst = true;
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    String fileName = saveImageLocally(file);
                    ClothesImage image = new ClothesImage(null, clothes, fileName, isFirst);
                    isFirst = false;
                    clothesImageRepository.save(image);
                }
            }
        }
        return "Cập nhật thành công!";
    }

    @Transactional
    public String deleteClothes(Integer id) {
        Clothes clothes = clothesRepository.findById(id).orElseThrow();
        clothes.setIsDeleted(true);
        if (clothes.getVariants() != null) {
            clothes.getVariants().forEach(v -> {
                v.setIsDeleted(true);
                clothesVariantRepository.save(v);
            });
        }
        clothesRepository.save(clothes);
        return "Đã xóa sản phẩm!";
    }

    // ==========================================
    // HÀM SEARCH ĐÃ ĐƯỢC NÂNG CẤP TÌM KHÔNG DẤU
    // ==========================================
    public List<ClothesResponse> searchAndFilterClothes(String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, List<Color> colors, List<Size> sizes) {

        // Cạo dấu từ khóa khách hàng nhập vào ("Ngô Thanh" -> "ngo thanh")
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? removeAccent(keyword.trim()) : null;

        return clothesRepository.findByIsDeletedFalse().stream()
                .filter(c -> {
                    // Lọc theo tên: Cạo dấu cả tên sản phẩm trong DB rồi mới so sánh
                    if (finalKeyword != null) {
                        String productNameNoAccent = removeAccent(c.getName());
                        if (!productNameNoAccent.contains(finalKeyword)) {
                            return false;
                        }
                    }

                    if (categoryId != null && (c.getCategory() == null || !c.getCategory().getCategoryId().equals(categoryId))) return false;
                    return c.getVariants().stream().anyMatch(v -> {
                        if (v.getIsDeleted()) return false;
                        if (minPrice != null && v.getPrice().compareTo(minPrice) < 0) return false;
                        if (maxPrice != null && v.getPrice().compareTo(maxPrice) > 0) return false;
                        if (colors != null && !colors.isEmpty() && !colors.contains(v.getColor())) return false;
                        if (sizes != null && !sizes.isEmpty() && !sizes.contains(v.getSize())) return false;
                        return true;
                    });
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TopProductDTO> getHotClothes() {
        return orderRepository.findAllHotProducts();
    }
}