function getXmlHttp(){
  var xmlhttp;
  try {
    xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
  } catch (e) {
    try {
      xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    } catch (E) {
      xmlhttp = false;
    }
  }
  if (!xmlhttp && typeof XMLHttpRequest!='undefined') {
    xmlhttp = new XMLHttpRequest();
  }
  return xmlhttp;
};

var xmlhttp= getXmlHttp();


var map;
function initialize(){
    lat=47.2584933;
    lng=39.7722394;
    var mapOptions = {
        center: new google.maps.LatLng(lat, lng),
        zoom: 15,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map =new  google.maps.Map(document.getElementById("map-canvas"),mapOptions);
    map.addListener('bounds_changed',function(){
        getData();

    })
}

google.maps.event.addDomListener(window, 'load', initialize);

function onSignIn(googleUser) {
            var profile = googleUser.getBasicProfile();
            var id_token = googleUser.getAuthResponse().id_token;
            authorize(id_token);
            console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
            console.log('Name: ' + profile.getName());
  console.log('Image URL: ' + profile.getImageUrl());
  console.log('Email: ' + profile.getEmail());
  document.getElementById("LoginZone").innerHTML="<button onClick='signOut()'>Выход</button>";
}

function signOut() {
    var auth2 = gapi.auth2.getAuthInstance();
    auth2.signOut().then(function () {
    document.getElementById("LoginZone").innerHTML='<div class="g-signin2" data-onsuccess="onSignIn">';});
}

var token='0';

function authorize(googleToken){
    xmlhttp = getXmlHttp();
    console.log("/intel/api.jsp?ReqName=Authorize&GoogleToken="+googleToken);
    xmlhttp.open('GET',"/intel/api.jsp?ReqName=Authorize&GoogleToken="+googleToken, true);
    xmlhttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xmlhttp.onreadystatechange = function() {
        if (xmlhttp.readyState == 4) {
            if(xmlhttp.status == 200) {
                var a=JSON.parse(xmlhttp.responseText);
                token=a.Token;
                console.log(xmlhttp.responseText);
                getData();
            } else
            console.log(xmlhttp);
        }
    }
    xmlhttp.send();
}

var player={};
var cities=[];

function getData(){
    if (token!='0' && token!=''){
        var map_bounds=map.getBounds();
    	var minlat=Math.round(map_bounds.getSouthWest().lat()*1E6);
    	var minlng=Math.round(map_bounds.getSouthWest().lng()*1E6);
    	var maxlat=Math.round(map_bounds.getNorthEast().lat()*1E6);
    	var maxlng=Math.round(map_bounds.getNorthEast().lng()*1E6);
        xmlhttp = getXmlHttp();
        console.log("/intel/api.jsp?ReqName=GetData&Token="+token+"&StartLat="+minlat+"&StartLng="+minlng+"&FinishLat="+maxlat+"&FinishLng="+maxlng);
        xmlhttp.open('GET',"/intel/api.jsp?ReqName=GetData&Token="+token+"&StartLat="+minlat+"&StartLng="+minlng+"&FinishLat="+maxlat+"&FinishLng="+maxlng, true);
        xmlhttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        xmlhttp.onreadystatechange = function() {
            if (xmlhttp.readyState == 4) {
                if(xmlhttp.status == 200) {
                    var data=JSON.parse(xmlhttp.responseText);
                    //Загрузка игрока
                    player.gold=data.Gold;
                    player.race=data.Race;
                    player.GUID=data.GUID;
                    player.exp=data.Exp;
                    player.name=data.Name;
                    //Jxbcnrf ujhljd tckb jyb tcnm/
                    if (cities!=null){
                        cities.forEach(function(currentVal,index,arr){
                           currentVal.mark.setMap(null);
                        });
                    }
                    cities=[];
                    data.Cities.forEach(function(currentVal,index,arr){
                        var city={};
                        city.lat=currentVal.Lat/1e6;
                        city.lng=currentVal.Lng/1e6;
                        city.level=currentVal.Level;
                        city.up=currentVal.Upgrade;
                        city.name=currentVal.Name;
                        city.race=currentVal.Faction;

                        city.mark = new google.maps.Marker({
                            position: {lat: city.lat, lng: city.lng},
                            title: city.name,
                            icon: "intel/img/city_"+(city.level/2);
                            map: map
                        });
                        cities.push(city);

                     })
                } else
                console.log(xmlhttp);
            }
        }
        xmlhttp.send();
    }
}



