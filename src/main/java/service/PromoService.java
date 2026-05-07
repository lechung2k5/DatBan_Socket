package service;
import entity.UuDai;
import utils.CacheService;
import dao.PromoDAO;
import utils.JsonUtil;
import network.Request;
import network.Response;
import java.util.List;
/**
* PromoService - Quản lý các chương trình ưu đãi
*/
public class PromoService {
    private final PromoDAO promoDAO = new PromoDAO();
    public Response handleGetAll(Request request) {
        try {
            // Check cache (Sử dụng số nhiều 'Promos' theo CacheService)
            String cached = CacheService.getPromos();
            if (cached != null) {
                return Response.ok(JsonUtil.fromJsonList(cached, UuDai.class));
            }
            List<UuDai> list = promoDAO.findAll();
            CacheService.setPromos(JsonUtil.toJson(list));
            return Response.ok(list);
        } catch (Exception e) {
        return Response.error("Lỗi lấy ưu đãi: " + e.getMessage());
    }
}
public Response handleUpdate(Request request) {
    try {
        UuDai ud = JsonUtil.convertValue(request.getParam("promo"), UuDai.class);
        promoDAO.insert(ud);
        CacheService.invalidatePromos(); // Xóa cache
        network.Service.broadcast(new network.RealTimeEvent(network.CommandType.UPDATE_PROMO, "[PROMO]:" + ud.getMaUuDai()));
        return Response.ok("Thao tác ưu đãi thành công");
    } catch (Exception e) {
    return Response.error("Lỗi cập nhật ưu đãi: " + e.getMessage());
}
}
public Response handleGetById(Request request) {
    try {
        String id = (String) request.getParam("id");
        UuDai ud = promoDAO.findById(id);
        return Response.ok(ud);
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleDelete(Request request) {
    try {
        String id = (String) request.getParam("id");
        promoDAO.delete(id);
        CacheService.invalidatePromos();
        network.Service.broadcast(new network.RealTimeEvent(network.CommandType.UPDATE_PROMO, "[PROMO]:" + id));
        return Response.ok("Xóa ưu đãi thành công");
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
}