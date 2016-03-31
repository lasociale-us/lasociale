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
app.use(bodyParser.raw({type:'image/*', limit:200*1024})); // for parsing testcases

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
                returnResult(req, res, 'xx', we, me);
            });
        }
        else
        {
            var we = body.rows[0].value;
            returnResult(req, res, 'xx', we);
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
    var isset = ((1 <<(n%4)) & nibval)>0;
    return isset;
}

function createSVG(req, hash, we, me)
{
    var centerX = 181.3; 
    var centerY = 214.9;
    var maxRadius = 140

    var color1 = 'style="stroke: #edcd6a !important;"';
    var color0 = '';

    var radiusMe = maxRadius * (me.lasociale / me.elapsed);
    var radiusWe = maxRadius * (we.lasociale / we.elapsed) ;
    var radiusTotal = 159;
    var PI = 3.14159
    var step = PI * 2 / 80;
    var angle = PI/4;
    var linesMe = '<g id="me_0_x2C_8">\n';
    var linesWe = '<g id="we_1_x2C_2">\n';
    var linesTotal = '<g id="total_0_x2C_35_sign">\n';
    for (var n=0; n < 80; n++) {
        var x1 = centerX;
        var y1 = centerY;
        angle = PI*2 * n / 80;
        var x2 = centerX + Math.cos(angle)*radiusMe;
        var y2 = centerY + Math.sin(angle)*radiusMe
        linesMe += '<line x1="'+x1+'" y1="'+y1+'" x2="' + x2 + '" y2="' + y2 + '" />\n';

        var x2 = centerX + Math.cos(angle)*radiusWe;
        var y2 = centerY + Math.sin(angle)*radiusWe
        linesWe += '<line x1="'+x1+'" y1="'+y1+'" x2="' + x2 + '" y2="' + y2 + '" />\n';

        var x2 = centerX + Math.cos(angle)*radiusTotal;
        var y2 = centerY + Math.sin(angle)*radiusTotal
        var color = getBit(hash, n) ? color1 : color0;
        linesTotal += '<line ' + color + ' x1="'+x1+'" y1="'+y1+'" x2="' + x2 + '" y2="' + y2 + '" />\n';

        angle += step;

    }
    linesMe += '</g>\n';
    linesWe += '</g>\n';
    linesTotal += '</g>\n';

    var total;
    if (radiusMe < radiusWe)
    {
        total = linesTotal + linesWe + linesMe;
    }
    else
        total = linesTotal + linesMe + linesWe;
    //reload template
    var svgTemplate = fs.readFileSync('template.svg', {encoding:'utf8'});
    var style = '#me_2_ { transform: scale(0.2); }\n';
    return (svgTemplate
        .replace('<!--STYLE-->', style)
        .replace('<!--MECONTENT-->', total)
        .replace('<!--ME-->',  Math.round(100*me.lasociale / me.elapsed)+'%' )
        .replace('<!--WE-->', Math.round(100*we.lasociale / we.elapsed)+'%' )
    );
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


app.put(/^\/[^_].*/, handlePut);
app.get(/^\/[^_].*/, handleGet);
app.put(/^\/_testcase/, handleTestcase);
app.get(/^\/_render/, handleRender);


app.listen(3333);
console.log("server is running.");

