const AWS = require('aws-sdk');
require('dotenv').config();

AWS.config.update({
    region: process.env.AWS_REGION || 'ap-southeast-1',
    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY
});

const docClient = new AWS.DynamoDB.DocumentClient();

const RELEVANT_TABLES = [
    'Invoices',
    'MenuItems',
    'Customers',
    'Employees',
    'Tables',
    'Promos',
    'Shifts'
];

async function inspect() {
    try {
        console.log("==========================================");
        console.log("RESTAURANT PROJECT SCHEMA INSPECTION");
        console.log("==========================================");

        for (const tableName of RELEVANT_TABLES) {
            console.log(`\nChecking Table: ${tableName}...`);
            try {
                const data = await docClient.scan({ TableName: tableName, Limit: 5 }).promise();
                
                if (data.Items.length === 0) {
                    console.log(`  [${tableName}] is empty.`);
                    continue;
                }

                const allKeys = new Set();
                data.Items.forEach(item => {
                    Object.keys(item).forEach(key => allKeys.add(key));
                });

                console.log("  Actual Fields (Sample data):");
                Array.from(allKeys).sort().forEach(key => {
                    const val = data.Items[0][key];
                    let type = typeof val;
                    if (val === null) type = 'null';
                    else if (Array.isArray(val)) type = 'array';
                    else if (type === 'object') type = 'object/map';
                    
                    console.log(`    - ${key} (${type})`);
                });
            } catch (err) {
                console.log(`  Error scanning ${tableName}: ${err.message}`);
            }
        }
    } catch (err) {
        console.error("Critical error:", err.message);
    }
}

inspect();
