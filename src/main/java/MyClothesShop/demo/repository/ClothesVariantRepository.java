package MyClothesShop.demo.repository;

import MyClothesShop.demo.entity.ClothesVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClothesVariantRepository extends JpaRepository<ClothesVariant, Integer> {
    // Thêm dòng này vào trong interface
    List<ClothesVariant> findByClothes_ClothesId(Integer clothesId);
}