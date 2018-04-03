const express = require('express')
const app = express()
const { execSync } = require('child_process');
const fs = require('fs');
var path = require('path');
var bodyParser = require('body-parser');
app.use(bodyParser.json());
app.use(express.static(path.join(__dirname, 'frontend'))) // public/static files
app.use(bodyParser.urlencoded({ extended: true }));
var frontend = __dirname + "/frontend/";
var visuals = __dirname + "/visuals/";
global.__basedir = __dirname;

app.get('/', (req, res) => res.sendFile(path.join(frontend + 'index.html')));

app.post('/histogram', function(req, res) {
    var parent = path.dirname(__basedir)
    const content = JSON.stringify(req.body);
    fs.writeFileSync(parent+'/configuration.json', content, 'utf8', function (err) {
        if (err) {
            return console.log(err);
        }

        console.log("The file was saved!");
    });
    execSync('python main_generator.py configuration.json', {stdio:[0,1,2],cwd: parent}, (err, stdout, stderr) => {
        if (err) {
            console.error(`exec error: ${err}`);
            return;
        }
        console.log(`stdout: ${stdout}`);
        console.log(`stderr: ${stderr}`);
    });
    fs.stat(parent+'/.histogram_main.sb.src', function (err, stats) {
        console.log(stats);//here we got all information of file in stats variable

        if (err) {
            return console.error(err);
        }

        fs.unlink(parent+'/.histogram_main.sb.src', function(err){
            if(err){
                return console.log(err);
            }
            console.log('file deleted successfully');
        });
    });
    execSync('./compile.sh histogram_main.sc', {stdio:[0,1,2],cwd: parent}, (err, stdout, stderr) => {
        if (err) {
            console.error(`exec error: ${err}`);
            return;
        }
        console.log(`stdout: ${stdout}`);
        console.log(`stderr: ${stderr}`);
    });
    execSync('./run.sh histogram_main.sb 2> out.txt', {stdio:[0,1,2],cwd: parent}, (err, stdout, stderr) => {
        if (err) {
            console.error(`exec error: ${err}`);
            return;
        }
        console.log(`stdout: ${stdout}`);
        console.log(`stderr: ${stderr}`);
    });
    execSync('python plot.py', {stdio:[0,1,2],cwd: parent}, (err, stdout, stderr) => {
        if (err) {
            console.error(`exec error: ${err}`);
            return;
        }
        console.log(`stdout: ${stdout}`);
        console.log(`stderr: ${stderr}`);
    });
    if (Array.isArray(req.body['attributes']) && req.body['attributes'].length == 2){
        res.sendFile(path.join(visuals + '2D_Histogram1.html'));
    } else {
        res.sendFile(path.join(visuals + '1D_Histogram1.html'));
    }
});


app.listen(3000, () => console.log('Example app listening on port 3000!'))