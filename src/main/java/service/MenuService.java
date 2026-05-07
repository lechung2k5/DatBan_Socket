package service;

import entity.MonAn;
import utils.CacheService;
import dao.MenuDAO;
import utils.JsonUtil;
import network.Request;
import network.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MenuService - Quản lý thực đơn nhà hàng
 */
public class MenuService {
    private final MenuDAO menuDAO = new MenuDAO();

    public Response handleGetAll(Request request) {
        try {
            String maDM = (String) request.getParam("maDM");
            String query = (String) request.getParam("query");
            List<MonAn> menus;

            // 1. Lấy dữ liệu (Ưu tiên Cache)
            String cachedMenu = CacheService.getMenu();
            if (cachedMenu != null) {
                menus = JsonUtil.fromJsonList(cachedMenu, MonAn.class);
            } else {
                menus = menuDAO.findAll();
                CacheService.setMenu(JsonUtil.toJson(menus));
            }

            // 2. Lọc dữ liệu theo yêu cầu (Nếu có)
            if (maDM != null || query != null) {
                if (maDM != null && !maDM.isEmpty()) {
                    menus = menus.stream()
                            .filter(m -> maDM.equals(m.getMaDM()))
                            .collect(Collectors.toList());
                }
                if (query != null && !query.isEmpty()) {
                    String finalQuery = query.toLowerCase();
                    menus = menus.stream()
                            .filter(m -> m.getTenMon().toLowerCase().contains(finalQuery))
                            .collect(Collectors.toList());
                }
            }
            return Response.ok(menus);
        } catch (Exception e) {
            System.err.println("[MenuService] Lỗi handleGetAll: " + e.getMessage());
            return Response.error("Lỗi khi lấy danh sách thực đơn: " + e.getMessage());
        }
    }

    public Response handleGetCategories(Request request) {
        try {
            return Response.ok(menuDAO.findAllCategories());
        } catch (Exception e) {
            return Response.error("Lỗi lấy danh mục: " + e.getMessage());
        }
    }

    public Response handleUpdate(Request request) {
        try {
            String type = (String) request.getParam("type"); // ADD, UPDATE, DELETE
            if ("ADD".equals(type)) {
                MonAn monAn = JsonUtil.convertValue(request.getParam("monAn"), MonAn.class);
                menuDAO.insert(monAn);
            } else if ("UPDATE".equals(type)) {
                MonAn monAn = JsonUtil.convertValue(request.getParam("monAn"), MonAn.class);
                menuDAO.update(monAn);
            } else if ("DELETE".equals(type)) {
                String catId = (String) request.getParam("maDM");
                String itemId = (String) request.getParam("maMon");
                menuDAO.delete(catId, itemId);
            } else {
                return Response.error("Loại thao tác không hợp lệ: " + type);
            }

            // Xóa cache thực đơn vì dữ liệu đã thay đổi
            CacheService.invalidateMenu();
            System.out.println("[MenuService] Đã cập nhật thực đơn, loại: " + type);
            return Response.ok("Thao tác thực đơn thành công");
        } catch (Exception e) {
            System.err.println("[MenuService] Lỗi handleUpdate: " + e.getMessage());
            return Response.error("Lỗi khi cập nhật thực đơn: " + e.getMessage());
        }
    }
}