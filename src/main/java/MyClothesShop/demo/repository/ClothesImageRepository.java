package MyClothesShop.demo.repository;

import MyClothesShop.demo.entity.ClothesImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClothesImageRepository extends JpaRepository<ClothesImage, Integer> {
    // Lấy danh sách ảnh của 1 sản phẩm
    List<ClothesImage> findByClothes_ClothesId(Integer clothesId);

}