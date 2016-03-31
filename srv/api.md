

API Documentation
=================

== Retrieve data:


GET /<hash>
GET /<hash>-<year>
GET /<hash>-<year>-<month>
GET /<hash>-<year>-<month>-<day>

GET /world
GET /world-<year>
GET /world-<year>-<month>
GET /world-<year>-<month>-<day>


Append request with 
.json
.png
.svg
.jpeg

to specify format, or use appropraiate "Accept-Header"

== Send data:

PUT /<hash>-<year>-<month>-<day>
Content-Type: application/json
{
  "lasociale": <seconds>,
  "elapsed":   <seconds>
}

== Specials:

PUT /\_link
Content-Type: application/json
{
  "nonce":     <nonce>,
  "hash":      <hash>
}

== Testing:

Custom rendering:
GET /\_render/<hash>/<lasociale>/<elapsed>

Send testcase:
PUT /\_testcase
bitmap-data

