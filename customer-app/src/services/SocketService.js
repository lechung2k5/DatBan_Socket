import AsyncStorage from '@react-native-async-storage/async-storage';
import { io } from 'socket.io-client';

const SERVER_IP = process.env.EXPO_PUBLIC_SOCKET_SERVER_IP || '192.168.1.1'; 
const WS_PORT = 8889;
const IO_PORT = 3001;
const WS_URL = `ws://${SERVER_IP}:${WS_PORT}`;
const IO_URL = `http://${SERVER_IP}:${IO_PORT}`;

class SocketService {
    constructor() {
        this.socket = null;
        this.io = null;
        this.listeners = new Set();
        this.token = null;
        this.userProfile = null;
        this.isLoaded = false;
    }

    async loadToken() {
        try {
            const [savedToken, savedProfile] = await Promise.all([
                AsyncStorage.getItem('userToken'),
                AsyncStorage.getItem('userProfile')
            ]);
            
            if (savedToken) {
                this.token = savedToken;
                console.log('[Socket] Đã nạp token từ bộ nhớ');
            }
            if (savedProfile) {
                this.userProfile = JSON.parse(savedProfile);
                console.log('[Socket] Đã nạp profile từ bộ nhớ');
            }
            this.isLoaded = true;
        } catch (e) {
            console.error('[Socket] Lỗi nạp dữ liệu:', e);
            this.isLoaded = true;
        }
    }

    async setToken(token, profile = null) {
        this.token = token;
        this.userProfile = profile;
        try {
            if (token) {
                await AsyncStorage.setItem('userToken', token);
            } else {
                await AsyncStorage.removeItem('userToken');
            }

            if (profile) {
                await AsyncStorage.setItem('userProfile', JSON.stringify(profile));
            } else {
                await AsyncStorage.removeItem('userProfile');
            }
        } catch (e) {
            console.error('[Socket] Lỗi lưu dữ liệu:', e);
        }
    }

    /**
     * Khởi tạo kết nối (Cả WS và IO)
     */
    connect() {
        // 1. Kết nối Java WebSocket (Legacy/Backup)
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
            console.log(`[Socket] Connecting to Java WS: ${WS_URL}`);
            this.socket = new WebSocket(WS_URL);
            this.socket.onmessage = (e) => {
                try {
                    const response = JSON.parse(e.data);
                    this.listeners.forEach(callback => callback(response));
                } catch (err) {}
            };
            this.socket.onclose = () => setTimeout(() => this.connect(), 10000);
        }

        // 2. Kết nối Node.js Socket.io (Chính cho Real-time)
        if (!this.io || !this.io.connected) {
            console.log(`[Socket.io] Connecting to Mobile Backend: ${IO_URL}`);
            this.io = io(IO_URL, {
                reconnection: true,
                reconnectionDelay: 5000
            });

            this.io.on('connect', () => {
                console.log('[Socket.io] Connected to Mobile Backend!');
                // Đăng ký phòng theo số điện thoại để nhận thông báo đích danh
                if (this.userProfile && this.userProfile.soDT) {
                    this.io.emit('register', this.userProfile.soDT);
                }
            });

            this.io.on('NEW_NOTIFICATION', (data) => {
                console.log('[Socket.io] New Notification received:', data.type || data.CommandType);
                // Phát tới tất cả listeners
                this.listeners.forEach(callback => callback(data));
            });

            this.io.on('disconnect', () => {
                console.log('[Socket.io] Disconnected');
            });
        }
    }

    /**
     * Gửi yêu cầu tới Server
     * @param {string} action CommandType (vd: 'SEND_OTP')
     * @param {object} params Tham số đi kèm
     * @param {string} token Token (nếu có - ưu tiên token truyền vào, nếu không dùng token đã lưu)
     */
    send(action, params = {}, token = null) {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
            console.warn('[Socket] Chưa kết nối, không thể gửi:', action);
            return false;
        }

        const request = {
            CommandType: action,
            params: params,
            token: token || this.token
        };

        this.socket.send(JSON.stringify(request));
        return true;
    }

    /**
     * Gửi yêu cầu và đợi phản hồi (Promise)
     */
    request(action, params = {}, token = null) {
        return new Promise((resolve, reject) => {
            if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
                return reject(new Error('Chưa kết nối tới Server'));
            }

            const requestId = Math.random().toString(36).substring(7);
            const request = {
                CommandType: action,
                params: params,
                token: token || this.token,
                requestId: requestId // Server cần trả lại requestId này
            };

            const timeout = setTimeout(() => {
                this.removeListener(handler);
                reject(new Error('Yêu cầu quá hạn (Timeout)'));
            }, 10000);

            const handler = (response) => {
                if (response.requestId === requestId || (response.action === action && !response.requestId)) {
                    clearTimeout(timeout);
                    this.removeListener(handler);
                    if (response.statusCode >= 200 && response.statusCode < 300) {
                        resolve(response);
                    } else {
                        reject(new Error(response.message || 'Lỗi Server'));
                    }
                }
            };

            this.addListener(handler);
            this.socket.send(JSON.stringify(request));
        });
    }

    /**
     * Đăng ký nhận phản hồi
     * Trả về callback để thuận tiện cho việc unsubscribe
     */
    addListener(callback) {
        this.listeners.add(callback);
        return callback; 
    }

    removeListener(callback) {
        if (callback) {
            this.listeners.delete(callback);
        }
    }

    /**
     * Đăng ký lắng nghe một CommandType cụ thể
     * Trả về hàm để hủy đăng ký (unsubscribe)
     */
    on(command, callback) {
        const handler = (response) => {
            if (response.CommandType === command) {
                callback(response.data);
            }
        };
        this.addListener(handler);
        return () => this.removeListener(handler);
    }
}

export default new SocketService();
