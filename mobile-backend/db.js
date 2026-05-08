const AWS = require('aws-sdk');
require('dotenv').config();

AWS.config.update({
    region: process.env.AWS_REGION || 'ap-southeast-1',
    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY
});

const docClient = new AWS.DynamoDB.DocumentClient();
const TABLE_NAME = process.env.DYNAMODB_TABLE_NAME || 'QuanLyDatBan-Table';

module.exports = {
    docClient,
    TABLE_NAME,
    // Helper to perform operations
    query: (params) => docClient.query(params).promise(),
    scan: (params) => docClient.scan(params).promise(),
    put: (params) => docClient.put(params).promise(),
    update: (params) => docClient.update(params).promise(),
    get: (params) => docClient.get(params).promise()
};
