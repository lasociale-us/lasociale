

var app     = require('express')();
var nano    = require('nano')('http://localhost:5984')

var bodyParser = require('body-parser');

var db_name = 'lasociale';
var db      = nano.use(db_name);

var doc_regex = /^[a-z0-9]{20}-\d{4}-\d{2}-\d{2}$/;

app.use(bodyParser.json()); // for parsing application/json


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
            var worlddoc = docname.replace(/^[^-]*-/, 'world-'); 
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
                returnResult(req, res, we, me);
            });
        }
        else
        {
            var we = body.rows[0].value;
            returnResult(req, res, we);
        }
    });
}



// sends the image or json to the client
function returnResult(req, res, we, me) {

    var docname = req.path.substr(1);
    var mime;
    if (docname.indexOf('.') > -1)
    {
        mime = docname.substr(docname.indexOf('.')+1);
        docname = docname.substr(docname.indexOf('.'));
        if (mime === 'png') 
            mime = 'image/png';
        else if (mime === 'svg')
            mime = 'image/svg+xml';
        else if (mime !== 'json')
        {
            res.status(406).send('Not acceptable').end();
            return;
        }
    }

    if (!mime) {
        mime = req.accepts(['json', 'image/png', 'image/svg+xml']);
        if (!mime)
        {
            res.status(406).send('Not acceptable').end();
            return;
        }
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
    }
}

app.put(/^\/[^_].*/, handlePut);
app.get(/^\/[^_].*/, handleGet);


app.listen(3333);
console.log("server is running.");

