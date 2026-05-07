package service;
import entity.Ban;
import utils.CacheService;
import dao.TableDAO;
import utils.JsonUtil;
import network.Request;
import network.Response;
import network.Service;
import network.RealTimeEvent;
import network.CommandType;
import java.util.List;
/**
* TableService - QuáÂºÂ£n lÃÂ½ tráÂºÂ¡ng thÃÂ¡i bÃÂ n ÃÆn vÃÂ  sÃÂ¡ ÃâáÂ»â phÃÂ²ng
* TÃÂ­ch háÂ»Â£p Redis Cache ÃâáÂ»Æ tÃÆng táÂ»âc ÃâáÂ»â¢ láÂºÂ¥y danh sÃÂ¡ch bÃÂ n.
*/
public class TableService {
    private final TableDAO tableDAO = new TableDAO();
    public Response getAllTables() {
        try {
            // 1. KiáÂ»Æm tra Redis Cache trÃÂ°áÂ»âºc
            String cachedTables = CacheService.getTables();
            if (cachedTables != null) {
                List<Ban> bans = JsonUtil.fromJsonList(cachedTables, Ban.class);
                return Response.ok(bans);
            }
            // 2. NáÂºÂ¿u miss, láÂºÂ¥y táÂ»Â« DynamoDB
            List<Ban> tables = tableDAO.findAll();
            // 3. CáÂºÂ­p nháÂºÂ­t Cache (TTL thÃÂ°áÂ»Âng lÃÂ  30s-1m cho bÃÂ n)
            CacheService.setTables(JsonUtil.toJson(tables));
            return Response.ok(tables);
        } catch (Exception e) {
        System.err.println("[TableService] LáÂ»âi getAllTables: " + e.getMessage());
        return Response.error("LáÂ»âi khi láÂºÂ¥y danh sÃÂ¡ch bÃÂ n: " + e.getMessage());
    }
}
    public Response handleGetTablesWithAvailability(Request request) {
        return getAllTables();
    }
    public Response updateStatus(Request request) {
    try {
        Object maBanObj = request.getParam("maBan");
        String newStatus = (String) request.getParam("newStatus");
        if (newStatus == null) {
            return Response.error("ThiáÂºÂ¿u tham sáÂ»â newStatus");
        }
        if (maBanObj instanceof List) {
            List<String> maBans = (List<String>) maBanObj;
            for (String mb : maBans) {
                tableDAO.updateStatus(mb, newStatus);
            }
        } else if (maBanObj instanceof String) {
        tableDAO.updateStatus((String) maBanObj, newStatus);
    } else {
    return Response.error("ThiáÂºÂ¿u tham sáÂ»â maBan");
}
// Quan tráÂ»Âng: Invalidate cache ÃâáÂ»Æ client khÃÂ¡c tháÂºÂ¥y sáÂ»Â± thay ÃâáÂ»â¢i ngay láÂºÂ­p táÂ»Â©c


        // Quan trọng: Invalidate cache để client khác thấy sự thay đổi ngay lập tức
        CacheService.invalidateTables();
        
        // 🔥 Broadcast sự kiện cập nhật bàn tới tất cả client
        Service.broadcast(new RealTimeEvent(CommandType.UPDATE_TABLE_STATUS, "Cập nhật trạng thái bàn"));
        
        return Response.ok("Cập nhật trạng thái bàn thành công");
} catch (Exception e) {
System.err.println("[TableService] LáÂ»âi updateStatus: " + e.getMessage());
return Response.error("LáÂ»âi cáÂºÂ­p nháÂºÂ­t bÃ n: " + e.getMessage());
}
}
}