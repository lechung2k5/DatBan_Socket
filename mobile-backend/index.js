const express = require('express');
require('dotenv').config();
const cors = require('cors');
const db = require('./db');
const { v4: uuidv4 } = require('uuid');
const http = require('http');
const { Server } = require('socket.io');
const Redis = require('ioredis');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

const redis = new Redis({
    host: process.env.REDIS_HOST || '127.0.0.1',
    port: process.env.REDIS_PORT || 6379,
    password: process.env.REDIS_PASSWORD || undefined
});

const redisSub = new Redis({
    host: process.env.REDIS_HOST || '127.0.0.1',
    port: process.env.REDIS_PORT || 6379,
    password: process.env.REDIS_PASSWORD || undefined
});

const PORT = process.env.MOBILE_BACKEND_PORT || 3001;

// --- SOCKET.IO LOGIC ---
io.on('connection', (socket) => {
    console.log(`[Socket.io] Client connected: ${socket.id}`);
    
    socket.on('register', (targetId) => {
        socket.join(targetId);
        console.log(`[Socket.io] Client registered for: ${targetId}`);
    });

    socket.on('disconnect', () => {
        console.log(`[Socket.io] Client disconnected: ${socket.id}`);
    });
});

// --- REDIS PUB/SUB LOGIC ---
redisSub.subscribe('notifications', (err, count) => {
    if (err) {
        console.error('[Redis] Failed to subscribe:', err.message);
    } else {
        console.log(`[Redis] Subscribed to ${count} channels. Listening for notifications...`);
    }
});

redisSub.on('message', (channel, message) => {
    if (channel === 'notifications') {
        try {
            const event = JSON.parse(message);
            console.log(`[Redis] Received notification for: ${event.affectedId || 'all'}`);
            
            // Emit to specific user room if targetId exists
            const targetId = event.params ? event.params.targetId : event.affectedId;
            if (targetId) {
                io.to(targetId).emit('NEW_NOTIFICATION', event);
            } else {
                io.emit('NEW_NOTIFICATION', event);
            }
        } catch (err) {
            console.error('[Redis] Message parse error:', err);
        }
    }
});

// Helper: Generate Invoice ID like Java (HDddMMyyxxxx)
async function generateInvoiceId() {
// ... (rest of helper)
    const now = new Date();
    const datePart = now.getDate().toString().padStart(2, '0') + 
                     (now.getMonth() + 1).toString().padStart(2, '0') + 
                     now.getFullYear().toString().slice(-2);
    
    // Count invoices today to get sequence
    const startOfDay = new Date();
    startOfDay.setHours(0,0,0,0);
    
    const data = await db.scan({
        TableName: 'Invoices',
        FilterExpression: 'createdAt >= :start',
        ExpressionAttributeValues: { ':start': startOfDay.toISOString() }
    });
    
    const nextSeq = (data.Items.length + 1).toString().padStart(4, '0');
    return `HD${datePart}${nextSeq}`;
}

// 1. Menu APIs
app.get('/api/menu', async (req, res) => {
    try {
        const data = await db.scan({ TableName: 'MenuItems' });
        // Map to match the mobile app's expectation
        const items = data.Items.map(item => ({
            maMon: item.itemId,
            tenMon: item.name,
            donGia: item.price,
            giaBan: item.price, // Keep for backward compatibility
            maDM: item.categoryId,
            hinhAnh: item.imageUrl,
            available: item.available
        }));
        res.json({ statusCode: 200, data: items });
    } catch (err) {
        res.status(500).json({ statusCode: 500, message: err.message });
    }
});

app.get('/api/categories', async (req, res) => {
    try {
        const data = await db.scan({ TableName: 'MenuItems', ProjectionExpression: 'categoryId' });
        const ids = [...new Set(data.Items.map(i => i.categoryId))];
        const categories = ids.map(id => {
            let name = id;
            switch(id) {
                case "CAT001": name = "Khai vị & Gỏi"; break;
                case "CAT002": name = "Đặc sản Đồng Quê"; break;
                case "CAT003": name = "Món Nhậu Lai Rai"; break;
                case "CAT004": name = "Lẩu & Món Chính"; break;
                case "CAT005": name = "Đồ uống"; break;
            }
            return { maDM: id, tenDM: name };
        });
        res.json({ statusCode: 200, data: categories });
    } catch (err) {
        res.status(500).json({ statusCode: 500, message: err.message });
    }
});

