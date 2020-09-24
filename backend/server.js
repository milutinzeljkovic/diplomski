var express = require('express');
var app = express();
var cors = require('cors')

const USER_ID = "hasjdkhsa18293";
let user = {
    id: USER_ID,
    name: "John",
    lastName: "Doe",
    records: []
}

let user2 = {
    id: "123123",
    lastName: "Doe",
    name: "Jane"
}

let users = []
users.push(user);
users.push(user2);

app.use(
    express.urlencoded({
      extended: true
    })
)

app.use(cors())

app.use(express.json())

let records = []

app.get('/', function (req, res) {
   res.send('Hello World');
})

app.post('/records', (req, res) => {
    console.log(req.body);
    records.push(req.body);
    user.records.push(req.body);
    res.status(200).send();
})

app.get('/records', (req, res) => {
    res.status(200).send(JSON.stringify(records))
})

app.get('/workers', (req, res) => {
    res.status(200).send(JSON.stringify(users))
})

var server = app.listen(8080, function () {
   var host = server.address().address
   var port = server.address().port
   
   console.log("Example app listening at http://%s:%s", host, port)
})