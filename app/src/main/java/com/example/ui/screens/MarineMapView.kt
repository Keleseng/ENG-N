package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.engine.MarineSeaRoute
import com.example.location.GpsLocationProvider
import com.example.model.Coordinate
import com.example.model.LocationPresets
import com.example.model.decimalToDmsParts
import com.example.offline.OfflineMapTileManager
import com.example.ui.TideNavViewModel
import com.example.ui.TideUiState
import com.example.ui.components.OfflineMapDialog
import com.example.ui.theme.*
import java.io.ByteArrayInputStream
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private fun serializeSeaRouteJson(route: MarineSeaRoute): String {
  val root = JSONObject()
  root.put("destLat", route.destLat)
  root.put("destLon", route.destLon)
  root.put("title", route.destinationName)
  root.put("totalDistanceNm", route.totalDistanceNm)
  root.put("speedKnots", route.speedKnots)
  root.put("etaTimeStr", route.etaTimeStr)
  root.put("fullEtaDateStr", route.fullEtaDateStr)
  root.put("durationStr", route.durationStr)
  root.put("initialBearing", route.initialBearingDegrees)

  val coordsArr = JSONArray()
  for (pt in route.pathCoordinates) {
    val pair = JSONArray()
    pair.put(pt.latitude)
    pair.put(pt.longitude)
    coordsArr.put(pair)
  }
  root.put("pathCoords", coordsArr)

  val wpsArr = JSONArray()
  for (wp in route.waypoints) {
    val wObj = JSONObject()
    wObj.put("id", wp.id)
    wObj.put("name", wp.name)
    wObj.put("desc", wp.description)
    wObj.put("lat", wp.latitude)
    wObj.put("lon", wp.longitude)
    wObj.put("legDist", wp.legDistanceNm)
    wObj.put("legBrg", wp.legBearingDegrees)
    wObj.put("nextBrg", wp.nextLegBearingDegrees)
    wObj.put("etaTimeStr", wp.etaTimeStr)
    wObj.put("durationStr", wp.durationStr)
    wObj.put("cumDist", wp.cumulativeDistanceNm)
    wpsArr.put(wObj)
  }
  root.put("turningPoints", wpsArr)
  return root.toString()
}

/**
 * JS Bridge to handle map interactions.
 */
class MarineMapJsBridge(
  private val onLocationSelected: (Double, Double) -> Unit,
  private val onDestinationPopupClosed: () -> Unit,
  private val onUserDraggedMap: () -> Unit
) {
  @JavascriptInterface
  fun onMapLocationSelected(lat: Double, lon: Double) {
    onLocationSelected(lat, lon)
  }

  @JavascriptInterface
  fun onPopupClosed() {
    onDestinationPopupClosed()
  }

  @JavascriptInterface
  fun onUserMapDrag() {
    onUserDraggedMap()
  }
}

