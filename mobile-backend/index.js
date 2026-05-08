const express = require('express');
const cors = require('cors');
const db = require('./db');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.MOBILE_BACKEND_PORT || 3001;

// Helper: Generate Invoice ID like Java (HDddMMyyxxxx)
async function generateInvoiceId() {
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
            giaBan: item.price,
            maDM: item.categoryId,
            hinhAnhUrl: item.imageUrl,
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

app.listen(PORT, () => {
    console.log(`Mobile Backend running on port ${PORT}`);
});
