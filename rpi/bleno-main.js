//runs on Raspberry pi
var bleno = require('bleno');
var axios = require('axios');

const USER_ID = "hasjdkhsa18293";
const DEVICE_NAME = "RPI";
const WORKING_TIME_EVIDENCE_SERVICE_UUID = "12ab";
const WORKER_AUTHENTICATION_CHARACTERISTIC_UUID = "34cd";
const WORKER_CHECKING_CHARACTERISTIC_UUID = "35cd";

const OFFICIAL_EXIT = 'OFFICIAL_EXIT';
const WORK_TIME_EXIT = 'WORK_TIME_END';
const WORK_TIME_BEGIN = 'WORK_TIME_BEGIN';
const PAUSE_EXIT = 'PAUSE_EXIT';

const OPTION_CHECK_IN = 'OPTION_CHECK_IN';
const OPTION_CHECK_END = 'OPTION_CHECK_END';
const OPTION_CHECK_OFFICAL_EXIT = 'OPTION_CHECK_OFFICAL_EXIT';
const OPTION_CHECK_PAUSE = 'OPTION_CHECK_PAUSE';

let user = {
    id: USER_ID,
    status: null,
    options: null,
    time: null
}

let users = []
users.push(user)

let currentWorker = null;

bleno.on('stateChange', function(state) {
    if (state === 'poweredOn') {
        bleno.startAdvertising(DEVICE_NAME, [WORKING_TIME_EVIDENCE_SERVICE_UUID]);
    } else {
        bleno.stopAdvertising();
    }
});

bleno.on('accept', function(clientAddress) {
    console.log("Accepted connection from address: " + clientAddress);
});

bleno.on('disconnect', function(clientAddress) {
    console.log("Disconnected from address: " + clientAddress);
});

bleno.on('advertisingStart', function(error) {
    if (error) {
        console.log("Advertising start error:" + error);
    } else {
        console.log("Advertising start success");
        bleno.setServices([            
            new bleno.PrimaryService({
                uuid : WORKING_TIME_EVIDENCE_SERVICE_UUID,
                characteristics : [
                    
                    new bleno.Characteristic({
                        value : null,
                        uuid : WORKER_AUTHENTICATION_CHARACTERISTIC_UUID,
                        properties : ['read', 'write'],
                        
                        onReadRequest : function(offset, callback) {
                            callback(this.RESULT_SUCCESS, new Buffer(this.value));
                        },
                        
                        onWriteRequest : function(data, offset, withoutResponse, callback) {
                            let worker = users.find(u => u.id === data.toString("utf-8"));
                            if(!worker){
                                bleno.disconnect();
                            }
                            else{
                                currentWorker = worker;
                                if(worker.status === null || worker.status === WORK_TIME_EXIT || worker.status === PAUSE_EXIT || worker.status === OFFICIAL_EXIT){
                                    worker.options = OPTION_CHECK_IN;
                                    this.value = OPTION_CHECK_IN;
                                }else if(worker.status === WORK_TIME_BEGIN){
                                    worker.options = OPTION_CHECK_OFFICAL_EXIT+'/'+OPTION_CHECK_PAUSE+"/"+OPTION_CHECK_END;
                                    this.value = OPTION_CHECK_OFFICAL_EXIT+'/'+OPTION_CHECK_PAUSE+"/"+OPTION_CHECK_END;
                                }
                            }
                            callback(this.RESULT_SUCCESS, new Buffer("succsess"));
                        }

                    }),
                    new bleno.Characteristic({
                        value: null,
                        uuid: WORKER_CHECKING_CHARACTERISTIC_UUID,
                        properties : ['write'],
                        onWriteRequest: function(data, offset, withoutResponse, callback) {
                            let choosenOption = data.toString('utf-8');
                            if(!currentWorker){
                                bleno.disconnect();
                            }else{
                                let record;
                                switch(choosenOption){
                                    case OPTION_CHECK_IN:
                                        switch(currentWorker.status){
                                            case WORK_TIME_EXIT:
                                                console.log("Worker arrived");
                                                sendRecord(`${new Date(new Date().toUTCString())}: arrived`); 
                                                break;
                                            case PAUSE_EXIT:
                                                record = `${new Date(new Date().toUTCString())}: came back from pause.`;
                                                console.log(record);
                                                sendRecord(record);
                                                break;
                                            case OFFICIAL_EXIT:
                                                record = `${new Date(new Date().toUTCString())}: came back from business departure.`;
                                                console.log(record);
                                                sendRecord(record);
                                                break;
                                            case null:
                                                console.log("worker arrived");      
                                                sendRecord(`${new Date(new Date().toUTCString())}: arrived`); 
                                                break;    
                                        }
                                        currentWorker.status = WORK_TIME_BEGIN;
                                        currentWorker.time = Date.now();
                                        break;
                                    case OPTION_CHECK_OFFICAL_EXIT:
                                        currentWorker.status = OFFICIAL_EXIT;
                                        record = `${new Date(new Date().toUTCString())}: official departure.`;
                                        sendRecord(record)
                                        currentWorker.time = Date.now()
                                        break;
                                    case OPTION_CHECK_PAUSE:
                                        currentWorker.status = PAUSE_EXIT;
                                        record = `${new Date(new Date().toUTCString())}: pause.`;
                                        sendRecord(record)
                                        currentWorker.time = Date.now();
                                        break;
                                    case OPTION_CHECK_END:
                                        currentWorker.status = WORK_TIME_EXIT;
                                        record = `${new Date(new Date().toUTCString())}: departure.`;
                                        console.log(record);
                                        sendRecord(record);
                                        currentWorker.time = Date.now();
                                        break;  
                                }
                                callback(this.RESULT_SUCCESS, new Buffer("succsess"));
                            }
                        }
                    })
                    
                ]
            })
        ]);
    }
});

const sendRecord = record => {
    const body = {
        record
    }
    axios.post("http://192.168.1.5:8080/records", body);
}