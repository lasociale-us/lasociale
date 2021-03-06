var fs      = require('fs');
var im      = require('imagemagick');
var spawn = require('child_process').spawn;

var app     = require('express')();
var nano    = require('nano')('http://localhost:5984')

var bodyParser = require('body-parser');

var db_name = 'lasociale';
var db      = nano.use(db_name);

var doc_regex = /^[a-z0-9]{20}-\d{4}-\d{2}-\d{2}$/;

var JPEG_QUALITY = 50;

app.use(bodyParser.json()); // for parsing json
app.use(bodyParser.raw({type:'image/*', limit:2*1024*1024})); // for parsing testcases

// load template
var svgTemplate = fs.readFileSync('template.svg', {encoding:'utf8'});

function handlePut(req, res) {

    var docname = req.path.substr(1);
    if (!doc_regex.test(docname))
    {
        console.error('No match: ', docname, doc_regex);
        res.status(400).send('invalid name').end();
        return;
    }
    var doc = req.body;
    doc._id = docname;

    db.get(docname, function(error, body) {

        if (error && error.statusCode !== 404) {
            console.error('error searching previous revision:', error);
            res.status(error.statusCode).send(error.error).end();
            return;
        }
        if (!error) {
            // mark previous version
            doc._rev = body._rev;
        }

        db.insert(doc, function(error) {
            if (error) {
                res.status(400).send(error.message).end();
                return;
            }
            else
            {
                console.log("INCOMING", req.body);
                console.log(req.path);
                res.status(201)

                // further processing as get 
                handleGet(req, res);
                
            }
        });
    });
}


function handleGet(req, res) {
    var docname = req.path.substr(1);
    if (docname.indexOf('.') > -1)
        docname = docname.substr(0,docname.indexOf('.'));

    console.log('retrieving ', docname);
    db.view('collect', 'collect', {key:docname}, function(error, body) {
        if (error) {
            console.error(error.message);
            res.status(error.statusCode).end();
            return;
        }
        if (body.rows.length == 0)
        {
            res.status(404).end();
            return;
        }


        if (!/^world/.test(docname)) {
            var me = body.rows[0].value;
            var worlddoc = docname.replace(/^[^-]*/, 'world'); 
            console.log('worlddoc:', worlddoc);
            db.view('collect', 'collect', {key:worlddoc}, 
            function(error, body) {
                if (error) {
                    console.error(error.message);
                    res.status(error.statusCode).end();
                    return;
                }
                if (body.rows.length == 0)
                {
                    res.status(404).end();
                    return;
                }
                var we = body.rows[0].value;
                var hash = docname.replace(/-.*$/g, '');
                returnResult(req, res, hash, we, me);
            });
        }
        else
        {
            var we = body.rows[0].value;
            returnResult(req, res, req.query.nonce||'00000000000000000000', we);
        }
    });
}

function getMimeType(req) {
    var docname = req.path.substr(1);
    var mime;
    if (docname.indexOf('.') > -1)
    {
        mime = docname.substr(docname.indexOf('.')+1);
        docname = docname.substr(docname.indexOf('.'));
        if (mime === 'png') 
            mime = 'image/png';
        else if (mime === 'jpeg')
            mime = 'image/jpeg';
        else if (mime === 'svg')
            mime = 'image/svg+xml';
        else if (mime !== 'json')
        {
            mime = 0; 
            return;
        }
    }

    if (!mime) {
        mime = req.accepts(['json', 'image/jpeg', 'image/png', 'image/svg+xml']);
    }

    return mime;
}

function getBit(hash, n) {
    var nibble = hash.substr(Math.floor(n/4),1);
    var nibval = parseInt(nibble, 16);
    var isset = ((1 <<(3-(n%4))) & nibval)>0;
    return isset;
}

