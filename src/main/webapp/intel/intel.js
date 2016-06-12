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
                console.log(xmlhttp.responseText);
            } else
            console.log(xmlhttp);
        }
    }
    xmlhttp.send();
}



