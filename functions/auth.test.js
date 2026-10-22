const {test} = require('node:test');
const assert = require('node:assert/strict');
process.env.GCLOUD_PROJECT = 'demo-woodmatch';
const endpoints = require('./index');
for (const [name, endpoint] of Object.entries(endpoints)) {
 test(`${name}: unauthenticated invocation is denied before accessing data or providers`, async () => {
  await assert.rejects(() => endpoint.run({data:{}}), error => error.code === 'unauthenticated');
 });
}
