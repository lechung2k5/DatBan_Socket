package network;

import utils.ServerSessionService;
import service.*;

/**
 * RequestDispatcher - Điều phối Request đến đúng Handler dựa trên CommandType
 */
public class RequestDispatcher {
    private final AuthService AuthService = new AuthService();
    private final TableService TableService = new TableService();
    private final MenuService MenuService = new MenuService();
    private final OrderService OrderService = new OrderService();
    private final PaymentService PaymentService = new PaymentService();
    private final CustomerService CustomerService = new CustomerService();
    private final StatsService StatsService = new StatsService();
    private final PromoService PromoService = new PromoService();
    private final EmployeeService EmployeeService = new EmployeeService();

    public Response dispatch(Request request) {
        CommandType action = request.getAction();
        if (action == null)
            return Response.error("Hành động không hợp lệ");
        // 1. Ngoại lệ: LOGIN, SEND_OTP, REGISTER_CUSTOMER không cần kiểm tra token
        if (action == CommandType.LOGIN ||
                action == CommandType.FORGOT_PASSWORD_UPDATE ||
                action == CommandType.SEND_OTP ||
                action == CommandType.REGISTER_CUSTOMER ||
                action == CommandType.CUSTOMER_LOGIN ||
                action == CommandType.GET_MENU ||
                action == CommandType.GET_MENU_CATEGORIES) {

            if (action == CommandType.LOGIN)
                return AuthService.handleLogin(request);
            if (action == CommandType.FORGOT_PASSWORD_UPDATE)
                return AuthService.handleForgotPasswordUpdate(request);
            if (action == CommandType.SEND_OTP)
                return AuthService.handleSendOTP(request);
            if (action == CommandType.REGISTER_CUSTOMER)
                return AuthService.handleRegisterCustomer(request);
            if (action == CommandType.CUSTOMER_LOGIN)
                return AuthService.handleCustomerLogin(request);
            if (action == CommandType.GET_MENU)
                return MenuService.handleGetAll(request);
            if (action == CommandType.GET_MENU_CATEGORIES)
                return MenuService.handleGetCategories(request);
        }
        // 2. Kiểm tra Session (Token) trong Redis
        String token = request.getToken();
        if (!ServerSessionService.isValid(token)) {
            System.err.println("[Dispatcher] Token không hợp lệ hoặc đã hết hạn: " + token);
            return new Response(401, "Phiên làm việc hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.", null);
        }
        // 3. Phân luồng xử lý theo CommandType
        switch (action) {
            case LOGOUT:
                return AuthService.handleLogout(request);
            case CHANGE_PASSWORD:
                return AuthService.handleChangePassword(request);
            case GET_TABLES:
                return TableService.getAllTables();
            case UPDATE_TABLE_STATUS:
                return TableService.updateStatus(request);
            case UPDATE_MANY_TABLES:
                return TableService.updateStatus(request); // Tái sử dụng logic update đơn lẻ
            case GET_MENU:
                return MenuService.handleGetAll(request);
            case GET_MENU_CATEGORIES:
                return MenuService.handleGetCategories(request);
            case UPDATE_MENU:
                return MenuService.handleUpdate(request);
            case CREATE_ORDER:
                return OrderService.handleCreateOrder(request);
            case GET_INVOICE_BY_ID:
                return OrderService.handleGetInvoice(request);
            case GET_INVOICES_TODAY:
                return OrderService.handleGetInvoicesToday(request);
            case GET_INVOICES_PENDING:
                return OrderService.handleGetInvoicesPending(request);
            case GET_ACTIVE_INVOICES:
                return OrderService.handleGetActiveInvoices(request);
            case UPDATE_INVOICE_PROMO:
                return OrderService.handleUpdateInvoicePromo(request);
            case UPDATE_INVOICE:
                return OrderService.handleUpdateInvoice(request);
            case CANCEL_INVOICE:
                return OrderService.handleCancelInvoice(request);
            case CHECK_OUT:
                return PaymentService.handleCheckout(request);
            case GET_STATS:
                return StatsService.handleGetStats(request);
            case GET_DASHBOARD_STATS:
                return StatsService.handleGetDashboardStats(request);
            case GET_REVENUE_BY_MONTH:
                return StatsService.handleGetMonthlyRevenue(request);
            case GET_CASH_STATS:
                return StatsService.handleGetCashStats(request);
            case GET_WEEKLY_SHIFTS:
                return StatsService.handleGetWeeklyShifts(request);
            case GET_MONTHLY_SHIFTS:
                return StatsService.handleGetMonthlyShifts(request);
            case GET_EMPLOYEE_KPI:
                return StatsService.handleGetEmployeeKpi(request);
            case UPDATE_SHIFT:
                return StatsService.handleUpdateShift(request);
            case DELETE_SHIFT:
                return StatsService.handleDeleteShift(request);
            case GET_WEEKLY_SHIFTS_ALL:
                return StatsService.handleGetWeeklyShiftsAll(request);
            case GET_KPIS_FOR_DATE:
                return StatsService.handleGetKpisForDate(request);
            case GET_INVOICE_STATS:
                return StatsService.handleGetInvoiceStats(request);
            case GET_DAILY_REVENUE_FOR_WEEK:
                return StatsService.handleGetDailyRevenueForWeek(request);
            case GET_ZONE_REVENUE_FOR_WEEK:
                return StatsService.handleGetZoneRevenueForWeek(request);
            case GET_TOP_SELLING_ITEMS:
                return StatsService.handleGetTopSellingItems(request);
            case GET_PROMOS:
                return PromoService.handleGetAll(request);
            case GET_TABLE_COUNTS:
                return StatsService.handleGetTableCounts(request);
            case UPDATE_PROMO:
                return PromoService.handleUpdate(request);
            case DELETE_PROMO:
                return PromoService.handleDelete(request);
            case GET_PROMO_BY_ID:
                return PromoService.handleGetById(request);
            case GET_EMPLOYEES:
                return EmployeeService.handleGetAll(request);
            case UPDATE_EMPLOYEE:
                return EmployeeService.handleUpdate(request);
            case FIND_CUSTOMER_BY_PHONE:
                return CustomerService.handleFindByPhone(request);
            case TIM_HOAC_TAO_KH:
                return CustomerService.handleCreate(request);
            case GET_CUSTOMERS:
                return CustomerService.handleGetAll(request);
            case UPDATE_CUSTOMER:
                return CustomerService.handleUpdate(request);
            case DELETE_CUSTOMER:
                return CustomerService.handleDelete(request);
            case GET_INVOICES_BY_CUSTOMER:
                return OrderService.handleGetInvoicesByCustomer(request);
            case GET_INVOICE_DETAILS:
                return OrderService.handleGetInvoiceDetails(request);
            case MERGE_INVOICES:
                return OrderService.handleMergeInvoices(request);
            case CLEANUP_MERGED:
                return OrderService.handleCleanupMerged(request);
            case SPLIT_INVOICE:
                return OrderService.handleSplitInvoice(request);
            case TRANSFER_TABLE:
                return OrderService.handleUpdateInvoice(request);
            case GET_INVOICES_ALL:
                return OrderService.handleGetInvoicesAll(request);
            case GET_SUB_INVOICES:
                return OrderService.handleGetSubInvoices(request);
            // UI Aliases
            case GET_PENDING_INVOICES:
                return OrderService.handleGetInvoicesPending(request);
            case GET_INVOICE:
                return OrderService.handleGetInvoice(request);
            case CHECKOUT:
                return PaymentService.handleCheckout(request);
            case GET_TABLES_WITH_AVAILABILITY:
                return TableService.handleGetTablesWithAvailability(request);
            case GET_INVOICES_BY_DATE:
                return OrderService.handleGetInvoicesToday(request);
            case CONFIRM_DEPOSIT:
                return PaymentService.handleConfirmDeposit(request);
            case GENERATE_INVOICE_ID:
                return OrderService.handleGenerateId(request);
            case LOCK_TABLES:
                return OrderService.handleLockTables(request);
            case UNLOCK_TABLES:
                return OrderService.handleUnlockTables(request);
            case SEND_OTP:
                return AuthService.handleSendOTP(request);
            case REGISTER_CUSTOMER:
                return AuthService.handleRegisterCustomer(request);
            case GET_NOTIFICATIONS:
                return NotificationService.handleGetNotifications(request);
            case DELETE_INVOICE:
                return OrderService.handleDeleteInvoice(request);
            case GET_INVOICE_DETAIL:
                return OrderService.handleGetInvoiceDetail(request);
            case MARK_NOTIFICATION_READ:
                return NotificationService.handleMarkAsRead(request);
            case DELETE_NOTIFICATION:
                return NotificationService.handleDeleteNotification(request);
            case GET_USER_PROFILE:
                return CustomerService.handleFindByPhone(request);
            default:
                System.out.println("[Dispatcher] CommandType chưa được xử lý: " + action);
                return Response.error("Chức năng [" + action + "] chưa được hỗ trợ trên Server");
        }
    }
}