function createSVG(req, hash, we, me)
{
    console.log('creating for hash', hash);
    var centerX = 181.3; 
    var centerY = 214.9;
    var maxRadius = 140
    var styleMe = ' style="fill:none;stroke:#EF5AA0;stroke-width:2.2677;stroke-miterlimit:10;" ';
    var styleWe = ' style="fill:none;stroke:#37B34A;stroke-width:3.4016;stroke-miterlimit:10;" ';
    
    // hue = 200/255 = 141/180
    //var styleT1 = ' style="fill:none;stroke:#b501ff;stroke-width:1.4921;stroke-miterlimit:10;" ';   
    //
    // hue = 134/255 = 95
    //var styleT2 = ' style="fill:none;stroke:#01d8ff;stroke-width:1.4921;stroke-miterlimit:10;" ';   

    var styleT1 = ' style="fill:none;stroke:#ff1111;stroke-width:1.9921;stroke-miterlimit:10;" ';   
    //
    var styleT2 = ' style="fill:none;stroke:#1111ff;stroke-width:1.9921;stroke-miterlimit:10;" ';   

    //var styleT1 = ' style="fill:#bbaa11;stroke:#bbaa11;stroke-width:1.9921;stroke-miterlimit:10;" ';   
    //
    //var styleT2 = ' style="fill:#aa11bb;stroke:#aa11bb;stroke-width:1.9921;stroke-miterlimit:10;" ';   
    var radiusMe;
    if (me)
        radiusMe = maxRadius * (me.lasociale / me.elapsed);
    else
        radiusMe = 0;
    var radiusWe = maxRadius * (we.lasociale / we.elapsed) ;
    var radiusTotal = 159;
    var PI = 3.14159
    var step = PI * 2 / 80;
    var angle = PI/4;
    var linesMe = '<g id="me_0_x2C_8">\n';
    var linesWe = '<g id="we_1_x2C_2">\n';
    var linesTotal = '<g id="total_0_x2C_35_sign">\n';
    var rnd = Math.random()*80;
    for (var n=0; n < 80; n++) {
        var x1 = centerX;
        var y1 = centerY;
        angle = PI*2 * n / 80;
        var x2 = centerX + Math.cos(angle)*radiusMe;
        var y2 = centerY + Math.sin(angle)*radiusMe
        
        if (n > rnd && n < rnd+1)
            linesMe += '<line '+styleMe+' x1="'+x1+'" y1="'+y1+'" x2="' + x2 + '" y2="' + y2 + '" />\n';

        var x2 = centerX + Math.cos(angle)*radiusWe;
        var y2 = centerY + Math.sin(angle)*radiusWe
        linesWe += '<line '+styleWe+' x1="'+x1+'" y1="'+y1+'" x2="' + x2 + '" y2="' + y2 + '" />\n';

        var x2 = centerX + Math.cos(angle)*radiusTotal;
        var y2 = centerY + Math.sin(angle)*radiusTotal;
        var x3 = centerX + Math.cos(angle)*radiusTotal*.95;
        var y3 = centerY + Math.sin(angle)*radiusTotal*.95;

        var arange = 2 * PI / 80 ;
        var x4 = centerX + Math.cos(angle+arange)*radiusTotal;
        var y4 = centerY + Math.sin(angle+arange)*radiusTotal;
        var x5 = centerX + Math.cos(angle+arange)*radiusTotal*.95;
        var y5 = centerY + Math.sin(angle+arange)*radiusTotal*.95;
        var color = getBit(hash, n) ? styleT2 : styleT1;
        linesTotal += '<line ' + color + ' x1="'+x1+'" y1="'+y1+'" x2="' + x2 + '" y2="' + y2 + '" />\n';
        /*linesTotal += '<path ' + color + 
            ' d="M' + x2 + ' ' + y2 + ' ' +
            '    L' + x3 + ' ' + y3 + ' ' +
            '    L' + x5 + ' ' + y5 + ' ' +
            '    L' + x4 + ' ' + y4 + ' Z" />' ;
*/
        angle += step;

    }
    linesMe += '</g>\n';
    linesWe += '</g>\n';
    linesTotal += '</g>\n';

    var total;
    total = linesTotal + linesWe + linesMe;
    //reload template
    var svgTemplate = fs.readFileSync('template.svg', {encoding:'utf8'});
    var style = '#me_2_ { transform: scale(0.2); }\n';
    var resp = svgTemplate
        .replace('<!--STYLE-->', style)
        .replace('<!--MECONTENT-->', total);
    if (me)
        resp = resp.replace('<!--ME-->',  Math.round(100*me.lasociale / me.elapsed)+'%' )
    if (we)
        resp = resp.replace('<!--WE-->', Math.round(100*we.lasociale / we.elapsed)+'%' )
    
    return resp;
}

