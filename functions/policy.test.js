const {test} = require('node:test');
const assert = require('node:assert/strict');
const {mayTransition, validPass} = require('./policy');
const item = {id:'w1', authorId:'owner', status:'DISPONIBLE'};
const request = {wasteId:'w1', authorId:'owner', requesterId:'receiver', status:'PENDIENTE'};
test('only owner can accept a pending request for available material', () => {
 assert.equal(mayTransition('owner',item,request,'accept'),true);
 for (const uid of ['receiver','stranger',null]) assert.equal(mayTransition(uid,item,request,'accept'),false);
 assert.equal(mayTransition('owner',{...item,status:'RESERVADO'},request,'accept'),false);
 assert.equal(mayTransition('owner',item,{...request,wasteId:'other'},'accept'),false);
});
test('delivery requires accepted reservation and previously validated pass', () => {
 const reserved = {...item,status:'RESERVADO',reservedBy:'receiver'};
 const accepted = {...request,status:'ACEPTADA'};
 assert.equal(mayTransition('owner',reserved,accepted,'complete'),false);
 assert.equal(mayTransition('owner',reserved,{...accepted,passValidatedAt:100},'complete'),true);
 assert.equal(mayTransition('owner',{...reserved,reservedBy:'other'},{...accepted,passValidatedAt:100},'complete'),false);
 assert.equal(mayTransition('owner',{...reserved,status:'APROVECHADO'},{...accepted,passValidatedAt:100},'complete'),false);
});
test('pass rejects expired, used, missing and wrong validator', () => {
 const pass={authorId:'owner',expiresAt:200,usedAt:null};
 assert.equal(validPass(pass,'owner',100),true);
 assert.equal(validPass(pass,'owner',200),false);
 assert.equal(validPass(pass,'stranger',100),false);
 assert.equal(validPass({...pass,usedAt:50},'owner',100),false);
 assert.ok(!validPass(null,'owner',100));
});
