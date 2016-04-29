
lasociale = window.lasociale || {};

lasociale.host = lasociale.host || '185.77.131.115';
lasociale.container = lasociale.container || 'cont';

// get today in yyyy-mm-dd format
lasociale.getDate = function() {
   var dt = new Date();
   var dtm = dt.getMonth()+1;
   if (dtm<10) dtm = '0' + dtm;
   var dtd = dt.getDate();
   if (dtd <10) dtd = '0' + dtd;
   var now = dt.getFullYear() +'-' + dtm + '-' + dtd;
 }

lasociale.getChecksum =function(bytes) {
    var n1 = 0;
    var mult = [ 17, 73, 14, 2, 27, 99, 101, 7];
    for(var n =0; n < 8; n++)
    {
       n1 = n1 + (bytes[n] + (bytes[n] * mult[n])) & 0xFFFF;
    }

    return [ (n1 & 0xFF), ((n1>>8)&0xFF)];
}

lasociale.setChecksum = function(bytes) {
   var cs = lasociale.getChecksum(bytes);
   bytes[8] = cs[0];
   bytes[9] = cs[1];
 }

lasociale.checkChecksum = function(bytes) {
   var cs = lasociale.getChecksum(bytes);
   return bytes[8] === cs[0] && bytes[9] == cs[1];
}



// convert byte-array to hex
lasociale.toHex = function(bytes) {
   var res = '';
   for(var b=0; b< bytes.length; b++) {
     if (bytes[b] < 0x10)
       res += '0';
     res += bytes[b].toString(16);
   }

   return res;
}

// creates a random nonce with checksum
// used to push lasociales to the server
lasociale.createNonce = function() {
   var bytes = [];
   for(var b=0; b < 10; b++) {
     for(;;)
     {
       bytes[b] = Math.floor(Math.random() * 256);
       if (bytes[b] !== 0xBA && bytes[b] !== 0x98 && bytes[b] !== 0xDC)
         break;
     }
   }
   bytes[6] = 0xBA;
   lasociale.setChecksum(bytes);
   return lasociale.toHex(bytes);
} 


lasociale.connectWS = function(nonce) {
   var url = 'ws://' + lasociale.host + ':3334/';
   var lssocket = new WebSocket(url);
   lssocket.onopen = function(event) {
     lssocket.send(nonce);
   }
   lssocket.onmessage = function (event) {
     var url = 'http://' + lasociale.host + ':3333/' + event.data + '.svg';
     document.getElementById(lasociale.container).innerHTML=
       '<img src="' + url + '"/>';

       
   }
 }

window.onload = function() {
   var nonce = lasociale.createNonce();
   console.log('Generate nonce-pic', nonce);
   document.getElementById(lasociale.container).innerHTML=
     '<img src="http://'+lasociale.host+':3333/world.svg?nonce='+nonce+'" />';

   lasociale.connectWS(nonce);
}