// sends the image or json to the client
function returnResult(req, res, hash, we, me) {

    var mime = getMimeType(req);
    if (!mime) {
        res.status(406).send('Not acceptable').end();
        return;
    }

    var resObj = {};
    if (me) resObj.me = me;
    if (we) resObj.we = we;

    if (mime === 'json') {
        res.setHeader('Content-Type', 'application/json');
        res.send(JSON.stringify(resObj, null, 2));
        res.end();
    }
    else {
        var svg = createSVG(req,hash,we,me);
        
        res.setHeader('Content-Type', mime);
        if (mime === 'image/svg+xml') {
            res.send(svg);
            res.end();
        }
        else if (mime === 'image/png')
        {
            // convert to png
            var conv = spawn('convert', ['svg:-', 'png:-'])
            conv.stdout.pipe(res);
            conv.stdin.write(svg);
            conv.stdin.end();
        }
        else if (mime === 'image/jpeg')
        {
            // convert to jpeg
            var conv = spawn('convert', ['svg:-', '-quality', JPEG_QUALITY, 'jpeg:-'])
            conv.stdout.pipe(res);
            conv.stdin.write(svg);
            conv.stdin.end();
        }
    }
}

// upload test-case 
// invoked when tapping the preview
function handleTestcase(req, res) {

    var outp = fs.createWriteStream('../test/cases/' + new Date().toISOString() + '.png');
    outp.write(req.body);
    outp.close();
    res.status(201).end();
}

// custom rendering for tests
function handleRender(req, res) {
    var path = req.path.substr('/_render/'.length);
    path = path.replace(/\//g, '-');

    var steps = path.split('-');
    var hash = steps[0];
    var we = {
        "lasociale": parseInt(steps[1]),
        "elapsed": 100
    }
    var me = {
        "lasociale": parseInt(steps[2]),
        "elapsed": 100
    }

    return returnResult(req, res, hash, we, me);
}


var sockets = {};

function handleLink(req, res) {
    console.log('LINK: ' + req.body.nonce + '=' + req.body.hash);

    if (sockets[req.body.nonce])
    {
        sockets[req.body.nonce].sendText(req.body.hash);
        res.status(200).end();
    }
    else
    {
        res.status(404).end();
    }

}

function handleTest(req, res) {
    var html = fs.readFileSync('test.html', {encoding:'utf8'});
    res.setHeader('Content-Type', 'text/html');

    res.send(html).end();
}

function handleScript(req, res) {
    var js = fs.readFileSync('lasociale.js', {encoding:'utf8'});
    res.setHeader('Content-Type', 'application/javascript');

    res.send(js).end();
}


app.put(/^\/[^_].*/, handlePut);
app.get(/^\/[^_].*/, handleGet);
app.put(/^\/_testcase/, handleTestcase);
app.get(/^\/_render/, handleRender);
app.get(/^\/_test/, handleTest);
app.put(/^\/_link/, handleLink);
app.get(/^\/_script/, handleScript);


app.listen(3333);
console.log("server is running.");

var ws = require("nodejs-websocket")
 
// Scream server example: "hi" -> "HI!!!" 
var server = ws.createServer(function (conn) {
    var nonce = 0;
    console.log("New connection")
    conn.on("text", function (str) {
        console.log("Received "+str)
        sockets[str] = conn;
        nonce = str;
        //conn.sendText(str.toUpperCase()+"!!!")
    })
    conn.on("close", function (code, reason) {
        console.log("Connection closed")
        delete sockets.nonce;
    })
}).listen(3334);

console.log('WS-server listening');

