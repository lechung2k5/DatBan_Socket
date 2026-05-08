import axios from 'axios';
import SocketService from './SocketService';

const SERVER_IP = process.env.EXPO_PUBLIC_SOCKET_SERVER_IP || '192.168.1.1';
const API_URL = `http://${SERVER_IP}:3001/api`;

const ApiService = {
    getMenu: async () => {
        try {
            const response = await axios.get(`${API_URL}/menu`);
            return response.data;
        } catch (error) {
            console.error('[ApiService] Error fetching menu:', error);
            throw error;
        }
    },

    getCategories: async () => {
        try {
            const response = await axios.get(`${API_URL}/categories`);
            return response.data;
        } catch (error) {
            console.error('[ApiService] Error fetching categories:', error);
            throw error;
        }
    },

    getTables: async () => {
        try {
            const response = await axios.get(`${API_URL}/tables`);
            return response.data;
        } catch (error) {
            console.error('[ApiService] Error fetching tables:', error);
            throw error;
        }
    },

    createBooking: async (bookingData) => {
        try {
            const response = await axios.post(`${API_URL}/bookings`, bookingData);
            return response.data;
        } catch (error) {
            console.error('[ApiService] Error creating booking:', error);
            throw error;
        }
    },

    confirmBooking: async (confirmData) => {
        try {
            const response = await axios.post(`${API_URL}/bookings/confirm`, confirmData);
            return response.data;
        } catch (error) {
            console.error('[ApiService] Error confirming booking:', error);
            throw error;
        }
    },

    getNotifications: async (targetId) => {
        try {
            const response = await axios.get(`${API_URL}/notifications/${targetId}`);
            return response.data;
        } catch (error) {
            console.error('[ApiService] Error fetching notifications:', error);
            throw error;
        }
    }
};

export default ApiService;
