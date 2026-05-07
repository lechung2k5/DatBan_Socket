package service;
import dao.StatsDAO;
import dao.TableDAO;
import dao.InvoiceDAO;
import network.Request;
import network.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
* StatsService - Quản lý báo cáo và thống kê doanh thu
*/
public class StatsService {
    private final StatsDAO statsDAO = new StatsDAO();
    private final TableDAO tableDAO = new TableDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final dao.ShiftDAO shiftDAO = new dao.ShiftDAO();
    public Response handleGetStats(Request request) {
        try {
            String dateStr = (String) request.getParam("date");
            LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();
            double revenue = statsDAO.getRevenueByDate(date);
            int count = statsDAO.countInvoicesByDate(date);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRevenue", revenue);
            stats.put("invoiceCount", count);
            return Response.ok(stats);
        } catch (Exception e) {
        System.err.println("[StatsService] Lỗi handleGetStats: " + e.getMessage());
        return Response.error("Lỗi thống kê: " + e.getMessage());
    }
}
public Response handleGetMonthlyRevenue(Request request) {
    try {
        Integer year = (Integer) request.getParam("year");
        Integer month = (Integer) request.getParam("month");
        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }
        double revenue = statsDAO.getRevenueByMonth(year, month);
        return Response.ok(revenue);
    } catch (Exception e) {
    return Response.error("Lỗi thống kê tháng: " + e.getMessage());
}
}
public Response handleGetDashboardStats(Request request) {
    try {
        String type = (String) request.getParam("type");
        if ("BOOKED_COUNT".equals(type)) {
            int count = invoiceDAO.findByStatus("Dat").size();
            return Response.ok(count);
        } else if ("TOTAL_TABLES".equals(type)) {
        int count = tableDAO.findAll().size();
        return Response.ok(count);
    }
    return Response.error("Loại thống kê không hợp lệ: " + type);
} catch (Exception e) {
return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleGetWeeklyShifts(Request request) {
    try {
        String startStr = (String) request.getParam("start");
        String maNV = (String) request.getParam("maNV");
        return Response.ok(shiftDAO.findWeeklyByEmployee(LocalDate.parse(startStr), maNV));
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleGetMonthlyShifts(Request request) {
    try {
        String maNV = (String) request.getParam("maNV");
        String startStr = (String) request.getParam("start");
        String endStr = (String) request.getParam("end");
        return Response.ok(shiftDAO.findMonthlyByEmployee(maNV, LocalDate.parse(startStr), LocalDate.parse(endStr)));
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleGetCashStats(Request request) {
    try {
        String maNV = (String) request.getParam("maNV");
        String dateStr = (String) request.getParam("date");
        double amount = statsDAO.getCashRevenueByEmployee(maNV, LocalDate.parse(dateStr));
        return Response.ok(amount);
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleGetEmployeeKpi(Request request) {
    try {
        String maNV = (String) request.getParam("maNV");
        String startStr = (String) request.getParam("start");
        String endStr = (String) request.getParam("end");
        LocalDate start = LocalDate.parse(startStr);
        LocalDate end = LocalDate.parse(endStr);
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("revenue", statsDAO.getEmployeeRevenue(maNV, start, end));
        kpi.put("scheduledHours", shiftDAO.getScheduledHours(maNV, start, end));
        return Response.ok(kpi);
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleUpdateShift(Request request) {
    try {
        entity.CaTruc ca = utils.JsonUtil.convertValue(request.getParam("shift"), entity.CaTruc.class);
        shiftDAO.upsert(ca);
        return Response.ok("Cập nhật ca trực thành công");
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleDeleteShift(Request request) {
    try {
        String maCa = (String) request.getParam("maCa");
        shiftDAO.delete(maCa);
        return Response.ok("Xóa ca trực thành công");
    } catch (Exception e) {
    return Response.error("Lỗi: " + e.getMessage());
}
}
public Response handleGetWeeklyShiftsAll(Request request) {
    try {
        String startStr = (String) request.getParam("start");
        return Response.ok(shiftDAO.findAllByWeek(LocalDate.parse(startStr)));
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleGetKpisForDate(Request request) {
    try {
        String dateStr = (String) request.getParam("date");
        LocalDate date = LocalDate.parse(dateStr);
        double revenue = statsDAO.getRevenueByDate(date);
        int count = statsDAO.countInvoicesByDate(date);
        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalRevenue", revenue);
        kpis.put("totalInvoices", count);
        return Response.ok(kpis);
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleGetInvoiceStats(Request request) {
    try {
        Integer day = (Integer) request.getParam("day");
        Integer month = (Integer) request.getParam("month");
        Integer year = (Integer) request.getParam("year");
        List<Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> items;
        if (day != null && month != null) {
            items = statsDAO.getRawInvoicesByDate(LocalDate.of(year, month, day));
        } else if (month != null) {
        items = statsDAO.getRawInvoicesByMonth(year, month);
    } else {
    // Approximate for year if needed, or just scan
    items = new ArrayList<>(); // Simplify for now
}
List<Map<String, Object>> list = items.stream().map(item -> {
    Map<String, Object> m = new HashMap<>();
    m.put("maHD", item.get("id").s());
    m.put("thoiGianVao", item.get("createdAt").s());
    m.put("tongTien", Double.parseDouble(item.get("total").n()));
    return m;
}).collect(java.util.stream.Collectors.toList());
double totalRev = list.stream().mapToDouble(m -> (double) m.get("tongTien")).sum();
Map<String, Object> res = new HashMap<>();
res.put("list", list);
res.put("totalInvoices", list.size());
res.put("totalRevenue", totalRev);
return Response.ok(res);
} catch (Exception e) {
return Response.error(e.getMessage());
}
}
public Response handleGetDailyRevenueForWeek(Request request) {
    try {
        LocalDate start = LocalDate.parse((String) request.getParam("start"));
        LocalDate end = LocalDate.parse((String) request.getParam("end"));
        List<Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> invoices = statsDAO.getRawInvoicesByRange(start, end);
        Map<String, Double> daily = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            daily.put(start.plusDays(i).toString(), 0.0);
        }
        for (Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> inv : invoices) {
            String d = inv.get("createdAt").s().substring(0, 10);
            daily.put(d, daily.getOrDefault(d, 0.0) + Double.parseDouble(inv.get("total").n()));
        }
        return Response.ok(daily);
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleGetZoneRevenueForWeek(Request request) {
    try {
        LocalDate start = LocalDate.parse((String) request.getParam("start"));
        LocalDate end = LocalDate.parse((String) request.getParam("end"));
        List<Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> invoices = statsDAO.getRawInvoicesByRange(start, end);
        // Map<Zone, Map<DateStr, Amount>>
        Map<String, Map<String, Double>> zoneData = new HashMap<>();
        String[] zones = {"Tầng trệt", "Tầng 1", "Phòng"};
        for (String z : zones) {
            Map<String, Double> dMap = new HashMap<>();
            for (int i = 0; i < 7; i++) dMap.put(start.plusDays(i).toString(), 0.0);
            zoneData.put(z, dMap);
        }
        for (Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> inv : invoices) {
            String d = inv.get("createdAt").s().substring(0, 10);
            String z = inv.containsKey("zone") ? inv.get("zone").s() : "Khác";
            if (zoneData.containsKey(z)) {
                zoneData.get(z).put(d, zoneData.get(z).get(d) + Double.parseDouble(inv.get("total").n()));
            }
        }
        return Response.ok(zoneData);
    } catch (Exception e) {
    return Response.error(e.getMessage());
}
}
public Response handleGetTopSellingItems(Request request) {
    try {
        Integer day = (Integer) request.getParam("day");
        Integer month = (Integer) request.getParam("month");
        Integer year = (Integer) request.getParam("year");
        List<Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> invoices;
        if (day != null && month != null) {
            invoices = statsDAO.getRawInvoicesByDate(LocalDate.of(year, month, day));
        } else if (month != null) {
        invoices = statsDAO.getRawInvoicesByMonth(year, month);
    } else {
    invoices = new ArrayList<>();
}
// Map<ItemName, Quantity>
Map<String, Integer> itemCounts = new HashMap<>();
Map<String, Double> itemRevenue = new HashMap<>();
for (Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> inv : invoices) {
    if (inv.containsKey("details")) {
        List<software.amazon.awssdk.services.dynamodb.model.AttributeValue> details = inv.get("details").l();
        for (software.amazon.awssdk.services.dynamodb.model.AttributeValue detail : details) {
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> dMap = detail.m();
            String name = dMap.get("productName").s();
            int qty = Integer.parseInt(dMap.get("quantity").n());
            double price = Double.parseDouble(dMap.get("price").n());
            itemCounts.put(name, itemCounts.getOrDefault(name, 0) + qty);
            itemRevenue.put(name, itemRevenue.getOrDefault(name, 0.0) + (qty * price));
        }
    }
}
List<Map<String, Object>> result = new ArrayList<>();
int stt = 1;
List<String> sortedNames = itemCounts.entrySet().stream()
.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
.map(Map.Entry::getKey)
.collect(java.util.stream.Collectors.toList());
for (String name : sortedNames) {
    Map<String, Object> m = new HashMap<>();
    m.put("stt", stt++);
    m.put("tenMon", name);
    m.put("soLuongBan", itemCounts.get(name));
    m.put("doanhThu", itemRevenue.get(name));
    result.add(m);
}
return Response.ok(result);
} catch (Exception e) {
return Response.error(e.getMessage());
}
}
public Response handleGetTableCounts(Request request) {
    try {
        List<entity.Ban> tables = tableDAO.findAll();
        Map<String, Integer> counts = new HashMap<>();
        counts.put("TRONG", 0);
        counts.put("DANG_SU_DUNG", 0);
        counts.put("DA_DAT", 0);
        for (entity.Ban b : tables) {
            String status = (b.getTrangThai() != null) ? b.getTrangThai().name() : "TRONG";
            counts.put(status, counts.getOrDefault(status, 0) + 1);
        }
        return Response.ok(counts);
    } catch (Exception e) {
        return Response.error("Lỗi đếm số lượng bàn: " + e.getMessage());
    }
}
}