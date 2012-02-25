var http = require("http"),
    Q = require('q'),
    _ = require('underscore'),
    dateformat = require('dateformat'),
    load = require("./load.js"),
    send = require("./send.js")
    ;

var prefix = 'dvx',
    eventId = 6,
    baseUrl = 'http://localhost:8080';

var voxxrin = {
    event: {},
    rooms: {},
    daySchedules: {}
};


Q.all([
    load('http://cfp.devoxx.com/rest/v1/events/' + eventId),
    load('http://cfp.devoxx.com/rest/v1/events/' + eventId + '/schedule/rooms'),
    load('http://cfp.devoxx.com/rest/v1/events/' + eventId + '/schedule')
]).spread(function(event, rooms, schedule) {
    console.log("loaded event " + event.id + ", " + schedule.length
        + " presentations and " + rooms.length + " rooms");
    var from = new Date(Date.parse(event.from)),
        to = new Date(Date.parse(event.to));

    voxxrin.event = {
        "id": prefix + event.id,
        "title":event.name,"subtitle":"","description":event.description,
        "dates": formatDates(from, to),
        "from": schedule.length ? schedule[0].fromTime : event.from,
        "to": schedule.length ? schedule[schedule.length-1].toTime : event.to,
        "location":event.location, "nbPresentations":0,
        "days":[],
        "enabled":true
    };
    _(rooms).each(function(r) {
        voxxrin.rooms[r.name] = {"id":voxxrin.event.id + "-" + r.id, "name": r.name,
            "uri": "/events/" + voxxrin.event.id + "/room/" + voxxrin.event.id + "-" + r.id};
    });
    var day, i;
    for (day = from, i=0; day <= to; day.setDate(day.getDate() + 1), i++) {
        voxxrin.event.days.push(
            {"id":prefix + event.id + '-' + i,
                "name": dateformat(day, 'mmm dd'),
                "uri": "/events/" + (prefix + event.id) + "/day/" + prefix + event.id + '-' + i,
                "nbPresentations":0});
        voxxrin.daySchedules[dateformat(day, 'yyyy-mm-dd')] =
            {"id":prefix + event.id + '-' + i, "dayNumber": i, "schedule":[]};
    }
    _(schedule).each(function(s) {
        if (s.presentationUri) {
            voxxrin.event.nbPresentations++;
            var fromTime = new Date(Date.parse(s.fromTime)),
                daySchedule = voxxrin.daySchedules[dateformat(fromTime, 'yyyy-mm-dd')];
            voxxrin.event.days[daySchedule.dayNumber].nbPresentations++;

            var voxxrinPres = {"id":prefix + s.id, "title":s.title, "type":s.type, "kind":s.kind,
                    "uri":"/events/" + voxxrin.event.id + "/presentations/" + prefix + s.id,
                    "speakers": _(s.speakers).map(toVoxxrinSpeaker),
                    "room": voxxrin.rooms[s.room],
                    "slot": dateformat(fromTime, fromTime.getMinutes() ? 'h:MMtt' : 'htt'), "fromTime":s.fromTime,"toTime":s.toTime};
            daySchedule.schedule.push(voxxrinPres);
            load(s.presentationUri).then(function(p) {
                send(baseUrl + '/r' + voxxrinPres.uri,
                    _.extend(voxxrinPres, {
                        "track":p.track,
                        "experience":p.experience,
                        "tags":p.tags,
                        "summary":p.summary
                    }))
                    .then(function() {console.log('PRESENTATION: ', voxxrinPres)})
                    .fail(onFailure);
            }).fail(onFailure);
        }
    });

    send(baseUrl + '/r/events', voxxrin.event).then(function() {
        console.log('EVENT:', voxxrin);
    }).fail(onFailure);
    _(voxxrin.daySchedules).each(function (ds) {
        send(baseUrl + '/r/events/' + voxxrin.event.id + '/day/' + ds.id, ds).then(function(){
            console.log('DAY SCHEDULE:', ds.id, ds.schedule);
        }).fail(onFailure);
    });
}).fail(onFailure);

function toVoxxrinSpeaker(sp) {
    var id = sp.speakerUri.substring(sp.speakerUri.lastIndexOf('/') + 1);
    return {"id":prefix + id, "name":sp.speaker,
        "uri":"/events/" + voxxrin.event.id + "/speakers/" + prefix + id}
}

function formatDates(from, to) {
    if (from === to) {
        return dateformat(from, 'mmm dd, yyyy');
    } else if (from.getMonth() === to.getMonth()) {
        return dateformat(from, 'mmm') + ' '
            + from.getDate() + '-' + to.getDate()
            + ', ' + dateformat(from, 'yyyy');
    } else if (from.getYear() === to.getYear()) {
        return dateformat(from, 'mmm dd') + ' - '
            + dateformat(to, 'mmm dd, yyyy');
    } else {
        return dateformat(from, 'mmm dd, yyyy') + ' - ' + dateformat(to, 'mmm dd, yyyy');
    }
}

function onFailure(err) {
    console.log(err);
}

http.createServer(function(req, response) {
  response.writeHead(200, {"Content-Type": "text/plain"});
  response.write("Started crawling...");
  response.end();
}).listen(8888);