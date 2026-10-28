package com.example.engine

import com.example.ui.TideUiState
import com.example.model.AnchorCalculationResult

object MapOverlayInjector {
  
  /**
   * Generates a JavaScript payload to inject into MarineTraffic WebView.
   * This script reads the map's center and zoom from the URL and draws
   * the GPS marker, AIS Vessel marker, Anchor Swinging Circle, and MOB marker.
   */
  fun getInjectableJavascript(uiState: TideUiState): String {
    val mobActive = uiState.mobEvent.isActive
    val mobLat = uiState.mobEvent.latitude
    val mobLon = uiState.mobEvent.longitude
    
    val anchorActive = uiState.anchorEvent.isAnchored
    val anchorLat = uiState.anchorEvent.latitude
    val anchorLon = uiState.anchorEvent.longitude
    
    val anchorResult = uiState.anchorCalculationResult
    val r1 = anchorResult.d_firstSwingingCircleMeters
    val r2 = anchorResult.f_secondSwingingCircleMeters

    val lastFix = uiState.lastGpsFix
    val hasGps = lastFix != null
    val gpsLat = lastFix?.latitude ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.latStr) ?: uiState.selectedPort.latitude)
    val gpsLon = lastFix?.longitude ?: (com.example.model.LocationPresets.parseCoordinateOrDecimal(uiState.lonStr) ?: uiState.selectedPort.longitude)
    val gpsCog = lastFix?.bearingDegrees?.toDouble() ?: (uiState.headingDegreesStr.toDoubleOrNull() ?: 0.0)
    val gpsSog = lastFix?.speedKnots ?: (uiState.speedStr.toDoubleOrNull() ?: 0.0)
    val gpsAcc = lastFix?.accuracyMeters ?: 0f



    // 4. Hedef / ETA Varış Mevkii (Elle girilen koordinat veya seçili liman)
    val targetEtaCoord = uiState.mapFocusCoordinate
      ?: uiState.selectedSimpleEtaDestination?.let { com.example.model.Coordinate(it.lat, it.lon) }
    val hasTargetEta = targetEtaCoord != null && (targetEtaCoord.latitude != 0.0 || targetEtaCoord.longitude != 0.0)
    val targetEtaLat = targetEtaCoord?.latitude ?: 0.0
    val targetEtaLon = targetEtaCoord?.longitude ?: 0.0
    val targetEtaName = (uiState.selectedSimpleEtaDestination?.name ?: "Varış Mevkii")
      .replace("'", "\\'").replace("\"", "\\\"")

    return """
      (function() {
          var config = {
              mobActive: $mobActive,
              mobLat: $mobLat,
              mobLon: $mobLon,
              anchorActive: $anchorActive,
              anchorLat: $anchorLat,
              anchorLon: $anchorLon,
              r1Meters: $r1,
              r2Meters: $r2
          };

          if (window.map && window.L) {
              if (!document.getElementById('topdown-flat-style')) {
                  var style = document.createElement('style');
                  style.id = 'topdown-flat-style';
                  style.innerHTML = 'html, body, #map, .leaflet-container { transform: none !important; perspective: none !important; }';
                  document.head.appendChild(style);
              }

              // 1. Anchor / Salma Dairesi Overlays
              if (window._anchorOuterCircle) { window.map.removeLayer(window._anchorOuterCircle); window._anchorOuterCircle = null; }
              if (window._anchorInnerCircle) { window.map.removeLayer(window._anchorInnerCircle); window._anchorInnerCircle = null; }
              if (window._anchorCenterMarker) { window.map.removeLayer(window._anchorCenterMarker); window._anchorCenterMarker = null; }

              if (config.anchorActive && config.anchorLat && config.anchorLon) {
                  // İç Salma Dairesi (Yeşil emniyet dairesi - Tam tepeden mükemmel daire)
                  window._anchorInnerCircle = window.L.circle([config.anchorLat, config.anchorLon], {
                      radius: config.r1Meters || 50,
                      color: '#10B981',
                      fillColor: '#10B981',
                      fillOpacity: 0.15,
                      weight: 1.5
                  }).addTo(window.map);

                  // Dış Salma Dairesi (Kırmızı/Sarı sınır dairesi - Tam tepeden mükemmel daire)
                  window._anchorOuterCircle = window.L.circle([config.anchorLat, config.anchorLon], {
                      radius: config.r2Meters || 100,
                      color: '#EF4444',
                      fillColor: '#EF4444',
                      fillOpacity: 0.08,
                      weight: 1.5,
                      dashArray: '5, 5'
                  }).addTo(window.map);

                  // Demir Merkezi İşareti (Anchor Marker)
                  var anchorIcon = window.L.divIcon({
                      className: 'custom-anchor-center-icon',
                      html: '<div style="position:relative; display:flex; align-items:center; justify-content:center; width:20px; height:20px; margin-left:-10px; margin-top:-10px;">' +
                            '<div style="background: rgba(15, 23, 42, 0.9); border: 1.5px solid #10B981; color: #34D399; padding: 2px 6px; border-radius: 4px; font-weight: bold; font-family: sans-serif; font-size: 10px; white-space: nowrap; transform: translate(-50%, -100%); margin-top: -12px; position: absolute; left: 50%;">⚓ DEMİR</div>' +
                            '<div style="width: 10px; height: 10px; background: #10B981; border: 2px solid white; border-radius: 50%; box-shadow: 0 1px 4px rgba(0,0,0,0.4);"></div>' +
                            '</div>',
                      iconSize: [20, 20],
                      iconAnchor: [10, 10]
                  });
                  window._anchorCenterMarker = window.L.marker([config.anchorLat, config.anchorLon], { icon: anchorIcon }).addTo(window.map);
              }

              // 2. MOB (Denize Adam Düştü) Overlays
              if (window._mobMarker) { window.map.removeLayer(window._mobMarker); window._mobMarker = null; }

              if (config.mobActive && config.mobLat && config.mobLon) {
                  var mobIcon = window.L.divIcon({
                      className: 'custom-mob-icon',
                      html: '<div style="position:relative; width:44px; height:44px; margin-left:-22px; margin-top:-22px; display:flex; align-items:center; justify-content:center;">' +
                            '<div style="position:absolute; width:40px; height:32px; background:#DC2626; border:2px solid white; border-radius:4px; display:flex; align-items:center; justify-content:center; box-shadow: 0 2px 8px rgba(0,0,0,0.5); z-index:1000;">' +
                            '<span style="color:white; font-weight:black; font-family:sans-serif; font-size:10px; letter-spacing:0.5px;">🚨 MOB</span>' +
                            '</div>' +
                            '<div style="position:absolute; width:44px; height:44px; border-radius:50%; background:rgba(220,38,38,0.25); border:1.5px dashed #DC2626; animation:pulseRing 2s infinite ease-out;"></div>' +
                            '</div>',
                      iconSize: [44, 44],
                      iconAnchor: [22, 22]
                  });
                  window._mobMarker = window.L.marker([config.mobLat, config.mobLon], { icon: mobIcon }).addTo(window.map);
                  window._mobMarker.bindPopup('🚨 <b>DENİZE ADAM DÜŞTÜ (MOB)</b><br>Mevki: ' + config.mobLat.toFixed(5) + '°, ' + config.mobLon.toFixed(5) + '°<br><span style="color:#EF4444; font-weight:bold;">ACİL DURUM!</span>');
                  window.map.setView([config.mobLat, config.mobLon], 16);
              }
          }
      })();
    """.trimIndent()
  }
}
