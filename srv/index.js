

var app     = require('express')();
var nano    = require('nano')('http://localhost:5984')
var bodyParser = require('body-parser');

var db_name = 'lasociale';
var db      = nano.use(db_name);

var doc_regex = /[a-z0-9]{20}-\d{4}-\d{2}-\d{2}T\d{2}/;

app.use(bodyParser.json()); // for parsing application/json


app.put(/^\/.*/, function(req, res) {
    console.log(req.body);
    console.log(req.path);


    var docname = req.path.substr(1);
    if (!doc_regex.test(docname))
    {
        console.error('No match: ', docname, doc_regex);
        res.status(400).end();
        return;
    }
    var doc = req.body;
    doc._id = docname;
    db.insert(doc, function(error) {
        if (error) {
            console.error(error.message);
            res.status(400).end();
        }
        else
        {
            console.log("INCOMING", req.body);
            console.log(req.path);
            res.status(201).end();
            
        }
    });

});

app.get(/^\/.*/, function(req, res) {
    var docname = req.path.substr(1);
    db.view('collect', 'collect', {key:docname}, function(error, body) {
        if (error) {
            console.error(error.message);
            res.status(400).end();
        }
        else
        {
            if (body.rows.length == 0)
            {
                res.status(404).end();
            }
            else
            {
                res.json(body.rows[0].value);
                res.status(200).end();
            }
        }
    });
});



app.listen(3333);
console.log("server is running. check expressjs.org for more cool tricks");

