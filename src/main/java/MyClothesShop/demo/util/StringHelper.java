package MyClothesShop.demo.util; // Sửa lại package cho chuẩn với project của sếp

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringHelper {

    // Hàm này sẽ lột sạch dấu Tiếng Việt, kể cả chữ Đ cứng đầu
    public static String removeAccent(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        // Chuẩn hóa Unicode
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        temp = pattern.matcher(temp).replaceAll("");

        // Xử lý riêng chữ Đ/đ và đưa tất cả về chữ thường
        return temp.replace("đ", "d").replace("Đ", "D").toLowerCase();
    }
}