/**
 * High-Performance Marine Map Screen with Smooth 60FPS Canvas Rendering,
 * Tile Pre-buffering, and Seamless JavaScript Bridge Interop.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarineMapView(
  uiState: TideUiState,
  viewModel: TideNavViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isDark = uiState.isDarkMode
  val coroutineScope = rememberCoroutineScope()
  val gpsProvider = remember { GpsLocationProvider(context) }

  val activeGps = uiState.lastGpsFix
  val currentLat = activeGps?.latitude
    ?: (LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.selectedPort.latitude)
  val currentLon = activeGps?.longitude
    ?: (LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.selectedPort.longitude)

  // Map Layer & Feature Toggles
  var mapTypeMode by remember { mutableStateOf("roadmap") } // "roadmap" ("harita") as default standard view
  var showSeamarksLayer by remember { mutableStateOf(true) }
  var showLighthouses by remember { mutableStateOf(true) }
  var showBuoys by remember { mutableStateOf(false) }
  var showAnchorageTss by remember { mutableStateOf(true) }
  var showDepthHazards by remember { mutableStateOf(false) }

  val offlineTileManager = remember { OfflineMapTileManager.getInstance(context) }
  val offlineDownloadState by offlineTileManager.downloadState.collectAsState()
  val forceOfflineMode by offlineTileManager.forceOfflineMode.collectAsState()
  val isDeviceOnline by offlineTileManager.isDeviceOnline.collectAsState()
  val isEffectiveOffline = forceOfflineMode || !isDeviceOnline

  var showLayersMenu by remember { mutableStateOf(false) }
  var showLegendDialog by remember { mutableStateOf(false) }
  var showOfflineMapDialog by remember { mutableStateOf(false) }

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isWebMapLoaded by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(true) }
  var loadingProgress by remember { mutableStateOf(0) }
  var hasLoadError by remember { mutableStateOf(false) }

  fun buildGoogleMapHtml(
    lat: Double,
    lon: Double,
    zoom: Int = 13
  ): String {
    val currentSpeedKnots = uiState.speedStr.toDoubleOrNull()?.takeIf { it > 0.0 }
      ?: activeGps?.speedKnots?.takeIf { it > 0.0 }
      ?: 10.0

    val initialHeading = activeGps?.bearingDegrees?.toDouble()
      ?: uiState.headingDegreesStr.toDoubleOrNull()
      ?: uiState.activeSeaRoute?.initialBearingDegrees?.toDouble()
      ?: 0.0

    val initialAutoFollow = uiState.isAutoFollowShip
    val initialHasLiveGps = activeGps != null
    val initialLyrs = when (mapTypeMode) {
      "satellite" -> "s"
      "hybrid" -> "y"
      "dark" -> "r"
      else -> "m"
    }

    val initialRoute = uiState.activeSeaRoute?.takeIf { !it.isRejectedLandTarget && it.pathCoordinates.size >= 2 }
    val initialRouteJs = if (initialRoute != null) {
      "renderSeaRouteData(${serializeSeaRouteJson(initialRoute)}, false);"
    } else ""

    val anchorResult = uiState.anchorCalculationResult
    val r1 = anchorResult.d_firstSwingingCircleMeters
    val r2 = anchorResult.f_secondSwingingCircleMeters

    val initialAnchorJs = if (uiState.anchorEvent.isAnchored) {
      "updateAnchorOverlay(true, ${uiState.anchorEvent.latitude}, ${uiState.anchorEvent.longitude}, $r1, $r2);"
    } else ""

    val initialMobJs = if (uiState.mobEvent.isActive) {
      "updateMobOverlay(true, ${uiState.mobEvent.latitude}, ${uiState.mobEvent.longitude});"
    } else ""

    return """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://marine.local/local-assets/leaflet/leaflet.css" />
        <script src="https://marine.local/local-assets/leaflet/leaflet.js"></script>
        <style>
          html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #0B132B;
            overflow: hidden;
            touch-action: pan-x pan-y;
            -webkit-tap-highlight-color: transparent;
            -webkit-user-select: none;
            user-select: none;
            transform: translateZ(0);
          }
          .leaflet-tile {
            will-change: transform;
            transform: translateZ(0);
            -webkit-backface-visibility: hidden;
            backface-visibility: hidden;
          }
          .leaflet-container {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            touch-action: pan-x pan-y;
            -webkit-user-select: none;
            user-select: none;
            background: #0B132B !important;
          }
          .leaflet-zoom-anim .leaflet-zoom-animated {
            will-change: transform;
            -webkit-transition: -webkit-transform 250ms cubic-bezier(0, 0, 0.25, 1);
            transition: transform 250ms cubic-bezier(0, 0, 0.25, 1);
          }
          .leaflet-marker-icon {
            will-change: transform;
          }
          .leaflet-control-attribution, .leaflet-bottom.leaflet-right {
            display: none !important;
          }
          .leaflet-popup-content-wrapper {
            background: rgba(15, 23, 42, 0.96);
            color: #FFFFFF;
            border-radius: 10px;
            border: 1px solid #38BDF8;
            font-size: 12px;
            box-shadow: 0 4px 16px rgba(0,0,0,0.6);
            backdrop-filter: blur(4px);
          }
          .leaflet-popup-tip {
            background: rgba(15, 23, 42, 0.96);
          }
          .leaflet-container a.leaflet-popup-close-button {
            color: #94A3B8;
            padding: 4px;
            font-size: 18px;
            font-weight: bold;
          }
          .leaflet-container a.leaflet-popup-close-button:hover {
            color: #EF4444;
          }
          @keyframes flashBeacon {
            0%, 100% { opacity: 0.3; transform: scale(0.9); }
            50% { opacity: 0.95; transform: scale(1.15); }
          }
          @keyframes pulseRing {
            0% { transform: scale(0.88); opacity: 0.85; }
            100% { transform: scale(1.55); opacity: 0; }
          }
        </style>
      </head>
      <body>
        <div id="map"></div>
        <script>
          var map, shipMarker, destMarker = null, routeLine = null;
          var routeCorridorBand = null, routeOutlineLine = null;
          var passedRouteOutline = null, passedRouteLine = null;
          var activeRouteCoords = [];
          var lastChevronUpdateLat = null, lastChevronUpdateLon = null;
          var turningPointMarkers = [];
          var legDirectionMarkers = [];
          var lighthouseLayerGroup = L.layerGroup();
          var buoyLayerGroup = L.layerGroup();
          var anchorageTssLayerGroup = L.layerGroup();
          var depthHazardLayerGroup = L.layerGroup();
          
          var googleTileLayer = null;
          var seamarkTileLayer = null;
          
          var rawShipLat = $lat;
          var rawShipLon = $lon;
          var currentDisplayLat = $lat;
          var currentDisplayLon = $lon;
          var shipSpeed = $currentSpeedKnots;
          var rawShipHeading = $initialHeading;
          var currentDisplayHeading = $initialHeading;
          var isAutoFollow = $initialAutoFollow;
          var hasLiveGpsFix = $initialHasLiveGps;
          var isProgrammaticPan = false;
          var animFrameId = null;
          var isUpdatingDestination = false;
          var anchorCenterMarker = null, anchorOuterCircle = null, anchorInnerCircle = null;
          var mobMarker = null;

          document.addEventListener('DOMContentLoaded', function() {
            initMap();
          });

          function calcBearingDeg(lat1, lon1, lat2, lon2) {
            var phi1 = lat1 * Math.PI / 180.0;
            var phi2 = lat2 * Math.PI / 180.0;
            var dLam = (lon2 - lon1) * Math.PI / 180.0;
            var y = Math.sin(dLam) * Math.cos(phi2);
            var x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLam);
            return ((Math.atan2(y, x) * 180.0 / Math.PI) + 360.0) % 360.0;
          }

          function shortestAngleDelta(fromDeg, toDeg) {
            var diff = ((toDeg - fromDeg) % 360.0 + 540.0) % 360.0 - 180.0;
            return diff;
          }

          function projectOntoRoute(lat, lon, coords) {
            if (!coords || coords.length < 2) return null;
            var cosLat = Math.cos(lat * Math.PI / 180.0);
            if (Math.abs(cosLat) < 0.01) cosLat = 0.01;
            var best = null;
            for (var i = 0; i < coords.length - 1; i++) {
              var lat1 = coords[i][0], lon1 = coords[i][1];
              var lat2 = coords[i + 1][0], lon2 = coords[i + 1][1];
              var ax = (lon1 - lon) * cosLat * 60.0;
              var ay = (lat1 - lat) * 60.0;
              var bx = (lon2 - lon) * cosLat * 60.0;
              var by = (lat2 - lat) * 60.0;
              var dx = bx - ax;
              var dy = by - ay;
              var lenSq = dx * dx + dy * dy;
              var t = 0.0;
              if (lenSq > 1e-10) {
                t = -(ax * dx + ay * dy) / lenSq;
                if (t < 0.0) t = 0.0;
                else if (t > 1.0) t = 1.0;
              }
              var px = ax + t * dx;
              var py = ay + t * dy;
              var distNm = Math.hypot(px, py);
              if (!best || distNm < best.distNm) {
                var pLat = lat1 + t * (lat2 - lat1);
                var pLon = lon1 + t * (lon2 - lon1);
                var brg = calcBearingDeg(lat1, lon1, lat2, lon2);
                best = {
                  projLat: pLat,
                  projLon: pLon,
                  distNm: distNm,
                  segIndex: i,
                  t: t,
                  segBearing: brg
                };
              }
            }
            return best;
          }

          function initMap() {
            if (map) return;
            map = L.map('map', {
              preferCanvas: true,
              zoomControl: false,
              attributionControl: false,
              closePopupOnClick: false,
              zoomAnimation: true,
              zoomAnimationThreshold: 4,
              fadeAnimation: true,
              markerZoomAnimation: true,
              inertia: true,
              inertiaDeceleration: 3000,
              bounceAtZoomLimits: false,
              wheelDebounceTime: 40,
              worldCopyJump: false
            }).setView([rawShipLat, rawShipLon], $zoom);

            googleTileLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=$initialLyrs&x={x}&y={y}&z={z}', {
              maxZoom: 20,
              maxNativeZoom: 19,
              subdomains: ['mt0','mt1','mt2','mt3'],
              keepBuffer: 6,
              updateWhenIdle: false,
              updateWhenZooming: false,
              crossOrigin: true
            }).addTo(map);

            seamarkTileLayer = L.tileLayer('https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png', {
              maxZoom: 18,
              opacity: 0.95,
              keepBuffer: 6,
              updateWhenIdle: false,
              updateWhenZooming: false,
              crossOrigin: true
            }).addTo(map);

            lighthouseLayerGroup.addTo(map);
            anchorageTssLayerGroup.addTo(map);

            loadMarineNavigationalAids();

            var shipIcon = L.divIcon({
              className: 'custom-ship-marker-wrapper',
              html: createShipMarkerHtml(currentDisplayHeading),
              iconSize: [48, 48],
              iconAnchor: [24, 24]
            });
            
            shipMarker = L.marker([currentDisplayLat, currentDisplayLon], {
              icon: shipIcon,
              zIndexOffset: 3000
            }).addTo(map);
            shipMarker.bindPopup('🧭 <b>Mevcut Araç / Gemi Konumu</b><br>' + rawShipLat.toFixed(5) + '°, ' + rawShipLon.toFixed(5) + '°');

            map.on('dragstart', function() {
              if (!isProgrammaticPan) {
                isAutoFollow = false;
                if (window.AndroidBridge && window.AndroidBridge.onUserMapDrag) {
                  window.AndroidBridge.onUserMapDrag();
                }
              }
            });

            map.on('click', function(e) {
              if (window.AndroidBridge && window.AndroidBridge.onMapLocationSelected) {
                window.AndroidBridge.onMapLocationSelected(e.latlng.lat, e.latlng.lng);
              }
            });

            $initialRouteJs
            $initialAnchorJs
            $initialMobJs
          }

          // Yandex Haritalar / Yandex Navigator tarzı 3B Araç İmleci (Tam Merkezli: 48x48, Anchor 24x24)
          function createShipMarkerHtml(headingDeg) {
            return '<div style="position:relative; width:48px; height:48px; display:flex; align-items:center; justify-content:center; pointer-events:auto;">' +
                   '<div style="position:absolute; left:2px; top:2px; width:44px; height:44px; border-radius:50%; background:rgba(25,109,255,0.20); border:1.5px solid rgba(25,109,255,0.48); animation:pulseRing 2.4s infinite ease-out; pointer-events:none;"></div>' +
                   '<div style="position:relative; width:34px; height:34px; border-radius:50%; background:#FFFFFF; border:2px solid #E2E8F0; box-shadow:0 3px 10px rgba(9,34,82,0.58); display:flex; align-items:center; justify-content:center;">' +
                   '<div id="ship-heading-icon" style="width:26px; height:26px; transform:rotate(' + headingDeg.toFixed(1) + 'deg); transform-origin:50% 50%; display:flex; align-items:center; justify-content:center;">' +
                   '<svg width="26" height="26" viewBox="0 0 26 26" fill="none" xmlns="http://www.w3.org/2000/svg">' +
                   '<path d="M13 2.2 L4.5 21.5 L13 17.0 Z" fill="#FF3B30"/>' +
                   '<path d="M13 2.2 L21.5 21.5 L13 17.0 Z" fill="#D70015"/>' +
                   '<path d="M13 2.2 L21.5 21.5 L13 17.0 L4.5 21.5 Z" stroke="#092252" stroke-opacity="0.22" stroke-width="0.8" stroke-linejoin="round"/>' +
                   '<circle cx="13" cy="13" r="2.0" fill="#FFFFFF"/>' +
                   '</svg>' +
                   '</div>' +
                   '</div>' +
                   '</div>';
          }

          function setMapStyle(styleMode) {
            if (!googleTileLayer || !map) return;
            var lyr = 'm';
            if (styleMode === 'roadmap') lyr = 'm';
            else if (styleMode === 'satellite') lyr = 's';
            else if (styleMode === 'hybrid') lyr = 'y';
            else if (styleMode === 'dark') lyr = 'r';

            googleTileLayer.setUrl('https://mt1.google.com/vt/lyrs=' + lyr + '&x={x}&y={y}&z={z}');
          }
          window.setMapStyle = setMapStyle;

          function toggleSeamarksLayer(visible) {
            if (!map || !seamarkTileLayer) return;
            if (visible) {
              if (!map.hasLayer(seamarkTileLayer)) map.addLayer(seamarkTileLayer);
            } else {
              if (map.hasLayer(seamarkTileLayer)) map.removeLayer(seamarkTileLayer);
            }
          }
          window.toggleSeamarksLayer = toggleSeamarksLayer;

          function toggleCategoryLayer(category, visible) {
            if (!map) return;
            var layer = null;
            if (category === 'lighthouses') layer = lighthouseLayerGroup;
            else if (category === 'buoys') layer = buoyLayerGroup;
            else if (category === 'anchorage') layer = anchorageTssLayerGroup;
            else if (category === 'hazards') layer = depthHazardLayerGroup;

            if (layer) {
              if (visible) { if (!map.hasLayer(layer)) map.addLayer(layer); }
              else { if (map.hasLayer(layer)) map.removeLayer(layer); }
            }
          }
          window.toggleCategoryLayer = toggleCategoryLayer;

          // Load rich Turkish and Mediterranean Marine Navigational Aids
          function loadMarineNavigationalAids() {
            var lighthouses = [
              { lat: 41.0078, lon: 28.9839, name: "Ahırkapı Feneri", char: "Fl.W.5s 16M 36m", desc: "İstanbul Boğazı Marmara Girişi Ana Deniz Feneri. Sektörlü beyaz ışık.", color: "#FACC15" },
              { lat: 41.2333, lon: 29.1108, name: "Rumeli Feneri", char: "Fl.W.12s 18M 58m", desc: "Karadeniz İstanbul Boğazı Batı Giriş Feneri.", color: "#FACC15" },
              { lat: 41.2189, lon: 29.1481, name: "Anadolu Feneri", char: "Fl.W.20s 20M 75m", desc: "Karadeniz İstanbul Boğazı Doğu Giriş Feneri.", color: "#FACC15" },
              { lat: 41.0211, lon: 29.0041, name: "Kız Kulesi Feneri", char: "Fl.W.3s 10M 15m", desc: "İstanbul Boğazı Salacak Geçiş Feneri.", color: "#FACC15" },
              { lat: 40.4089, lon: 26.6908, name: "Gelibolu Feneri", char: "Fl.W.10s 15M 34m", desc: "Çanakkale Boğazı Kuzey Giriş Feneri.", color: "#FACC15" },
              { lat: 40.0436, lon: 26.1772, name: "Mehmetçik Feneri", char: "Fl.W.5s 19M 50m", desc: "Çanakkale Boğazı Ege Girişi (Seddülbahir).", color: "#FACC15" },
              { lat: 39.4792, lon: 26.0622, name: "Babakale Feneri", char: "Fl.W.5s 12M 32m", desc: "Asya Kıtasının En Batı Ucu Deniz Feneri.", color: "#FACC15" },
              { lat: 38.6739, lon: 26.7531, name: "Foça Feneri", char: "Fl.W.5s 14M 25m", desc: "İzmir Körfezi Dış Geçiş Feneri.", color: "#FACC15" },
              { lat: 38.6578, lon: 26.3639, name: "Sarpıncık Feneri", char: "Fl.W.12s 16M 97m", desc: "Karaburun İzmir Körfezi Giriş Feneri.", color: "#FACC15" },
              { lat: 37.0311, lon: 27.4244, name: "Bodrum Kale Feneri", char: "Fl.W.5s 12M 15m", desc: "Bodrum Limanı ve İstanköy Kanalı Feneri.", color: "#FACC15" },
              { lat: 36.6858, lon: 27.3756, name: "Deveboynu (Knidos) Feneri", char: "Fl.W.10s 14M 104m", desc: "Datça Yarımadası Ege/Akdeniz Ayrım Feneri.", color: "#FACC15" },
              { lat: 36.1408, lon: 30.4078, name: "Gelidonya Feneri", char: "Fl.W.3s 15M 227m", desc: "Taşlık Burnu Antalya Körfezi Giriş Feneri.", color: "#FACC15" },
              { lat: 36.8528, lon: 30.7075, name: "Baba Burnu Feneri", char: "Fl.W.5s 18M 30m", desc: "Antalya Falezler Deniz Feneri.", color: "#FACC15" },
              { lat: 36.5364, lon: 31.9908, name: "Alanya Feneri", char: "Fl.W.6s 15M 209m", desc: "Alanya Kale İçi Deniz Feneri.", color: "#FACC15" },
              { lat: 36.0153, lon: 32.8028, name: "Anamur Feneri", char: "Fl.W.10s 18M 78m", desc: "Türkiye Kıbrıs Kanalı Ana Seyir Feneri.", color: "#FACC15" },
              { lat: 36.7725, lon: 34.6369, name: "Mersin Feneri", char: "Fl.W.10s 15M 16m", desc: "Mersin Liman Yaklaşma Feneri.", color: "#FACC15" },
              { lat: 42.0125, lon: 34.9389, name: "Sinop İnceburun Feneri", char: "Fl.W.20s 18M 38m", desc: "Türkiye'nin En Kuzey Ucu Deniz Feneri.", color: "#FACC15" },
              { lat: 41.1783, lon: 29.6106, name: "Şile Feneri", char: "Fl.W.15s 20M 60m", desc: "Tarihi Şile Karadeniz Feneri.", color: "#FACC15" },
              { lat: 41.7483, lon: 32.3831, name: "Amasra Feneri", char: "Fl.W.10s 12M 77m", desc: "Amasra Karadeniz Liman Feneri.", color: "#FACC15" }
            ];

            lighthouses.forEach(function(lh) {
              var lhIcon = L.divIcon({
                className: 'custom-lighthouse-icon',
                html: '<div style="position:relative; width:30px; height:30px; display:flex; align-items:center; justify-content:center;">' +
                      '<div style="position:absolute; width:28px; height:28px; border-radius:50%; background:rgba(250,204,21,0.25); border:1px dashed #FACC15; animation:flashBeacon 3s infinite ease-in-out;"></div>' +
                      '<div style="width:18px; height:18px; background:#0F172A; border:2px solid #FACC15; border-radius:50%; display:flex; align-items:center; justify-content:center; box-shadow:0 0 8px rgba(250,204,21,0.9); z-index:10;">' +
                      '<span style="font-size:10px;">🚨</span>' +
                      '</div>' +
                      '</div>',
                iconSize: [30, 30],
                iconAnchor: [15, 15]
              });
              var m = L.marker([lh.lat, lh.lon], { icon: lhIcon });
              m.bindPopup('<div style="line-height:1.4; padding:2px;">' +
                '<b style="color:#FACC15; font-size:12.5px;">⚓ ' + lh.name + '</b><br>' +
                '<span style="color:#38BDF8; font-weight:bold; font-family:monospace; font-size:11px;">💡 ' + lh.char + '</span><br>' +
                '<span style="color:#CBD5E1; font-size:10.5px;">' + lh.desc + '</span><br>' +
                '<span style="color:#94A3B8; font-size:9.5px;">Mevki: ' + lh.lat.toFixed(4) + '°K, ' + lh.lon.toFixed(4) + '°D</span>' +
                '</div>');
              lighthouseLayerGroup.addLayer(m);
            });

            var buoys = [
              { lat: 40.9789, lon: 28.9611, name: "Ahırkapı Sancak Fenerli Şamandırası", type: "sancak", char: "Fl.G.3s", desc: "Sancak Tarafı Geçiş Şamandırası (Yeşil Koni)" },
              { lat: 40.9822, lon: 28.9712, name: "Ahırkapı İskele Fenerli Şamandırası", type: "iskele", char: "Fl.R.3s", desc: "İskele Tarafı Geçiş Şamandırası (Kırmızı Silindir)" },
              { lat: 41.0025, lon: 28.9815, name: "Kuzey Kardinal Şamandırası (Ahırkapı Sığlık)", type: "kuzey_kardinal", char: "Q.W (Sürekli Çakar)", desc: "Kuzey Kardinal: Emniyetli Geçiş Kuzeydedir." },
              { lat: 40.9650, lon: 29.0210, name: "Kınalıada Doğu Kardinal Şamandırası", type: "dogu_kardinal", char: "Q(3).W 10s", desc: "Doğu Kardinal: Emniyetli Geçiş Doğudadır." },
              { lat: 40.8711, lon: 29.1189, name: "Hayırsızada Tecrit Edilmiş Tehlike Şamandırası", type: "tehlike", char: "Fl(2).W 5s", desc: "Tecrit Edilmiş Sığ Sualtı Kayalığı!" },
              { lat: 41.2400, lon: 29.1250, name: "Karadeniz Emniyetli Su Şamandırası", type: "emniyetli", char: "Iso.W.4s", desc: "Emniyetli Açık Deniz Su Kanalı (Kırmızı/Beyaz Çizgili)" },
              { lat: 40.0310, lon: 26.1620, name: "Karanlık Liman İskele Şamandırası", type: "iskele", char: "Fl.R.4s", desc: "Çanakkale Boğazı İskele Şamandırası" },
              { lat: 40.0380, lon: 26.1750, name: "Karanlık Liman Sancak Şamandırası", type: "sancak", char: "Fl.G.4s", desc: "Çanakkale Boğazı Sancak Şamandırası" }
            ];

            buoys.forEach(function(b) {
              var colorBg = "#EF4444";
              var symbolStr = "🚩";
              if (b.type === "sancak") { colorBg = "#10B981"; symbolStr = "🟢"; }
              else if (b.type === "iskele") { colorBg = "#EF4444"; symbolStr = "🔴"; }
              else if (b.type === "kuzey_kardinal") { colorBg = "#FACC15"; symbolStr = "▲▲"; }
              else if (b.type === "dogu_kardinal") { colorBg = "#FACC15"; symbolStr = "▲▼"; }
              else if (b.type === "tehlike") { colorBg = "#DC2626"; symbolStr = "⚠️"; }
              else if (b.type === "emniyetli") { colorBg = "#3B82F6"; symbolStr = "⚪🔴"; }

              var buoyIcon = L.divIcon({
                className: 'custom-buoy-icon',
                html: '<div style="position:relative; width:24px; height:24px; display:flex; align-items:center; justify-content:center;">' +
                      '<div style="width:16px; height:16px; background:' + colorBg + '; border:1.5px solid #FFFFFF; border-radius:4px; display:flex; align-items:center; justify-content:center; box-shadow:0 2px 6px rgba(0,0,0,0.5); font-size:8px; font-weight:bold; color:white;">' +
                      symbolStr +
                      '</div>' +
                      '</div>',
                iconSize: [24, 24],
                iconAnchor: [12, 12]
              });
              var bm = L.marker([b.lat, b.lon], { icon: buoyIcon });
              bm.bindPopup('<div style="line-height:1.4; padding:2px;">' +
                '<b style="color:#38BDF8; font-size:12px;">⚓ ' + b.name + '</b><br>' +
                '<span style="color:#FACC15; font-weight:bold; font-size:11px;">Işık: ' + b.char + '</span><br>' +
                '<span style="color:#CBD5E1; font-size:10.5px;">' + b.desc + '</span>' +
                '</div>');
              buoyLayerGroup.addLayer(bm);
            });

            var ahirkapiAnchorage = L.polygon([
              [40.975, 28.950],
              [40.975, 28.995],
              [40.940, 28.995],
              [40.940, 28.950]
            ], {
              color: '#3B82F6',
              weight: 1.8,
              dashArray: '6, 6',
              fillColor: '#3B82F6',
              fillOpacity: 0.12
            });
            ahirkapiAnchorage.bindPopup('<b>⚓ Ahırkapı Demir Sahası (İstanbul)</b><br>Ticari Gemiler Demirleme & Bekleme Bölgesi');
            anchorageTssLayerGroup.addLayer(ahirkapiAnchorage);

            var tssIstanbul = L.polyline([
              [40.970, 28.980],
              [41.015, 29.000],
              [41.060, 29.050],
              [41.120, 29.070],
              [41.220, 29.130]
            ], {
              color: '#F59E0B',
              weight: 2.2,
              dashArray: '8, 8',
              opacity: 0.8
            });
            tssIstanbul.bindPopup('<b>🚢 İstanbul Boğazı TSS Trafik Ayrım Koridoru</b><br>Uluslararası Deniz Trafik Şeridi');
            anchorageTssLayerGroup.addLayer(tssIstanbul);

            var shallowBelek = L.circle([41.0180, 28.9950], {
              radius: 400,
              color: '#EF4444',
              fillColor: '#EF4444',
              fillOpacity: 0.2,
              weight: 1.5,
              dashArray: '4, 4'
            });
            shallowBelek.bindPopup('<b>⚠️ SIĞLIK UYARISI: Salacak Sığlığı</b><br>Derinlik < 4.5 Metre. Diğer Su Çekimli Gemiler Dikkat!');
            depthHazardLayerGroup.addLayer(shallowBelek);
          }

          function clearRouteChevrons() {
            if (legDirectionMarkers && legDirectionMarkers.length > 0) {
              legDirectionMarkers.forEach(function(m) { map.removeLayer(m); });
              legDirectionMarkers = [];
            }
          }

          // Yandex Haritalar stili mavi rota şeridinin tam ortasında ilerleyen beyaz yön okları
          function renderRouteChevrons(aheadCoords) {
            clearRouteChevrons();
            if (!aheadCoords || aheadCoords.length < 2) return;

            var segLengths = [];
            var totalNm = 0.0;
            for (var i = 0; i < aheadCoords.length - 1; i++) {
              var lat1 = aheadCoords[i][0], lon1 = aheadCoords[i][1];
              var lat2 = aheadCoords[i + 1][0], lon2 = aheadCoords[i + 1][1];
              var meanCos = Math.cos(((lat1 + lat2) * 0.5) * Math.PI / 180.0);
              var nm = Math.hypot((lat2 - lat1) * 60.0, (lon2 - lon1) * meanCos * 60.0);
              segLengths.push(nm);
              totalNm += nm;
            }
            if (totalNm < 0.25) return;

            var stepNm = Math.max(0.45, totalNm / 16.0);
            var targetDist = Math.min(stepNm * 0.65, totalNm * 0.5);
            var accBefore = 0.0;
            var count = 0;

            for (var s = 0; s < segLengths.length && count < 24; s++) {
              var sLen = segLengths[s];
              if (sLen <= 1e-6) continue;
              var aLat = aheadCoords[s][0], aLon = aheadCoords[s][1];
              var bLat = aheadCoords[s + 1][0], bLon = aheadCoords[s + 1][1];
              var brgDeg = calcBearingDeg(aLat, aLon, bLat, bLon);

              while (targetDist <= accBefore + sLen && count < 24) {
                var frac = (targetDist - accBefore) / sLen;
                if (frac >= 0.02 && frac <= 0.98) {
                  var cLat = aLat + frac * (bLat - aLat);
                  var cLon = aLon + frac * (bLon - aLon);
                  var arrowIcon = L.divIcon({
                    className: 'custom-leg-arrow',
                    html: '<div style="width:14px; height:14px; transform:rotate(' + brgDeg.toFixed(1) + 'deg); transform-origin:50% 50%; display:flex; align-items:center; justify-content:center; pointer-events:none;">' +
                          '<svg width="12" height="12" viewBox="0 0 12 12" xmlns="http://www.w3.org/2000/svg">' +
                          '<path d="M6 1.2 L10.8 9.8 L6 7.4 L1.2 9.8 Z" fill="#FFFFFF" stroke="#092252" stroke-width="1.0" stroke-linejoin="round"/>' +
                          '</svg>' +
                          '</div>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                  });
                  var arrowM = L.marker([cLat, cLon], {
                    icon: arrowIcon,
                    interactive: false,
                    zIndexOffset: 900
                  }).addTo(map);
                  legDirectionMarkers.push(arrowM);
                  count++;
                }
                targetDist += stepNm;
              }
              accBefore += sLen;
            }
          }

          function updateRouteSplitVisuals(proj, isSnapped, forceChevrons) {
            if (!map || !activeRouteCoords || activeRouteCoords.length < 2) return;

            var passedCoords = [];
            var aheadCoords = [];

            if (isSnapped && proj) {
              if (proj.segIndex > 0 || proj.t > 0.002) {
                passedCoords = activeRouteCoords.slice(0, proj.segIndex + 1);
                passedCoords.push([proj.projLat, proj.projLon]);
              }
              aheadCoords = [[proj.projLat, proj.projLon]].concat(activeRouteCoords.slice(proj.segIndex + 1));
            } else {
              aheadCoords = activeRouteCoords.slice();
            }

            if (passedCoords.length >= 2) {
              if (passedRouteOutline) {
                passedRouteOutline.setLatLngs(passedCoords);
              } else {
                passedRouteOutline = L.polyline(passedCoords, {
                  color: '#475569', weight: 12, opacity: 0.85, lineCap: 'round', lineJoin: 'round', interactive: false
                }).addTo(map);
              }
              if (passedRouteLine) {
                passedRouteLine.setLatLngs(passedCoords);
              } else {
                passedRouteLine = L.polyline(passedCoords, {
                  color: '#94A3B8', weight: 8, opacity: 0.92, lineCap: 'round', lineJoin: 'round', interactive: false
                }).addTo(map);
              }
            } else {
              if (passedRouteOutline) { map.removeLayer(passedRouteOutline); passedRouteOutline = null; }
              if (passedRouteLine) { map.removeLayer(passedRouteLine); passedRouteLine = null; }
            }

            if (aheadCoords.length >= 2) {
              if (routeCorridorBand) {
                routeCorridorBand.setLatLngs(aheadCoords);
              } else {
                routeCorridorBand = L.polyline(aheadCoords, {
                  color: '#3B82F6', weight: 20, opacity: 0.18, lineCap: 'round', lineJoin: 'round', interactive: false
                }).addTo(map);
              }
              if (routeOutlineLine) {
                routeOutlineLine.setLatLngs(aheadCoords);
              } else {
                routeOutlineLine = L.polyline(aheadCoords, {
                  color: '#092252', weight: 14, opacity: 0.96, lineCap: 'round', lineJoin: 'round', interactive: false
                }).addTo(map);
              }
              if (routeLine) {
                routeLine.setLatLngs(aheadCoords);
                routeLine.setStyle({ color: '#196DFF', weight: 10, opacity: 1.0, dashArray: null, lineCap: 'round', lineJoin: 'round' });
              } else {
                routeLine = L.polyline(aheadCoords, {
                  color: '#196DFF', weight: 10, opacity: 1.0, lineCap: 'round', lineJoin: 'round', interactive: false
                }).addTo(map);
              }

              var movedSinceChevrons = 999.0;
              if (lastChevronUpdateLat !== null && lastChevronUpdateLon !== null) {
                var cLatRef = aheadCoords[0][0], cLonRef = aheadCoords[0][1];
                movedSinceChevrons = Math.hypot((cLatRef - lastChevronUpdateLat) * 60.0, (cLonRef - lastChevronUpdateLon) * 45.0);
              }
              if (forceChevrons || movedSinceChevrons >= 0.08) {
                lastChevronUpdateLat = aheadCoords[0][0];
                lastChevronUpdateLon = aheadCoords[0][1];
                renderRouteChevrons(aheadCoords);
              }
            }
          }

          function applyVehicleVisualState(lat, lon, headingDeg, proj, isSnapped, forceChevrons) {
            currentDisplayLat = lat;
            currentDisplayLon = lon;
            currentDisplayHeading = ((headingDeg % 360.0) + 360.0) % 360.0;

            if (shipMarker) {
              shipMarker.setLatLng([currentDisplayLat, currentDisplayLon]);
              shipMarker.setZIndexOffset(3000);
            }
            var headingEl = document.getElementById('ship-heading-icon');
            if (headingEl) {
              headingEl.style.transform = 'rotate(' + currentDisplayHeading.toFixed(1) + 'deg)';
            }
            if (activeRouteCoords && activeRouteCoords.length >= 2) {
              var snapProj = isSnapped ? projectOntoRoute(currentDisplayLat, currentDisplayLon, activeRouteCoords) : proj;
              updateRouteSplitVisuals(snapProj, isSnapped, forceChevrons);
            }
          }

          function syncVehicleWithRoute(animate, panIfFollowing) {
            if (!map) return;
            var targetLat = rawShipLat;
            var targetLon = rawShipLon;
            var targetHeading = rawShipHeading;
            var proj = null;
            var isSnapped = false;

            if (activeRouteCoords && activeRouteCoords.length >= 2) {
              proj = projectOntoRoute(rawShipLat, rawShipLon, activeRouteCoords);
              if (proj && ((!hasLiveGpsFix && proj.distNm <= 1.6) || proj.distNm <= 0.85)) {
                isSnapped = true;
                targetLat = proj.projLat;
                targetLon = proj.projLon;
                var headingDiff = Math.abs(shortestAngleDelta(rawShipHeading, proj.segBearing));
                if (!hasLiveGpsFix || shipSpeed < 1.5 || headingDiff <= 105.0) {
                  targetHeading = proj.segBearing;
                }
              }
            }

            if (animFrameId) {
              cancelAnimationFrame(animFrameId);
              animFrameId = null;
            }

            var distJumpNm = Math.hypot((targetLat - currentDisplayLat) * 60.0, (targetLon - currentDisplayLon) * 45.0);
            if (!animate || distJumpNm > 3.0 || distJumpNm < 0.0002) {
              applyVehicleVisualState(targetLat, targetLon, targetHeading, proj, isSnapped, true);
              if (panIfFollowing && isAutoFollow) {
                isProgrammaticPan = true;
                map.panTo([targetLat, targetLon], { animate: true, duration: 0.4 });
                setTimeout(function() { isProgrammaticPan = false; }, 450);
              }
              return;
            }

            var startLat = currentDisplayLat;
            var startLon = currentDisplayLon;
            var startHeading = currentDisplayHeading;
            var deltaHeading = shortestAngleDelta(startHeading, targetHeading);
            var durationMs = 420.0;
            var startTime = performance.now();

            function stepFrame(now) {
              var elapsed = now - startTime;
              var u = Math.min(1.0, Math.max(0.0, elapsed / durationMs));
              var ease = u < 0.5 ? 2.0 * u * u : 1.0 - Math.pow(-2.0 * u + 2.0, 2) / 2.0;
              var iLat = startLat + (targetLat - startLat) * ease;
              var iLon = startLon + (targetLon - startLon) * ease;
              var iHeading = startHeading + deltaHeading * ease;

              applyVehicleVisualState(iLat, iLon, iHeading, proj, isSnapped, u >= 1.0);

              if (u < 1.0) {
                animFrameId = requestAnimationFrame(stepFrame);
              } else {
                animFrameId = null;
                if (panIfFollowing && isAutoFollow) {
                  isProgrammaticPan = true;
                  map.panTo([targetLat, targetLon], { animate: true, duration: 0.35 });
                  setTimeout(function() { isProgrammaticPan = false; }, 400);
                }
              }
            }
            animFrameId = requestAnimationFrame(stepFrame);
          }

          function clearDestinationAndRoute(notifyBridge) {
            activeRouteCoords = [];
            lastChevronUpdateLat = null;
            lastChevronUpdateLon = null;
            if (turningPointMarkers && turningPointMarkers.length > 0) {
              turningPointMarkers.forEach(function(m) { map.removeLayer(m); });
              turningPointMarkers = [];
            }
            clearRouteChevrons();
            if (passedRouteOutline) { map.removeLayer(passedRouteOutline); passedRouteOutline = null; }
            if (passedRouteLine) { map.removeLayer(passedRouteLine); passedRouteLine = null; }
            if (routeCorridorBand) { map.removeLayer(routeCorridorBand); routeCorridorBand = null; }
            if (routeOutlineLine) { map.removeLayer(routeOutlineLine); routeOutlineLine = null; }
            if (routeLine) { map.removeLayer(routeLine); routeLine = null; }
            if (destMarker) { map.removeLayer(destMarker); destMarker = null; }
            syncVehicleWithRoute(false, false);
            if (notifyBridge && window.AndroidBridge && window.AndroidBridge.onPopupClosed) {
              window.AndroidBridge.onPopupClosed();
            }
          }
          window.clearDestinationAndRoute = clearDestinationAndRoute;

          window.updateAnchorOverlay = function(active, lat, lon, r1, r2) {
            if (anchorOuterCircle) { map.removeLayer(anchorOuterCircle); anchorOuterCircle = null; }
            if (anchorInnerCircle) { map.removeLayer(anchorInnerCircle); anchorInnerCircle = null; }
            if (anchorCenterMarker) { map.removeLayer(anchorCenterMarker); anchorCenterMarker = null; }

            if (active && lat && lon) {
              anchorInnerCircle = L.circle([lat, lon], {
                radius: r1 || 50, color: '#10B981', fillColor: '#10B981', fillOpacity: 0.15, weight: 1.5
              }).addTo(map);

              anchorOuterCircle = L.circle([lat, lon], {
                radius: r2 || 100, color: '#EF4444', fillColor: '#EF4444', fillOpacity: 0.08, weight: 1.5, dashArray: '5, 5'
              }).addTo(map);

              var anchorIcon = L.divIcon({
                className: 'custom-anchor-center-icon',
                html: '<div style="position:relative; display:flex; align-items:center; justify-content:center; width:20px; height:20px;">' +
                      '<div style="background: rgba(15, 23, 42, 0.9); border: 1.5px solid #10B981; color: #34D399; padding: 2px 6px; border-radius: 4px; font-weight: bold; font-family: sans-serif; font-size: 10px; white-space: nowrap; transform: translate(-50%, -100%); margin-top: -12px; position: absolute; left: 50%; top: 0;">⚓ DEMİR</div>' +
                      '<div style="width: 10px; height: 10px; background: #10B981; border: 2px solid white; border-radius: 50%; box-shadow: 0 1px 4px rgba(0,0,0,0.4);"></div>' +
                      '</div>',
                iconSize: [20, 20],
                iconAnchor: [10, 10]
              });
              anchorCenterMarker = L.marker([lat, lon], { icon: anchorIcon }).addTo(map);
            }
          };

          window.updateMobOverlay = function(active, lat, lon) {
            if (mobMarker) { map.removeLayer(mobMarker); mobMarker = null; }

            if (active && lat && lon) {
              var mobIcon = L.divIcon({
                className: 'custom-mob-icon',
                html: '<div style="position:relative; width:44px; height:44px; display:flex; align-items:center; justify-content:center;">' +
                      '<div style="position:absolute; width:40px; height:32px; background:#DC2626; border:2px solid white; border-radius:4px; display:flex; align-items:center; justify-content:center; box-shadow: 0 2px 8px rgba(0,0,0,0.5); z-index:100;">' +
                      '<span style="color:white; font-weight:black; font-family:sans-serif; font-size:10px; letter-spacing:0.5px;">🚨 MOB</span>' +
                      '</div>' +
                      '<div style="position:absolute; width:44px; height:44px; border-radius:50%; background:rgba(220,38,38,0.25); border:1.5px dashed #DC2626; animation:pulseRing 2s infinite ease-out;"></div>' +
                      '</div>',
                iconSize: [44, 44],
                iconAnchor: [22, 22]
              });
              mobMarker = L.marker([lat, lon], { icon: mobIcon, zIndexOffset: 3500 }).addTo(map);
              mobMarker.bindPopup('🚨 <b>DENİZE ADAM DÜŞTÜ (MOB)</b><br>Mevki: ' + lat.toFixed(5) + '°, ' + lon.toFixed(5) + '°<br><span style="color:#EF4444; font-weight:bold;">ACİL DURUM!</span>');
              isProgrammaticPan = true;
              map.panTo([lat, lon], { animate: true, duration: 0.5 });
              setTimeout(function() { isProgrammaticPan = false; }, 550);
            }
          };

          function toDms(val, isLat) {
            var abs = Math.abs(val);
            var deg = Math.floor(abs);
            var minFloat = (abs - deg) * 60;
            var min = Math.floor(minFloat);
            var sec = ((minFloat - min) * 60).toFixed(1);
            var dir = isLat ? (val >= 0 ? 'K' : 'G') : (val >= 0 ? 'D' : 'B');
            return deg + '°' + (min < 10 ? '0' : '') + min + '\'' + (sec < 10 ? '0' : '') + sec + '"' + dir;
          }

          var destIcon = L.divIcon({
            className: 'custom-dest-marker',
            html: '<div style="position:relative; width:32px; height:32px;">' +
                  '<div style="position:absolute; left:1px; top:1px; width:30px; height:30px; border-radius:50% 50% 50% 0; background:#EF4444; transform:rotate(-45deg); border:2.5px solid #FFFFFF; box-shadow:0 3px 10px rgba(0,0,0,0.55);"></div>' +
                  '<div style="position:absolute; left:9px; top:9px; width:14px; height:14px; border-radius:50%; background:#FFFFFF;"></div>' +
                  '</div>',
            iconSize: [32, 32],
            iconAnchor: [16, 31],
            popupAnchor: [0, -31]
          });

          var landWarningPopup = null;
          window.showLandRejectedWarning = function(lat, lon) {
            if (!map) return;
            if (landWarningPopup) { map.removeLayer(landWarningPopup); landWarningPopup = null; }
            var html = '<div style="padding:4px; min-width:190px; line-height:1.45; border-left:3px solid #EF4444; padding-left:8px;">' +
              '<b style="color:#F87171; font-size:12.5px;">⛔ KARA MEVKİİ SEÇİLEMEZ!</b><br>' +
              '<span style="color:#F1F5F9; font-size:11px; font-weight:600;">Gemiler karadan gidemez.</span><br>' +
              '<span style="color:#94A3B8; font-size:10px;">Lütfen harita üzerinde deniz / su alanında bir mevki seçiniz.</span>' +
              '</div>';
            landWarningPopup = L.popup({ autoPan: true, closeButton: true })
              .setLatLng([lat, lon])
              .setContent(html)
              .openOn(map);
            setTimeout(function() {
              if (landWarningPopup && map) { map.removeLayer(landWarningPopup); landWarningPopup = null; }
            }, 3800);
          };

          window.renderSeaRouteData = function(routeData, openPopupNow) {
            if (!map || !routeData) return;
            isUpdatingDestination = true;

            if (landWarningPopup) { map.removeLayer(landWarningPopup); landWarningPopup = null; }
            if (turningPointMarkers && turningPointMarkers.length > 0) {
              turningPointMarkers.forEach(function(m) { map.removeLayer(m); });
              turningPointMarkers = [];
            }
            clearRouteChevrons();

            var dLat = routeData.destLat;
            var dLon = routeData.destLon;
            var title = routeData.title || 'Varış Mevkii';

            if (!destMarker) {
              destMarker = L.marker([dLat, dLon], { icon: destIcon, zIndexOffset: 2000 }).addTo(map);
            } else {
              destMarker.setLatLng([dLat, dLon]);
            }

            var wps = routeData.turningPoints || [];
            if (wps.length > 0) {
              wps.forEach(function(wp) {
                var dnIcon = L.divIcon({
                  className: 'custom-dn-marker',
                  html: '<div style="position:relative; width:24px; height:24px; display:flex; align-items:center; justify-content:center;">' +
                        '<div style="position:absolute; width:18px; height:18px; background:#0F172A; border:2px solid #FFFFFF; transform:rotate(45deg); border-radius:3px; box-shadow:0 2px 6px rgba(9,34,82,0.75);"></div>' +
                        '<div style="position:absolute; color:#38BDF8; font-weight:900; font-size:8px; font-family:monospace; z-index:10;">' + wp.id + '</div>' +
                        '</div>',
                  iconSize: [24, 24],
                  iconAnchor: [12, 12]
                });
                var m = L.marker([wp.lat, wp.lon], { icon: dnIcon, zIndexOffset: 1200 }).addTo(map);
                var wpPopup = '<div style="padding: 2px; min-width: 170px; line-height: 1.45;">' +
                  '<b style="color: #38BDF8; font-size: 12px;">⚓ ' + wp.name + '</b><br>' +
                  '<span style="color: #34D399; font-size: 10px; font-weight:bold;">🌊 ' + (wp.desc || 'Deniz Seyir Koridoru') + '</span><br>' +
                  '<span style="font-family: monospace; font-size: 10px; color: #CBD5E1;">' + toDms(wp.lat, true) + ' ' + toDms(wp.lon, false) + '</span>' +
                  '<div style="margin-top: 4px; padding-top: 3px; border-top: 1px solid rgba(255,255,255,0.15);">' +
                  '<span style="color: #FACC15; font-weight: bold; font-size: 10.5px;">Bacak: ' + Number(wp.legDist).toFixed(1) + ' NM</span> • ' +
                  '<span style="color: #FACC15; font-weight: bold; font-size: 10.5px;">Geliş: ' + Math.round(wp.legBrg) + '° ➔ Yeni: ' + Math.round(wp.nextBrg) + '°</span><br>' +
                  '<span style="color: #34D399; font-weight: bold; font-size: 11px;">⏱️ ' + wp.id + ' ETA: ' + (wp.etaTimeStr || '--:--') + '</span> ' +
                  '<span style="color: #94A3B8; font-size: 10px;">(' + (wp.durationStr || '') + ')</span>' +
                  '</div>' +
                  '</div>';
                m.bindPopup(wpPopup);
                turningPointMarkers.push(m);
              });
            }

            var dmsStr = toDms(dLat, true) + ' ' + toDms(dLon, false);
            var waypointsLabel = wps.length > 0
              ? '<div style="margin-top: 3px; font-size: 10.5px; color: #38BDF8; font-weight: bold;">🧭 Deniz Seyir Koridoru (' + wps.length + ' DN): ' + wps.map(function(p){ return p.id; }).join(' ➔ ') + '</div>'
              : '<div style="margin-top: 3px; font-size: 10.5px; color: #34D399; font-weight: bold;">🧭 Emniyetli Deniz Seyir Kanalı</div>';

            var popupContent = '<div style="line-height: 1.45; padding: 3px; min-width: 190px;">' +
              '<b style="color: #38BDF8; font-size: 13px;">📍 ' + title + '</b><br>' +
              '<span style="font-family: monospace; font-size: 11px; color: #CBD5E1;">' + dmsStr + '</span>' +
              waypointsLabel +
              '<div style="margin-top: 5px; padding-top: 5px; border-top: 1px solid rgba(255,255,255,0.15);">' +
              '<span style="color: #FACC15; font-weight: bold;">Seyir Mesafesi: ' + Number(routeData.totalDistanceNm).toFixed(2) + ' NM</span> • ' +
              '<span style="color: #38BDF8; font-weight: bold;">İlk Rota: ' + Math.round(routeData.initialBearing || 0) + '°</span><br>' +
              '<div style="margin-top: 4px; background: rgba(30, 58, 138, 0.45); padding: 4px 6px; border-radius: 4px; border: 1px solid rgba(59, 130, 246, 0.4);">' +
              '⏱️ <b style="color: #93C5FD;">Varış Zamanı (ETA):</b> <b style="color: #34D399; font-size: 12px;">' + (routeData.etaTimeStr || '--:--') + '</b> (' + (routeData.fullEtaDateStr || '') + ')<br>' +
              '<span style="font-size: 10px; color: #94A3B8;">Seyir Süresi: ' + (routeData.durationStr || '--') + ' (@' + Number(routeData.speedKnots || 10).toFixed(1) + ' kts)</span>' +
              '</div>' +
              '<div style="font-size: 9.5px; color: #34D399; margin-top: 4px; opacity: 0.95;">🚢 <i>Deniz seyir koridorları (TSS) üzerinden hesaplanmıştır.</i></div>' +
              '<div style="margin-top: 6px; padding-top: 4px; border-top: 1px solid rgba(255,255,255,0.1); text-align: right;"><button onclick="clearDestinationAndRoute(true);" style="background:#EF4444; color:white; border:none; padding:3px 8px; border-radius:4px; font-size:10px; font-weight:bold; cursor:pointer;">🗑️ Rotayı Temizle</button></div>' +
              '</div>' +
              '</div>';

            destMarker.bindPopup(popupContent, { autoPan: Boolean(openPopupNow), closeOnClick: false });
            if (openPopupNow) { destMarker.openPopup(); }

            activeRouteCoords = routeData.pathCoords || [];
            lastChevronUpdateLat = null;
            lastChevronUpdateLon = null;

            syncVehicleWithRoute(false, !openPopupNow);

            setTimeout(function() { isUpdatingDestination = false; }, 300);
          };

          window.panToCoord = function(pLat, pLon) {
            if (map) {
              isProgrammaticPan = true;
              map.panTo([pLat, pLon], { animate: true, duration: 0.5 });
              setTimeout(function() { isProgrammaticPan = false; }, 550);
            }
          };

          window.recenterOnVehicle = function() {
            if (!map) return;
            isAutoFollow = true;
            isProgrammaticPan = true;
            var targetZoom = Math.max(map.getZoom(), 14);
            map.flyTo([currentDisplayLat, currentDisplayLon], targetZoom, { animate: true, duration: 0.55 });
            setTimeout(function() { isProgrammaticPan = false; }, 650);
          };

          window.updateShipPosition = function(sLat, sLon, sSpeed, sHeading, autoFollow, liveGps) {
            rawShipLat = sLat;
            rawShipLon = sLon;
            if (sSpeed && sSpeed > 0) shipSpeed = sSpeed;
            if (typeof sHeading === 'number' && !isNaN(sHeading)) {
              rawShipHeading = sHeading;
            }
            if (typeof autoFollow === 'boolean') {
              isAutoFollow = autoFollow;
            }
            if (typeof liveGps === 'boolean') {
              hasLiveGpsFix = liveGps;
            }
            if (shipMarker) {
              shipMarker.setPopupContent('🧭 <b>Mevcut Araç / Gemi Konumu</b><br>' + sLat.toFixed(5) + '°, ' + sLon.toFixed(5) + '°<br>Sürat: ' + Number(shipSpeed).toFixed(1) + ' kn');
            }
            syncVehicleWithRoute(true, true);
          };

          window.clearDestination = function() {
            clearDestinationAndRoute(false);
          };

          window.refreshOfflineTiles = function() {
            if (googleTileLayer) googleTileLayer.redraw();
            if (seamarkTileLayer && map && map.hasLayer(seamarkTileLayer)) {
              seamarkTileLayer.redraw();
            }
          };
        </script>
      </body>
      </html>
    """.trimIndent()
  }

  fun loadMapLocation(lat: Double, lon: Double, zoom: Int = 13) {
    if (isWebMapLoaded && webViewRef != null) {
      webViewRef?.evaluateJavascript(
        "if (window.panToCoord) { window.panToCoord($lat, $lon); }",
        null
      )
    } else {
      val html = buildGoogleMapHtml(lat, lon, zoom)
      webViewRef?.loadDataWithBaseURL("https://marine.local/", html, "text/html", "UTF-8", null)
    }
  }

  LaunchedEffect(uiState.mapFocusCoordinate) {
    uiState.mapFocusCoordinate?.let { target ->
      if (webViewRef != null && isWebMapLoaded) {
        webViewRef?.evaluateJavascript(
          "if (window.panToCoord) { window.panToCoord(${target.latitude}, ${target.longitude}); }",
          null
        )
      } else {
        loadMapLocation(target.latitude, target.longitude, 14)
      }
    }
  }

  LaunchedEffect(
    currentLat,
    currentLon,
    uiState.speedStr,
    uiState.headingDegreesStr,
    activeGps,
    uiState.isAutoFollowShip,
    isWebMapLoaded
  ) {
    if (!isWebMapLoaded) return@LaunchedEffect
    val speedVal = uiState.speedStr.toDoubleOrNull()?.takeIf { it > 0.0 }
      ?: activeGps?.speedKnots?.takeIf { it > 0.0 }
      ?: 10.0
    val effectiveHeading = activeGps?.bearingDegrees?.toDouble()
      ?: uiState.headingDegreesStr.toDoubleOrNull()
      ?: uiState.activeSeaRoute?.initialBearingDegrees?.toDouble()
      ?: 0.0
    val hasLiveGps = activeGps != null
    webViewRef?.evaluateJavascript(
      "if (window.updateShipPosition) { window.updateShipPosition($currentLat, $currentLon, $speedVal, $effectiveHeading, ${uiState.isAutoFollowShip}, $hasLiveGps); }",
      null
    )
  }

  LaunchedEffect(uiState.activeSeaRoute, isWebMapLoaded) {
    if (!isWebMapLoaded) return@LaunchedEffect
    val activeRoute = uiState.activeSeaRoute
    if (activeRoute != null && !activeRoute.isRejectedLandTarget && activeRoute.pathCoordinates.size >= 2) {
      val json = serializeSeaRouteJson(activeRoute)
      webViewRef?.evaluateJavascript(
        "if (window.renderSeaRouteData) { window.renderSeaRouteData($json, false); }",
        null
      )
    } else if (activeRoute == null) {
      webViewRef?.evaluateJavascript(
        "if (window.clearDestination) { window.clearDestination(); }",
        null
      )
    }
  }

  LaunchedEffect(forceOfflineMode, isDeviceOnline, offlineDownloadState.isDownloading, isWebMapLoaded) {
    if (!isWebMapLoaded) return@LaunchedEffect
    if (!offlineDownloadState.isDownloading) {
      webViewRef?.evaluateJavascript(
        "if (window.refreshOfflineTiles) { window.refreshOfflineTiles(); }",
        null
      )
    }
  }

  fun fetchGpsNow() {
    viewModel.setAutoFollowShip(true)
    viewModel.setGpsLoading(true)
    coroutineScope.launch {
      val result = gpsProvider.getCurrentGpsFix()
      result.fold(
        onSuccess = { fix ->
          viewModel.applyGpsFix(fix, isExplicitSync = true, formatAsDms = true)
          viewModel.startContinuousLocationUpdates(gpsProvider)
          webViewRef?.evaluateJavascript("if (window.recenterOnVehicle) { window.recenterOnVehicle(); }", null)
        },
        onFailure = { error ->
          viewModel.setGpsError(error.localizedMessage ?: "GPS konumu alınamadı.")
        }
      )
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      fetchGpsNow()
    } else {
      viewModel.setGpsError("Haritada canlı GPS takibi için konum izni gereklidir.")
    }
  }

  fun requestLocationPermissionAndFetch() {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (fine || coarse) {
      fetchGpsNow()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  LaunchedEffect(uiState.mobEvent, uiState.anchorEvent, uiState.anchorCalculationResult) {
    val mob = uiState.mobEvent
    val anchor = uiState.anchorEvent
    val anchorResult = uiState.anchorCalculationResult
    val r1 = anchorResult.d_firstSwingingCircleMeters
    val r2 = anchorResult.f_secondSwingingCircleMeters

    webViewRef?.evaluateJavascript(
      "if (window.updateAnchorOverlay) { window.updateAnchorOverlay(${anchor.isAnchored}, ${anchor.latitude}, ${anchor.longitude}, $r1, $r2); }",
      null
    )
    webViewRef?.evaluateJavascript(
      "if (window.updateMobOverlay) { window.updateMobOverlay(${mob.isActive}, ${mob.latitude}, ${mob.longitude}); }",
      null
    )
  }

  val blockedTrackerHosts = remember {
    listOf(
      "doubleclick.net", "google-analytics.com", "googlesyndication.com",
      "adnxs.com", "criteo.com", "amazon-adsystem.com", "adservice.google",
      "rubiconproject.com", "pubmatic.com", "casalemedia.com", "outbrain.com",
      "taboola.com", "didomi.io", "cookiebot.com", "cookielaw.org",
      "facebook.net", "hotjar.com", "scorecardresearch.com"
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(if (isDark) Color(0xFF0B132B) else Color(0xFFF1F5F9))
      .testTag("screen_marinetraffic_map")
  ) {
    // WEBVIEW GOOGLE MARINE MAP
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .testTag("webview_marinetraffic_map"),
      factory = { ctx ->
        WebView(ctx).apply {
          try {
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
          } catch (_: Throwable) {
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
          }
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )

          CookieManager.getInstance().setAcceptCookie(true)
          CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 MarineNav/1.0"
          }

          webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
              val req = request ?: return null
              val urlStr = req.url?.toString()?.lowercase() ?: return super.shouldInterceptRequest(view, req)
              for (host in blockedTrackerHosts) {
                if (urlStr.contains(host)) {
                  return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                }
              }
              offlineTileManager.interceptWebRequest(req)?.let { intercepted ->
                return intercepted
              }
              return super.shouldInterceptRequest(view, req)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              isLoading = true
              hasLoadError = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              isLoading = false
              hasLoadError = false
              isWebMapLoaded = true
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
              super.onReceivedError(view, request, error)
              val reqHost = request?.url?.host?.lowercase() ?: ""
              if (request?.isForMainFrame == true && reqHost != "marine.local" && !offlineTileManager.isEffectiveOffline()) {
                isLoading = false
                hasLoadError = true
              }
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean = true
          }

          webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              super.onProgressChanged(view, newProgress)
              loadingProgress = newProgress
              if (newProgress >= 70) isLoading = false
            }
          }

          addJavascriptInterface(
            MarineMapJsBridge(
              onLocationSelected = { lat, lon ->
                Handler(Looper.getMainLooper()).post {
                  val seaRoute = viewModel.calculateAndSetSeaRoute(
                    destLat = lat,
                    destLon = lon,
                    destName = "Haritada Seçilen Deniz Mevkii",
                    playVoice = true,
                    rejectLandDestination = true
                  )

                  if (seaRoute.isRejectedLandTarget) {
                    webViewRef?.evaluateJavascript("if (window.showLandRejectedWarning) { window.showLandRejectedWarning($lat, $lon); }", null)
                    Toast.makeText(context, "⛔ Seçilen mevki kara üzerindedir! Gemiler karadan gidemez.\nLütfen deniz üzerinde bir mevki seçiniz.", Toast.LENGTH_LONG).show()
                    return@post
                  }

                  val json = serializeSeaRouteJson(seaRoute)
                  webViewRef?.evaluateJavascript("if (window.renderSeaRouteData) { window.renderSeaRouteData($json, true); }", null)
                }
              },
              onDestinationPopupClosed = {
                Handler(Looper.getMainLooper()).post {
                  viewModel.setSimpleEtaDestination(null)
                }
              },
              onUserDraggedMap = {
                Handler(Looper.getMainLooper()).post {
                  viewModel.setAutoFollowShip(false)
                }
              }
            ),
            "AndroidBridge"
          )

          webViewRef = this
          loadMapLocation(currentLat, currentLon, 13)
        }
      }
    )

    // ANCHOR MONITORING CARD OVERLAY (TOP CENTER)
    if (uiState.anchorEvent.isAnchored) {
      val cLat = LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.anchorEvent.latitude
      val cLon = LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.anchorEvent.longitude
      val anchorDistMeters = uiState.anchorEvent.calculateDistanceMeters(cLat, cLon)
      val limitMeters = uiState.anchorEvent.safeSwingingRadiusGomina * 185.2
      val isDragging = anchorDistMeters > limitMeters

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDragging) (if (isDark) Color(0xFF450A0A).copy(alpha = 0.9f) else Color(0xFFFEF2F2).copy(alpha = 0.95f)) else (if (isDark) Color(0xFF064E3B).copy(alpha = 0.9f) else Color(0xFFECFDF5).copy(alpha = 0.95f)),
        border = BorderStroke(1.5.dp, if (isDragging) Color(0xFFDC2626) else Color(0xFF10B981)),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 86.dp, start = 16.dp, end = 16.dp)
          .fillMaxWidth(0.9f)
          .clickable { loadMapLocation(uiState.anchorEvent.latitude, uiState.anchorEvent.longitude, 16) }
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Anchor, contentDescription = null, tint = if (isDragging) Color(0xFFEF4444) else Color(0xFF10B981), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isDragging) "⚠️ DEMİR TARIYOR!" else "⚓ DEMİR NÖBETİ AKTİF",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                color = if (isDragging) (if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)) else (if (isDark) Color(0xFF34D399) else Color(0xFF047857))
              )
            }
            Text(
              text = "Saat: ${uiState.anchorEvent.dropTimeFormatted}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = if (isDragging) Color(0xFFF87171) else Color(0xFF34D399)
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Uzaklık: ${String.format(Locale.US, "%.1f", anchorDistMeters)}m",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
              color = if (isDragging) Color(0xFFF87171) else Color(0xFF34D399)
            )
            Text(
              text = "Limit: ${String.format(Locale.US, "%.1f", limitMeters)}m",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
              color = if (isDragging) Color(0xFFF87171) else Color(0xFF34D399)
            )
          }
        }
      }
    }

    // OFF-ROUTE COMPACT WARNING BADGE (TOP CENTER)
    val activeRouteForBanner = uiState.activeSeaRoute
    val currentGpsForBanner = activeGps
    if (activeRouteForBanner != null && !activeRouteForBanner.isRejectedLandTarget && currentGpsForBanner != null && activeRouteForBanner.pathCoordinates.size >= 2) {
      val offRouteDistNm = LocationPresets.calculateMinDistanceToPathNm(currentGpsForBanner.latitude, currentGpsForBanner.longitude, activeRouteForBanner.pathCoordinates)
      if (offRouteDistNm > 1.0) {
        Surface(
          onClick = { viewModel.toggleVoiceGuidance() },
          shape = RoundedCornerShape(20.dp),
          color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.92f) else Color(0xFFFEF2F2).copy(alpha = 0.95f),
          border = BorderStroke(1.dp, Color(0xFFEF4444)),
          shadowElevation = 6.dp,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = if (uiState.anchorEvent.isAnchored) 144.dp else 64.dp)
            .testTag("badge_off_route_alert")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AltRoute,
              contentDescription = null,
              tint = Color(0xFFFCA5A5),
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "⚠️ Rota Dışı: ${String.format(Locale.US, "%.1f", offRouteDistNm)} NM",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
              color = Color.White
            )
            Icon(
              imageVector = if (uiState.isVoiceGuidanceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
              contentDescription = null,
              tint = if (uiState.isVoiceGuidanceEnabled) Color(0xFFFCA5A5) else Color.Gray,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }
    }

    // TOP LEFT FLOATING MARINE LAYER CONTROLS BUTTONS
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(top = 16.dp, start = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Katmanlar & Yardımcılar Butonu
      Surface(
        onClick = { showLayersMenu = !showLayersMenu },
        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)),
        shadowElevation = 4.dp,
        modifier = Modifier.testTag("btn_map_layers_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Layers, contentDescription = "Katmanlar", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
          Text(
            text = "Deniz Katmanları",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = if (isDark) Color.White else Color(0xFF0F172A)
          )
        }
      }

      // Deniz İşaretleri Rehberi (Lejant) Butonu
      Surface(
        onClick = { showLegendDialog = true },
        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFFACC15)),
        shadowElevation = 4.dp,
        modifier = Modifier.testTag("btn_map_legend_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(Icons.Default.Explore, contentDescription = "Rehber", tint = Color(0xFFEAB308), modifier = Modifier.size(16.dp))
          Text(
            text = "Harita Lejantı",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = if (isDark) Color.White else Color(0xFF0F172A)
          )
        }
      }

      // Çevrimdışı Harita İndirme & İnternetsiz Seyir Butonu
      val dlPercent = if (offlineDownloadState.totalTiles > 0) {
        ((offlineDownloadState.completedTiles * 100) / offlineDownloadState.totalTiles).coerceIn(0, 100)
      } else 0
      Surface(
        onClick = {
          offlineTileManager.refreshStorageStatsAsync()
          showOfflineMapDialog = true
        },
        color = if (isEffectiveOffline) {
          if (isDark) Color(0xFF451A03).copy(alpha = 0.94f) else Color(0xFFFFFBEB).copy(alpha = 0.96f)
        } else {
          if (isDark) Color(0xFF0F172A).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
          1.dp,
          when {
            offlineDownloadState.isDownloading -> Color(0xFF196DFF)
            isEffectiveOffline -> Color(0xFFF59E0B)
            else -> Color(0xFF10B981)
          }
        ),
        shadowElevation = 4.dp,
        modifier = Modifier.testTag("btn_offline_map_open")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = if (isEffectiveOffline) Icons.Default.OfflinePin else Icons.Default.DownloadForOffline,
            contentDescription = "Çevrimdışı Harita İndir",
            tint = when {
              offlineDownloadState.isDownloading -> Color(0xFF196DFF)
              isEffectiveOffline -> Color(0xFFF59E0B)
              else -> Color(0xFF10B981)
            },
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = when {
              offlineDownloadState.isDownloading -> "İndiriliyor %$dlPercent"
              isEffectiveOffline -> "Çevrimdışı Harita"
              offlineDownloadState.cachedTileCount > 0 -> "Harita İndir (${offlineDownloadState.cachedTileCount})"
              else -> "Harita İndir"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = if (isEffectiveOffline) {
              if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
            } else {
              if (isDark) Color.White else Color(0xFF0F172A)
            }
          )
        }
      }
    }

    // TOP RIGHT YANDEX-STYLE SPEEDOMETER & ROUTE NAVIGATION BADGE
    val activeHudRoute = uiState.activeSeaRoute?.takeIf { !it.isRejectedLandTarget && it.pathCoordinates.size >= 2 }
    val displayKnots = activeGps?.speedKnots
      ?: uiState.speedStr.toDoubleOrNull()
      ?: 0.0
    val displayBearing = activeGps?.bearingDegrees?.toInt()
      ?: activeHudRoute?.initialBearingDegrees
      ?: (uiState.headingDegreesStr.toIntOrNull() ?: 0)

    Surface(
      onClick = {
        viewModel.setAutoFollowShip(true)
        webViewRef?.evaluateJavascript("if (window.recenterOnVehicle) { window.recenterOnVehicle(); }", null)
      },
      shape = RoundedCornerShape(22.dp),
      color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.96f),
      border = BorderStroke(1.5.dp, Color(0xFF196DFF)),
      shadowElevation = 6.dp,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 16.dp, end = 12.dp)
        .testTag("hud_yandex_nav_status")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(Color.White, CircleShape)
            .border(2.dp, Color(0xFFEF4444), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = String.format(Locale.US, "%.1f", displayKnots),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.5.sp),
              color = Color(0xFF0F172A)
            )
            Text(
              text = "kn",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 7.sp),
              color = Color(0xFF64748B)
            )
          }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(
            text = "🧭 ${String.format(Locale.US, "%03d", ((displayBearing % 360) + 360) % 360)}°",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
            color = if (isDark) Color.White else Color(0xFF0F172A)
          )
          if (activeHudRoute != null) {
            Text(
              text = "${String.format(Locale.US, "%.1f", activeHudRoute.totalDistanceNm)} NM • ${activeHudRoute.etaTimeStr}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
              color = Color(0xFF196DFF)
            )
          } else {
            Text(
              text = if (uiState.isAutoFollowShip) "Takip Aktif" else "Serbest Bakış",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
              color = Color(0xFF64748B)
            )
          }
        }
      }
    }

    // EXPANDABLE MARINE LAYERS CONTROL PANEL (TOP LEFT DROPDOWN)
    if (showLayersMenu) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.97f) else Color.White.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
        shadowElevation = 10.dp,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = 128.dp, start = 12.dp)
          .width(260.dp)
          .testTag("panel_marine_layers")
      ) {
        Column(
          modifier = Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("🗺️ Deniz Katman Seçimi", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
            IconButton(onClick = { showLayersMenu = false }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
          }

          HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

          Text("HARİTA TİPİ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.Gray)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
              selected = mapTypeMode == "hybrid",
              onClick = {
                mapTypeMode = "hybrid"
                webViewRef?.evaluateJavascript("setMapStyle('hybrid');", null)
              },
              label = { Text("Hibrit", fontSize = 10.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = mapTypeMode == "satellite",
              onClick = {
                mapTypeMode = "satellite"
                webViewRef?.evaluateJavascript("setMapStyle('satellite');", null)
              },
              label = { Text("Uydu", fontSize = 10.sp) },
              modifier = Modifier.weight(1f)
            )
            FilterChip(
              selected = mapTypeMode == "roadmap",
              onClick = {
                mapTypeMode = "roadmap"
                webViewRef?.evaluateJavascript("setMapStyle('roadmap');", null)
              },
              label = { Text("Harita", fontSize = 10.sp) },
              modifier = Modifier.weight(1f)
            )
          }

          HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

          Text("DENİZ YARDIMCILARI & İŞARETLER", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.Gray)

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🌊 OpenSeaMap Katmanı", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = if (isDark) Color.White else Color.Black)
            Switch(
              checked = showSeamarksLayer,
              onCheckedChange = {
                showSeamarksLayer = it
                webViewRef?.evaluateJavascript("toggleSeamarksLayer($it);", null)
              },
              modifier = Modifier.height(24.dp)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🚨 Fenerler & Çakar Işıkları", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = if (isDark) Color.White else Color.Black)
            Switch(
              checked = showLighthouses,
              onCheckedChange = {
                showLighthouses = it
                webViewRef?.evaluateJavascript("toggleCategoryLayer('lighthouses', $it);", null)
              },
              modifier = Modifier.height(24.dp)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("⚓ Kardinal & Lateral Şamandıra", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = if (isDark) Color.White else Color.Black)
            Switch(
              checked = showBuoys,
              onCheckedChange = {
                showBuoys = it
                webViewRef?.evaluateJavascript("toggleCategoryLayer('buoys', $it);", null)
              },
              modifier = Modifier.height(24.dp)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("🛳️ Demir Sahaları & Boğaz TSS", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = if (isDark) Color.White else Color.Black)
            Switch(
              checked = showAnchorageTss,
              onCheckedChange = {
                showAnchorageTss = it
                webViewRef?.evaluateJavascript("toggleCategoryLayer('anchorage', $it);", null)
              },
              modifier = Modifier.height(24.dp)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️ Sığlık Uyarısı & Derinlik", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = if (isDark) Color.White else Color.Black)
            Switch(
              checked = showDepthHazards,
              onCheckedChange = {
                showDepthHazards = it
                webViewRef?.evaluateJavascript("toggleCategoryLayer('hazards', $it);", null)
              },
              modifier = Modifier.height(24.dp)
            )
          }
        }
      }
    }

    // BOTTOM RIGHT FLOATING CONTROL ACTION BUTTONS (MOB / ANCHOR / MY LOCATION / FOCUS / RELOAD)
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 12.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.End
    ) {
      // Audio Voice Warning Mute/Unmute & Voice Settings Row
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          onClick = { viewModel.toggleVoiceGuidance() },
          color = if (uiState.isVoiceGuidanceEnabled) Color(0xFF0284C7) else Color(0xFF475569),
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 3.dp,
          modifier = Modifier.height(32.dp).testTag("fab_voice_mute_toggle")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = if (uiState.isVoiceGuidanceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
              contentDescription = "Tüm Sesli Uyarıları Aç / Sessize Al",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = if (uiState.isVoiceGuidanceEnabled) "Ses Açık" else "Sessiz",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp),
              color = Color.White
            )
          }
        }

        Surface(
          onClick = { viewModel.setShowVoiceSettingsDialog(true) },
          color = Color(0xFF0369A1),
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 3.dp,
          modifier = Modifier.height(32.dp).testTag("fab_voice_settings_open")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Ses Ayarları",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "Ayarlar",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp),
              color = Color.White
            )
          }
        }
      }

      // MOB Button
      Surface(
        onClick = {
          if (uiState.mobEvent.isActive) viewModel.cancelMob() else viewModel.triggerMob()
        },
        color = if (uiState.mobEvent.isActive) Color.Black else Color(0xFFDC2626),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 3.dp,
        modifier = Modifier.height(32.dp).testTag("fab_mob_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.Warning, contentDescription = "MOB", tint = Color.White, modifier = Modifier.size(14.dp))
          Text(
            text = if (uiState.mobEvent.isActive) "MOB İptal" else "MOB",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp),
            color = Color.White
          )
        }
      }

      // Anchor Button
      Surface(
        onClick = {
          if (uiState.anchorEvent.isAnchored) {
            viewModel.liftAnchor()
          } else {
            viewModel.applyAnchorCalculationToSwingingCircle()
            viewModel.dropAnchor()
          }
        },
        color = if (uiState.anchorEvent.isAnchored) Color(0xFF059669) else Color(0xFFEAB308),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 3.dp,
        modifier = Modifier.height(32.dp).testTag("fab_anchor_toggle")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(Icons.Default.Anchor, contentDescription = "Demir", tint = Color.White, modifier = Modifier.size(14.dp))
          Text(
            text = if (uiState.anchorEvent.isAnchored) "Demir Al" else "Demir At",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp),
            color = Color.White
          )
        }
      }

      // Yandex-Style Re-center & Auto-Follow Vehicle Button (shown when user pans map manually)
      AnimatedVisibility(
        visible = !uiState.isAutoFollowShip,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Surface(
          onClick = {
            viewModel.setAutoFollowShip(true)
            webViewRef?.evaluateJavascript("if (window.recenterOnVehicle) { window.recenterOnVehicle(); }", null)
          },
          color = Color(0xFF196DFF),
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.5.dp, Color.White),
          shadowElevation = 6.dp,
          modifier = Modifier.height(34.dp).testTag("btn_recenter_follow_vehicle")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Navigation,
              contentDescription = "Rotadaki Araca Ortala",
              tint = Color.White,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "Rotaya Ortala",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp),
              color = Color.White
            )
          }
        }
      }

      // Live GPS Center Button
      FloatingActionButton(
        onClick = {
          viewModel.setAutoFollowShip(true)
          webViewRef?.evaluateJavascript("if (window.recenterOnVehicle) { window.recenterOnVehicle(); }", null)
          requestLocationPermissionAndFetch()
        },
        containerColor = Color(0xFF059669),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.size(38.dp).testTag("fab_refresh_gps_map")
      ) {
        Icon(imageVector = Icons.Default.MyLocation, contentDescription = "GPS Konumu ve Odaklan", modifier = Modifier.size(18.dp))
      }

      // Focus Destination Pin Button
      if (uiState.mapFocusCoordinate != null || uiState.selectedSimpleEtaDestination != null) {
        val target = uiState.mapFocusCoordinate
          ?: uiState.selectedSimpleEtaDestination?.let { Coordinate(it.lat, it.lon) }
        if (target != null) {
          FloatingActionButton(
            onClick = {
              webViewRef?.evaluateJavascript("if (window.panToCoord) { window.panToCoord(${target.latitude}, ${target.longitude}); }", null)
            },
            containerColor = Color(0xFF1D4ED8),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(38.dp).testTag("fab_focus_target_eta")
          ) {
            Icon(imageVector = Icons.Default.PinDrop, contentDescription = "Hedef Noktaya Odaklan", modifier = Modifier.size(18.dp))
          }
        }
      }

      // Reload Map Button
      SmallFloatingActionButton(
        onClick = {
          isWebMapLoaded = false
          webViewRef?.reload()
        },
        containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
        contentColor = getMarineTextPrimary(isDark),
        shape = CircleShape,
        modifier = Modifier.size(34.dp).testTag("fab_reload_webview_map")
      ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Haritayı Yenile", modifier = Modifier.size(16.dp))
      }
    }

    // BOTTOM-LEFT ONE-TAP OFFLINE-CAPABLE ACTIVE NAVIGATION BUTTON
    val hasValidNavRoute = activeHudRoute != null
    val isLiveNavActive = uiState.isAutoFollowShip && activeGps != null
    Surface(
      onClick = {
        viewModel.setAutoFollowShip(true)
        webViewRef?.evaluateJavascript("if (window.recenterOnVehicle) { window.recenterOnVehicle(); }", null)
        requestLocationPermissionAndFetch()
        if (hasValidNavRoute) {
          viewModel.speakActiveRouteRecommendation()
          val modeNote = if (isEffectiveOffline) " (İnternetsiz Uydu GPS)" else ""
          Toast.makeText(
            context,
            "🧭 Navigasyon aktif$modeNote: ${activeHudRoute?.destinationName ?: ""}",
            Toast.LENGTH_SHORT
          ).show()
        } else {
          Toast.makeText(
            context,
            "🧭 Canlı GPS takibi başlatıldı! Rota oluşturmak için haritada deniz üzerinde bir noktaya dokunun.",
            Toast.LENGTH_LONG
          ).show()
        }
      },
      shape = RoundedCornerShape(22.dp),
      color = if (isLiveNavActive && hasValidNavRoute) Color(0xFF059669) else Color(0xFF196DFF),
      border = BorderStroke(1.5.dp, Color.White),
      shadowElevation = 8.dp,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 12.dp, bottom = 38.dp)
        .heightIn(min = 42.dp)
        .testTag("btn_start_active_navigation")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Navigation,
          contentDescription = "Navigasyonu Başlat",
          tint = Color.White,
          modifier = Modifier.size(17.dp)
        )
        Column {
          Text(
            text = when {
              isLiveNavActive && hasValidNavRoute -> "Navigasyon Aktif"
              hasValidNavRoute -> "Navigasyonu Başlat"
              else -> "GPS Seyri Başlat"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp),
            color = Color.White
          )
          if (isEffectiveOffline) {
            Text(
              text = "📴 İnternetsiz Uydu GPS",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
              color = Color(0xFFFDE68A)
            )
          }
        }
      }
    }

    // TOP LOADING INDICATOR
    AnimatedVisibility(
      visible = isLoading,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter)
    ) {
      LinearProgressIndicator(
        progress = { loadingProgress / 100f },
        modifier = Modifier.fillMaxWidth().height(3.dp),
        color = MarineYellow,
        trackColor = Color(0xFF0F172A).copy(alpha = 0.3f)
      )
    }

    // ERROR CONTAINER
    if (hasLoadError) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = getMarineCardBg(isDark).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, DangerRed),
        shadowElevation = 8.dp,
        modifier = Modifier.align(Alignment.Center).padding(24.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(imageVector = Icons.Default.WifiOff, contentDescription = null, tint = DangerRed, modifier = Modifier.size(40.dp))
          Spacer(modifier = Modifier.height(10.dp))
          Text("Deniz Haritası Yüklenemedi", style = MaterialTheme.typography.titleSmall, color = getMarineTextPrimary(isDark))
          Spacer(modifier = Modifier.height(4.dp))
          Text("İnternet bağlantınızı kontrol edin.", style = MaterialTheme.typography.bodySmall, color = getMarineTextSecondary(isDark))
          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                hasLoadError = false
                isWebMapLoaded = false
                webViewRef?.reload()
              },
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
              Text("Tekrar Dene")
            }
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$currentLat,$currentLon"))
                context.startActivity(intent)
              }
            ) {
              Text("Tarayıcıda Aç")
            }
          }
        }
      }
    }

    // MARINE NAVIGATIONAL AIDS LEGEND DIALOG (DENİZ SİMGELERİ REHBERİ)
    if (showLegendDialog) {
      Dialog(onDismissRequest = { showLegendDialog = false }) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isDark) Color(0xFF0F172A) else Color.White,
          border = BorderStroke(1.dp, Color(0xFF38BDF8)),
          shadowElevation = 16.dp,
          modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
        ) {
          Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(20.dp))
                Text("Deniz İşaretleri & Harita Lejantı", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (isDark) Color.White else Color(0xFF0F172A))
              }
              IconButton(onClick = { showLegendDialog = false }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
              }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            // Fenerler
            Text("🚨 DENİZ FENERLERİ & ÇAKAR IŞIKLARI (IALA)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFACC15))
            Text("Haritada sarı halkalı fener simgeleri gerçek deniz fenerlerini temsil eder. Örn: Fl.W.5s 16M (Her 5 saniyede bir çakan, 16 Deniz Mili menzilli beyaz fener).", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))

            // Kardinal Şamandıralar
            Text("▲ IALA KARDİNAL ŞAMANDIRALARI", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
            Text("• Kuzey Kardinal (▲▲): Tehlike güneyde, geçiş KUZEYDEN yapılır.\n• Güney Kardinal (▼▼): Tehlike kuzeyde, geçiş GÜNEYDEN yapılır.\n• Doğu Kardinal (▲▼): Tehlike batıda, geçiş DOĞUDAN yapılır.\n• Batı Kardinal (▼▲): Tehlike doğuda, geçiş BATIDAN yapılır.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))

            // Lateral Şamandıralar
            Text("🔴🟢 LATERAL ŞAMANDIRALAR (Sancak / İskele)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF34D399))
            Text("• Yeşil Koni (Sancak): Limana giriş yönünde sancak tarafınızda bırakılır.\n• Kırmızı Silindir (İskele): Limana giriş yönünde iskele tarafınızda bırakılır.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))

            // Demir Sahası & TSS
            Text("⚓ DEMİR SAHALARI & TRAFİK AYRIM DÜZENİ (TSS)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF60A5FA))
            Text("• Mavi Kesikli Çizgili Sahalar: Ticari ve amatör gemiler için demirleme bölgesidir.\n• Turuncu Çift Çizgiler: Boğaz ve yoğun deniz trafiği ayrım şeritleridir (TSS).", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))

            // Sığlık & Tehlike
            Text("⚠️ SIĞLIK & DÖKÜNTÜ KAYALIK UYARISI", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF87171))
            Text("Kırmızı dairesel uyarılı alanlar derinliği 5 metreden az olan sığlıkları, batıkları ve döküntü kayalıkları işaret eder.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp), color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155))

            Button(
              onClick = { showLegendDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
              Text("Anladım")
            }
          }
        }
      }
    }

    // OFFLINE MAP DOWNLOAD & OFFLINE NAVIGATION MANAGER DIALOG
    if (showOfflineMapDialog) {
      OfflineMapDialog(
        isDark = isDark,
        isDeviceOnline = isDeviceOnline,
        forceOfflineMode = forceOfflineMode,
        downloadState = offlineDownloadState,
        activeRoute = uiState.activeSeaRoute,
        currentLat = currentLat,
        currentLon = currentLon,
        onToggleForceOffline = { enabled ->
          offlineTileManager.setForceOfflineMode(enabled)
        },
        onDownloadActiveRoute = {
          val route = uiState.activeSeaRoute
          if (route != null && !route.isRejectedLandTarget && route.pathCoordinates.size >= 2) {
            offlineTileManager.downloadRouteCorridor(
              pathCoordinates = route.pathCoordinates,
              routeTitle = route.destinationName,
              mapTypeMode = mapTypeMode,
              includeSeamarks = showSeamarksLayer
            )
          }
        },
        onDownloadAroundCurrent = { radiusNm ->
          offlineTileManager.downloadAroundLocation(
            centerLat = currentLat,
            centerLon = currentLon,
            radiusNm = radiusNm,
            mapTypeMode = mapTypeMode,
            includeSeamarks = showSeamarksLayer
          )
        },
        onDownloadRegionPreset = { preset ->
          offlineTileManager.downloadRegionPreset(
            preset = preset,
            mapTypeMode = mapTypeMode,
            includeSeamarks = showSeamarksLayer
          )
        },
        onCancelDownload = {
          offlineTileManager.cancelDownload()
        },
        onClearCache = {
          offlineTileManager.clearAllOfflineTiles()
          if (isWebMapLoaded) {
            webViewRef?.evaluateJavascript("if (window.refreshOfflineTiles) { window.refreshOfflineTiles(); }", null)
          }
        },
        onStartNavigationNow = {
          showOfflineMapDialog = false
          viewModel.setAutoFollowShip(true)
          webViewRef?.evaluateJavascript("if (window.recenterOnVehicle) { window.recenterOnVehicle(); }", null)
          requestLocationPermissionAndFetch()
          if (uiState.activeSeaRoute != null && !uiState.activeSeaRoute.isRejectedLandTarget) {
            viewModel.speakActiveRouteRecommendation()
          }
          Toast.makeText(
            context,
            "🧭 İnternetsiz uydu GPS navigasyonu aktif!",
            Toast.LENGTH_SHORT
          ).show()
        },
        onDismiss = { showOfflineMapDialog = false }
      )
    }
  }
}