// 2. Table APIs
app.get('/api/tables', async (req, res) => {
    try {
        const data = await db.scan({ TableName: 'Tables' });
        res.json({ statusCode: 200, data: data.Items });
    } catch (err) {
        res.status(500).json({ statusCode: 500, message: err.message });
    }
});

// 3. Booking APIs
app.post('/api/bookings', async (req, res) => {
    try {
        const booking = req.body; 
        const bookingId = await generateInvoiceId(); 
        
        const invoice = {
            invoiceId: bookingId,
            customerPhone: booking.customerPhone || booking.customerId,
            customerName: booking.customerName || 'Khách Mobile',
            tableId: booking.maBan,
            createdAt: new Date().toISOString(),
            gioVao: `${booking.ngayDat}T${booking.gioDat}:00`,
            status: 'ChoXacNhan',
            tienCoc: booking.tienCoc || 0,
            ghiChu: booking.ghiChu,
            soKhach: booking.soKhach,
            loaiHD: 'DatTruoc',
            itemsJson: booking.items ? JSON.stringify(booking.items) : '[]',
            total: booking.total || 0,
            updatedAt: new Date().toISOString()
        };

        await db.put({ TableName: 'Invoices', Item: invoice });
        
        console.log(`[Booking Created] ID: ${bookingId} - Status: ChoXacNhan`);
        res.json({ statusCode: 200, message: 'Đã tạo yêu cầu đặt bàn', data: invoice });
    } catch (err) {
        console.error('[Booking Error]', err);
        res.status(500).json({ statusCode: 500, message: err.message });
    }
});

// 4. Confirm Payment API
app.post('/api/bookings/confirm', async (req, res) => {
    try {
        const { invoiceId, maBan } = req.body;
        
        // 1. Cập nhật hóa đơn sang trạng thái "Dat"
        await db.update({
            TableName: 'Invoices',
            Key: { invoiceId: invoiceId },
            UpdateExpression: 'SET #s = :status, updatedAt = :ts',
            ExpressionAttributeNames: { '#s': 'status' },
            ExpressionAttributeValues: {
                ':status': 'Dat',
                ':ts': new Date().toISOString()
            }
        });

        // 2. Cập nhật bàn sang trạng thái "DaDat"
        if (maBan) {
            await db.update({
                TableName: 'Tables',
                Key: { maBan: maBan },
                UpdateExpression: 'SET trangThai = :status, updatedAt = :ts',
                ExpressionAttributeValues: {
                    ':status': 'DaDat',
                    ':ts': new Date().toISOString()
                }
            });
        }

        console.log(`[Booking Confirmed] ID: ${invoiceId} for Table: ${maBan}`);
        res.json({ statusCode: 200, message: 'Xác nhận đặt bàn thành công' });
    } catch (err) {
        console.error('[Confirm Error]', err);
        res.status(500).json({ statusCode: 500, message: err.message });
    }
});

// 5. Notification APIs
app.get('/api/notifications/:targetId', async (req, res) => {
    try {
        const { targetId } = req.params;
        console.log(`[Notification API] Fetching for: ${targetId}`);
        const data = await db.query({
            TableName: 'AppNotifications',
            KeyConditionExpression: 'targetId = :tid',
            ExpressionAttributeValues: { ':tid': targetId },
            ScanIndexForward: false // Newest first
        });
        res.json({ statusCode: 200, data: data.Items });
    } catch (err) {
        console.error('[Notification API Error]', err);
        res.status(500).json({ statusCode: 500, message: err.message });
    }
});

server.listen(PORT, () => {
    console.log(`Mobile Backend with Socket.io running on port ${PORT}`);
});
