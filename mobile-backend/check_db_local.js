const db = require('./db');

async function check() {
    console.log("--- TABLES ---");
    const tables = await db.scan({ TableName: 'Tables' });
    tables.Items.forEach(t => console.log(`Table: ${t.maBan} | Status: ${t.trangThai}`));

    console.log("\n--- RECENT INVOICES (LAST 5) ---");
    const invoices = await db.scan({ TableName: 'Invoices' });
    invoices.Items
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        .slice(0, 5)
        .forEach(inv => console.log(`Inv: ${inv.invoiceId} | Table: ${inv.tableId} | Status: ${inv.status} | Created: ${inv.createdAt}`));
}

check().catch(console.error);
