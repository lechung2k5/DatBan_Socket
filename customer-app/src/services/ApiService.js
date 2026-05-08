import SocketService from './SocketService';

const SERVER_IP = process.env.EXPO_PUBLIC_SOCKET_SERVER_IP || '192.168.1.1';
const ApiService = {
    getMenu: async () => {
        try {
            const API_URL = `http://${SERVER_IP}:3001/api/menu`;
            const response = await fetch(API_URL);
            return await response.json();
        } catch (error) {
            console.error('[ApiService] Error fetching menu:', error);
            throw error;
        }
    },

    getCategories: async () => {
        try {
            const API_URL = `http://${SERVER_IP}:3001/api/categories`;
            const response = await fetch(API_URL);
            return await response.json();
        } catch (error) {
            console.error('[ApiService] Error fetching categories:', error);
            throw error;
        }
    },

    getTables: async () => {
        try {
            const API_URL = `http://${SERVER_IP}:3001/api/tables`;
            const response = await fetch(API_URL);
            return await response.json();
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
            const API_URL = `http://${SERVER_IP}:3001/api/notifications/${targetId}`;
            const response = await fetch(API_URL);
            const data = await response.json();
            return data;
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
    },

    getInvoicesByCustomer: async (maKH) => {
        try {
            const response = await SocketService.request('GET_INVOICES_BY_CUSTOMER', { maKH });
            return response;
        } catch (error) {
            console.error('[ApiService] Error fetching customer invoices:', error);
            throw error;
        }
    },

    getUserProfile: async (phone) => {
        try {
            const response = await SocketService.request('GET_USER_PROFILE', { phone });
            return response;
        } catch (error) {
            console.error('[ApiService] Error fetching user profile:', error);
            throw error;
        }
    }
};

export default ApiService;
