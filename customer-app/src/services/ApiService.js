import SocketService from './SocketService';

const SERVER_IP = process.env.EXPO_PUBLIC_SOCKET_SERVER_IP || '192.168.1.1';
const ApiService = {
    getMenu: async () => {
        try {
            const response = await SocketService.request('GET_MENU');
            return response;
        } catch (error) {
            console.error('[ApiService] Error fetching menu:', error);
            throw error;
        }
    },

    getCategories: async () => {
        try {
            const response = await SocketService.request('GET_MENU_CATEGORIES');
            return response;
        } catch (error) {
            console.error('[ApiService] Error fetching categories:', error);
            throw error;
        }
    },

    getTables: async () => {
        try {
            const response = await SocketService.request('GET_TABLES_WITH_AVAILABILITY');
            return response;
        } catch (error) {
            console.error('[ApiService] Error fetching tables:', error);
            throw error;
        }
    },

    createBooking: async (bookingData) => {
        try {
            const response = await SocketService.request('CREATE_ORDER', bookingData);
            return response;
        } catch (error) {
            console.error('[ApiService] Error creating booking:', error);
            throw error;
        }
    },

    confirmBooking: async (confirmData) => {
        try {
            const response = await SocketService.request('CONFIRM_DEPOSIT', confirmData);
            return response;
        } catch (error) {
            console.error('[ApiService] Error confirming booking:', error);
            throw error;
        }
    },

    getNotifications: async (targetId) => {
        try {
            const response = await SocketService.request('GET_NOTIFICATIONS', { targetId });
            return response;
        } catch (error) {
            console.error('[ApiService] Error fetching notifications:', error);
            throw error;
        }
    },

    getInvoiceDetail: async (maHD) => {
        try {
            const response = await SocketService.request('GET_INVOICE_DETAIL', { maHD });
            return response;
        } catch (error) {
            console.error('[ApiService] Error fetching invoice detail:', error);
            throw error;
        }
    },

    updateInvoice: async (data) => {
        try {
            const response = await SocketService.request('UPDATE_INVOICE', data);
            return response;
        } catch (error) {
            console.error('[ApiService] Error updating invoice:', error);
            throw error;
        }
    },

    markNotificationAsRead: async (notificationId) => {
        try {
            const targetId = SocketService.userProfile?.soDT;
            const response = await SocketService.request('MARK_NOTIFICATION_READ', { targetId, notificationId });
            return response;
        } catch (error) {
            console.error('[ApiService] Error marking read:', error);
            throw error;
        }
    },

    deleteNotification: async (notificationId) => {
        try {
            const targetId = SocketService.userProfile?.soDT;
            const response = await SocketService.request('DELETE_NOTIFICATION', { targetId, notificationId });
            return response;
        } catch (error) {
            console.error('[ApiService] Error deleting notification:', error);
            throw error;
        }
    }
};

export default ApiService;
