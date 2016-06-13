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
}

function signOut() {
    var auth2 = gapi.auth2.getAuthInstance();
    auth2.signOut().then(function () {
    document.getElementById("LoginZone").innerHTML='<div class="g-signin2" data-onsuccess="onSignIn">';});
}

var token='0';

function authorize(googleToken){
    xmlhttp = getXmlHttp();
    xmlhttp.open('GET',"/intel/api.jsp?ReqName=Authorize&GoogleToken="+googleToken, true);
    xmlhttp.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xmlhttp.onreadystatechange = function() {
        if (xmlhttp.readyState == 4) {
            if(xmlhttp.status == 200) {
                var a=JSON.parse(xmlhttp.responseText);
                token=a.Token;

                getData();
            } else
            console.log(xmlhttp);
        }
    }
    xmlhttp.send();
}

var player={};
var cities=[];
var ambushes=[];
var routes=[];

function getData(){
    if (token!='0' && token!=''){
        var map_bounds=map.getBounds();
    	var minlat=Math.round(map_bounds.getSouthWest().lat()*1E6);
    	var minlng=Math.round(map_bounds.getSouthWest().lng()*1E6);
    	var maxlat=Math.round(map_bounds.getNorthEast().lat()*1E6);
    	var maxlng=Math.round(map_bounds.getNorthEast().lng()*1E6);
        xmlhttp = getXmlHttp();
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
                    //Очищаем города
                    if (cities!=null){
                        cities.forEach(function(currentVal,index,arr){
                           currentVal.mark.setMap(null);
                           currentVal.circle.setMap(null);
                        });
                    }
                    //Рисуем города
                    cities=[];
                    data.Cities.forEach(function(currentVal,index,arr){
                        var city={};
                        city.lat=currentVal.Lat/1e6;
                        city.lng=currentVal.Lng/1e6;
                        city.level=currentVal.Level;
                        city.up=currentVal.Upgrade;
                        city.name=currentVal.Name;
                        city.founder=currentVal.Founder;
                        city.race=currentVal.Faction;
                        city.infmax=currentVal.Inf1+currentVal.Inf2+currentVal.Inf3;
                        city.inf1=currentVal.Inf1;
                        city.inf2=currentVal.Inf2;
                        city.inf3=currentVal.Inf3;
                        var pic="intel/img/city_"+Math.round(city.level/2)+".png";
                        var size=6;
                        if (city.level<3) size=2; //Поселки маленькие
                        size=size*(map.getZoom()+1);
                        var image = {
                            url: pic,
                            // This marker is 20 pixels wide by 32 pixels high.
                            size: new google.maps.Size(192, 192),
                            // The origin for this image is (0, 0).
                            origin: new google.maps.Point(0, 0),
                            // The anchor for this image is the base of the flagpole at (0, 32).
                            anchor: new google.maps.Point(size/2, size/2),
                            scaledSize: new google.maps.Size(size, size)
                          };
                        city.mark = new google.maps.Marker({
                            position: {lat: city.lat, lng: city.lng},
                            title: city.name,
                            icon: image,
                            map: map
                        });

                        city.radius=100+5*(city.level - 1);
                        city.circle= new google.maps.Circle({
                            center:{lat: city.lat, lng: city.lng},
                            radius:city.radius,
                            fillOpacity:0,
                            strokeColor:'#0000ff',
                            map:map
                        });
                        if (map.getZoom()<15) city.circle.setVisible(false);
                        city.infoWindow = new google.maps.InfoWindow({
                        content:"<table><tr><td>Название:</td><td>"+city.name+" "+city.level+"</td></tr>"+
                        "<tr><td>Основатель:</td><td>"+city.founder+"</td></tr>"+
                        "<tr><td>Умение:</td><td>"+city.up+"</td></tr>"+
                        "<tr><td>Гильдия:</td><td><progress max='"+city.infmax+"' value='"+city.inf1+"'/></td></tr>"+
                        "<tr><td>Альянс:</td><td><progress max='"+city.infmax+"' value='"+city.inf2+"'/></td></tr>"+
                        "<tr><td>Союз:</td><td><progress max='"+city.infmax+"' value='"+city.inf3+"'/></td></tr></table>",
                        position:{lat: city.lat, lng: city.lng}
                        });
                        google.maps.event.addListener(city.mark, 'click', function () {
                                        city.infoWindow.open(map, this);
                                    });
                        cities.push(city);

                     });
                     //Очищаем засады
                     if (ambushes!=null){
                         ambushes.forEach(function(currentVal,index,arr){
                            currentVal.mark.setMap(null);
                            currentVal.circle.setMap(null);
                         });
                     }
                     //Рисуем рисуем засады
                     ambushes=[];
                     data.Ambushes.forEach(function(currentVal,index,arr){
                         var ambushe={};
                         ambushe.lat=currentVal.Lat/1e6;
                         ambushe.lng=currentVal.Lng/1e6;
                         ambushe.life=currentVal.Life*10;
                         ambushe.tts=currentVal.TTS;
                         ambushe.name=currentVal.Name;
                         ambushe.radius=currentVal.Radius;
                         var pic="intel/img/ambush.png";
                         var size=2;
                         size=size*(map.getZoom()+1);
                         var image = {
                             url: pic,
                             // This marker is 20 pixels wide by 32 pixels high.
                             size: new google.maps.Size(192, 192),
                             // The origin for this image is (0, 0).
                             origin: new google.maps.Point(0, 0),
                             // The anchor for this image is the base of the flagpole at (0, 32).
                             anchor: new google.maps.Point(size/2, size),
                             scaledSize: new google.maps.Size(size, size)
                           };
                         ambushe.mark = new google.maps.Marker({
                             position: {lat: ambushe.lat, lng: ambushe.lng},
                             title: ambushe.name,
                             icon: image,
                             map: map
                         });

                         ambushe.circle= new google.maps.Circle({
                             center:{lat: ambushe.lat, lng: ambushe.lng},
                             radius:ambushe.radius,
                             fillOpacity:0,
                             strokeColor:'#ff0000',
                             map:map
                         });
                         if (map.getZoom()<15) ambushe.circle.setVisible(false);
                         ambushe.infoWindow = new google.maps.InfoWindow({
                         content:"<table><tr><td>Название:</td><td>"+ambushe.name+"</td></tr>"+
                         "<tr><td>Наемников:</td><td>"+ambushe.life+"</td></tr>"+
                         "<tr><td>Стоит:</td><td>"+ambushe.tts+"</td></tr></table>",
                         position:{lat: ambushe.lat, lng: ambushe.lng}
                         });
                         google.maps.event.addListener(ambushe.mark, 'click', function () {
                                         ambushe.infoWindow.open(map, this);
                                     });
                         ambushes.push(ambushe);

                      })
                      //Очищаем маршруты
                       if (routes!=null){
                           routes.forEach(function(currentVal,index,arr){
                              currentVal.mark.setMap(null);
                              currentVal.line.setMap(null)
                           });
                       }
                       //Рисуем маршруты
                       routes=[];
                       data.Routes.forEach(function(currentVal,index,arr){
                           var route={};
                           route.lat=currentVal.Lat/1e6;
                           route.lng=currentVal.Lng/1e6;
                           route.slat=currentVal.SLat/1e6;
                           route.slng=currentVal.SLng/1e6;
                           route.flat=currentVal.FLat/1e6;
                           route.flng=currentVal.FLng/1e6;
                           route.profit=currentVal.Profit;
                           var pic="intel/img/caravan.png";
                           var size=2;
                           size=size*(map.getZoom()+1);
                           var image = {
                               url: pic,
                               // This marker is 20 pixels wide by 32 pixels high.
                               size: new google.maps.Size(192, 192),
                               // The origin for this image is (0, 0).
                               origin: new google.maps.Point(0, 0),
                               // The anchor for this image is the base of the flagpole at (0, 32).
                               anchor: new google.maps.Point(0, 32),
                               scaledSize: new google.maps.Size(size, size)
                             };
                           route.mark = new google.maps.Marker({
                               position: {lat: route.lat, lng: route.lng},
                               title: route.name,
                               icon: image,
                               map: map
                           });
                           route.line= new google.maps.Polyline({
                                geodesic:true,
                                path:[{lat: route.slat, lng: route.slng},
                                    {lat: route.lat, lng: route.lng},
                                    {lat: route.flat, lng: route.flng}],
                                strokeColor:'#0000ff',
                                map:map
                           });

                           routes.push(route);

                        })

                } else
                console.log(xmlhttp);
            }
        }
        xmlhttp.send();
    }
}

function setPos()
{
 if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(showPosition);
    }

}

function showPosition(position) {
   lat=position.coords.latitude;
   lng=position.coords.longitude;
   map.setCenter({lat: lat, lng: lng});
}
