package MyClothesShop.demo.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import MyClothesShop.demo.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, Integer> {

    // Chỉ lấy những sản phẩm đang được bán (is_deleted = false)
    List<Clothes> findByIsDeletedFalse();
    // 1. Tìm kiếm áo theo tên (gõ chữ thường/chữ hoa đều tìm được)
    List<Clothes> findByNameContainingIgnoreCaseAndIsDeletedFalse(String keyword);

    // 2. Lọc danh sách áo theo ID của Danh mục (Ví dụ: Lọc riêng Áo Polo)
    List<Clothes> findByCategory_CategoryIdAndIsDeletedFalse(Integer categoryId);
    // Tìm áo thuộc Category hiện tại HOẶC thuộc các Category con của nó
    @Query("SELECT c FROM Clothes c WHERE (c.category.categoryId = :id OR c.category.parentCategory.categoryId = :id) AND c.isDeleted = false")
    List<Clothes> findByCategoryIdOrParentId(@Param("id") Integer id);
    // 3. Đếm tổng số lượng quần áo đang bán (isDeleted = false)
    long countByIsDeletedFalse();
    // Tìm kiếm tương đối (LIKE) trên cột searchName
    @Query("SELECT c FROM Clothes c WHERE c.isDeleted = false AND c.searchName LIKE %:keyword%")
    List<Clothes> searchClothesByName(@Param("keyword") String keyword);
}