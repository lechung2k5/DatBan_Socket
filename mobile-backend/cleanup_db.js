const db = require('./db');

async function cleanup() {
    console.log("Starting Database Cleanup...");

    // 1. Cleanup Tables with 'P' prefix
    console.log("\n1. Checking legacy tables (prefix 'P')...");
    const tableData = await db.scan({ TableName: 'Tables' });
    const pTables = tableData.Items.filter(t => t.maBan && t.maBan.startsWith('P'));
    
    for (const t of pTables) {
        console.log(`Deleting legacy table: ${t.maBan}`);
        await db.docClient.delete({ TableName: 'Tables', Key: { maBan: t.maBan } }).promise();
    }
    console.log(`Deleted ${pTables.length} legacy tables.`);

    // 2. Cleanup Orphaned Invoices
    console.log("\n2. Checking orphaned invoices (empty tableId and status 'ChoXacNhan')...");
    const invoiceData = await db.scan({ TableName: 'Invoices' });
    const orphans = invoiceData.Items.filter(inv => 
        (!inv.tableId || inv.tableId.trim() === "") && 
        (inv.status === 'ChoXacNhan' || inv.status === 'Dat')
    );

    for (const inv of orphans) {
        console.log(`Deleting orphaned invoice: ${inv.invoiceId} (Created: ${inv.createdAt})`);
        await db.docClient.delete({ TableName: 'Invoices', Key: { invoiceId: inv.invoiceId } }).promise();
    }
    console.log(`Deleted ${orphans.length} orphaned invoices.`);

    console.log("\nCleanup Complete!");
}

cleanup().catch(err => {
    console.error("Cleanup Failed:", err);
    process.exit(1);
});
