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

    // AIS Gemi Verisi (Canlı AIS veya Seçili Gemi)
    val aisVessel = uiState.activeAisVesselData ?: (if (uiState.mmsiStr.isNotBlank()) com.example.engine.AisTrackingEngine.getNb252ShipData() else null)
    val hasAis = aisVessel != null
    val aisLat = aisVessel?.latitude ?: 0.0
    val aisLon = aisVessel?.longitude ?: 0.0
    val aisCog = aisVessel?.cogDegrees ?: 0.0
    val aisHeading = aisVessel?.headingDegrees ?: 0
    val aisSog = aisVessel?.sogKnots ?: 0.0
    val aisName = (aisVessel?.name ?: "AIS").replace("'", "\\'").replace("\"", "\\\"")
    val aisMmsi = aisVessel?.mmsi ?: ""

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
              r2Meters: $r2,
              hasGps: $hasGps,
              gpsLat: $gpsLat,
              gpsLon: $gpsLon,
              gpsCog: $gpsCog,
              gpsSog: $gpsSog,
              gpsAcc: $gpsAcc,
              hasAis: $hasAis,
              aisLat: $aisLat,
              aisLon: $aisLon,
              aisCog: $aisCog,
              aisHeading: $aisHeading,
              aisSog: $aisSog,
              aisName: '$aisName',
              aisMmsi: '$aisMmsi',
              hasTargetEta: $hasTargetEta,
              targetEtaLat: $targetEtaLat,
              targetEtaLon: $targetEtaLon,
              targetEtaName: '$targetEtaName'
          };

          function lonToX(lon, zoom) { return (lon + 180) / 360 * 256 * Math.pow(2, zoom); }
          function latToY(lat, zoom) {
              var latRad = lat * Math.PI / 180;
              return (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * 256 * Math.pow(2, zoom);
          }

          var container = document.getElementById('marine-nav-overlay');
          if (!container) {
              container = document.createElement('div');
              container.id = 'marine-nav-overlay';
              container.style.position = 'fixed';
              container.style.top = '0';
              container.style.left = '0';
              container.style.width = '100vw';
              container.style.height = '100vh';
              container.style.pointerEvents = 'none';
              container.style.zIndex = '99999';
              document.body.appendChild(container);
          }

          if (!document.getElementById('marine-marker-pulse-style')) {
              var pStyle = document.createElement('style');
              pStyle.id = 'marine-marker-pulse-style';
              pStyle.innerHTML = '@keyframes marinePulse { 0% { transform: translate(-50%, -50%) scale(0.7); opacity: 0.95; } 100% { transform: translate(-50%, -50%) scale(2.2); opacity: 0; } }';
              document.head.appendChild(pStyle);
          }

          // 1. Canlı MOB (Denize Adam Düştü) - KIRMIZI DARBELİ İŞARET
          var mobMarker = document.getElementById('mob-marker');
          if (config.mobActive) {
              if (!mobMarker) {
                  mobMarker = document.createElement('div');
                  mobMarker.id = 'mob-marker';
                  mobMarker.style.position = 'absolute';
                  mobMarker.style.transform = 'translate(-50%, -50%)';
                  mobMarker.style.pointerEvents = 'none';
                  mobMarker.innerHTML = '<div style="position: absolute; width: 38px; height: 38px; background: rgba(239, 68, 68, 0.4); border: 2.5px solid #EF4444; border-radius: 50%; transform: translate(-50%, -50%); animation: marinePulse 1.3s infinite ease-out;"></div><div style="position: absolute; width: 16px; height: 16px; background: #DC2626; border: 3px solid #FFFFFF; border-radius: 50%; box-shadow: 0 0 10px #991B1B, 0 0 20px #EF4444; transform: translate(-50%, -50%);"></div><div id="mob-label" style="position: absolute; transform: translate(-50%, -100%); margin-top: -14px; background: rgba(185, 28, 28, 0.95); border: 1.5px solid #FCA5A5; color: #FFFFFF; padding: 2px 7px; border-radius: 4px; font-weight: 900; font-family: -apple-system, sans-serif; font-size: 10px; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.5); letter-spacing: 0.3px;">🚨 MOB</div>';
                  container.appendChild(mobMarker);
              }
              mobMarker.style.display = 'block';
          } else if (mobMarker) {
              mobMarker.style.display = 'none';
          }

          // 2. Canlı GPS Gemi İşareti - YEŞİL DARBELİ İŞARET
          var gpsMarker = document.getElementById('gps-vessel-marker');
          var gpsAccuracyCircle = document.getElementById('gps-accuracy-circle');
          if (config.hasGps || config.gpsLat) {
              if (!gpsAccuracyCircle) {
                  gpsAccuracyCircle = document.createElement('div');
                  gpsAccuracyCircle.id = 'gps-accuracy-circle';
                  gpsAccuracyCircle.style.position = 'absolute';
                  gpsAccuracyCircle.style.border = '1.5px solid rgba(34, 197, 94, 0.6)';
                  gpsAccuracyCircle.style.backgroundColor = 'rgba(34, 197, 94, 0.12)';
                  gpsAccuracyCircle.style.borderRadius = '50%';
                  gpsAccuracyCircle.style.transform = 'translate(-50%, -50%)';
                  gpsAccuracyCircle.style.pointerEvents = 'none';
                  container.appendChild(gpsAccuracyCircle);
              }
              if (!gpsMarker) {
                  gpsMarker = document.createElement('div');
                  gpsMarker.id = 'gps-vessel-marker';
                  gpsMarker.style.position = 'absolute';
                  gpsMarker.style.transform = 'translate(-50%, -50%)';
                  gpsMarker.style.pointerEvents = 'none';
                  gpsMarker.innerHTML = '<div style="position: absolute; width: 34px; height: 34px; background: rgba(34, 197, 94, 0.35); border: 2px solid #22C55E; border-radius: 50%; transform: translate(-50%, -50%); animation: marinePulse 1.8s infinite ease-out;"></div><div id="gps-heading-wrap" style="position: absolute; width: 0; height: 0; transform: rotate(0deg);"><div style="position: absolute; left: -7px; top: -21px; width: 0; height: 0; border-left: 7px solid transparent; border-right: 7px solid transparent; border-bottom: 16px solid #16A34A; filter: drop-shadow(0 0 2.5px rgba(255,255,255,0.9));"></div></div><div style="position: absolute; width: 16px; height: 16px; background: #16A34A; border: 3px solid #FFFFFF; border-radius: 50%; box-shadow: 0 0 10px #15803D, 0 0 18px #22C55E; transform: translate(-50%, -50%);"></div><div id="gps-label" style="position: absolute; transform: translate(-50%, 0); margin-top: 13px; background: rgba(15, 23, 42, 0.92); border: 1.5px solid #4ADE80; color: #FFFFFF; padding: 2px 7px; border-radius: 4px; font-weight: 900; font-family: -apple-system, sans-serif; font-size: 10px; text-align: center; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.5); letter-spacing: 0.3px;">🟢 GPS</div>';
                  container.appendChild(gpsMarker);
              }
              gpsMarker.style.display = 'block';
              gpsAccuracyCircle.style.display = 'block';
          } else {
              if (gpsMarker) gpsMarker.style.display = 'none';
              if (gpsAccuracyCircle) gpsAccuracyCircle.style.display = 'none';
          }

          // 3. Canlı AIS Gemi İşareti - SARI DARBELİ İŞARET
          var aisMarker = document.getElementById('ais-vessel-marker');
          if (config.hasAis && config.aisLat !== 0) {
              if (!aisMarker) {
                  aisMarker = document.createElement('div');
                  aisMarker.id = 'ais-vessel-marker';
                  aisMarker.style.position = 'absolute';
                  aisMarker.style.transform = 'translate(-50%, -50%)';
                  aisMarker.style.pointerEvents = 'none';
                  aisMarker.innerHTML = '<div style="position: absolute; width: 34px; height: 34px; background: rgba(234, 179, 8, 0.35); border: 2px solid #EAB308; border-radius: 50%; transform: translate(-50%, -50%); animation: marinePulse 2.0s infinite ease-out;"></div><div id="ais-heading-wrap" style="position: absolute; width: 0; height: 0; transform: rotate(0deg);"><div style="position: absolute; left: -7px; top: -21px; width: 0; height: 0; border-left: 7px solid transparent; border-right: 7px solid transparent; border-bottom: 16px solid #CA8A04; filter: drop-shadow(0 0 2.5px rgba(255,255,255,0.9));"></div></div><div style="position: absolute; width: 16px; height: 16px; background: #CA8A04; border: 3px solid #FFFFFF; border-radius: 50%; box-shadow: 0 0 10px #854D0E, 0 0 18px #EAB308; transform: translate(-50%, -50%);"></div><div id="ais-label" style="position: absolute; transform: translate(-50%, 0); margin-top: 13px; background: rgba(15, 23, 42, 0.92); border: 1.5px solid #FDE047; color: #FFFFFF; padding: 2px 7px; border-radius: 4px; font-weight: 900; font-family: -apple-system, sans-serif; font-size: 10px; text-align: center; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.5); letter-spacing: 0.3px;">🟡 AIS</div>';
                  container.appendChild(aisMarker);
              }
              aisMarker.style.display = 'block';
          } else if (aisMarker) {
              aisMarker.style.display = 'none';
          }

          var anchorOuter = document.getElementById('anchor-outer');
          var anchorInner = document.getElementById('anchor-inner');
          var anchorCenter = document.getElementById('anchor-center');
          if (config.anchorActive) {
              if (!anchorOuter) {
                  anchorOuter = document.createElement('div');
                  anchorOuter.id = 'anchor-outer';
                  anchorOuter.style.position = 'absolute';
                  anchorOuter.style.border = '2px dashed rgba(234, 179, 8, 0.85)';
                  anchorOuter.style.backgroundColor = 'rgba(234, 179, 8, 0.12)';
                  anchorOuter.style.borderRadius = '50%';
                  anchorOuter.style.transform = 'translate(-50%, -50%)';
                  anchorOuter.style.pointerEvents = 'none';
                  container.appendChild(anchorOuter);

                  anchorInner = document.createElement('div');
                  anchorInner.id = 'anchor-inner';
                  anchorInner.style.position = 'absolute';
                  anchorInner.style.border = '2px solid rgba(34, 197, 94, 0.9)';
                  anchorInner.style.backgroundColor = 'rgba(34, 197, 94, 0.18)';
                  anchorInner.style.borderRadius = '50%';
                  anchorInner.style.transform = 'translate(-50%, -50%)';
                  anchorInner.style.pointerEvents = 'none';
                  container.appendChild(anchorInner);

                  anchorCenter = document.createElement('div');
                  anchorCenter.id = 'anchor-center';
                  anchorCenter.style.position = 'absolute';
                  anchorCenter.innerHTML = '<div style="background: rgba(15, 23, 42, 0.9); border: 1.5px solid #EAB308; color: #FACC15; padding: 2px 6px; border-radius: 4px; font-weight: bold; font-family: sans-serif; font-size: 10px; transform: translate(-50%, -100%); margin-top: -6px; white-space: nowrap;">⚓ DEMİR</div><div style="width: 10px; height: 10px; background: #EAB308; border: 2px solid white; border-radius: 50%; transform: translate(-50%, -50%);"></div>';
                  anchorCenter.style.transform = 'translate(-50%, -50%)';
                  container.appendChild(anchorCenter);
              }
              anchorOuter.style.display = 'block';
              anchorInner.style.display = 'block';
              anchorCenter.style.display = 'block';
          } else if (anchorOuter) {
              anchorOuter.style.display = 'none';
              anchorInner.style.display = 'none';
              anchorCenter.style.display = 'none';
          }

          // 4. Elle Girilen / Seçilen Varış Mevkii Mavi Nokta İşareti (Target ETA Blue Dot)
          var targetEtaMarker = document.getElementById('target-eta-marker');
          if (config.hasTargetEta && config.targetEtaLat !== 0) {
              if (!targetEtaMarker) {
                  targetEtaMarker = document.createElement('div');
                  targetEtaMarker.id = 'target-eta-marker';
                  targetEtaMarker.style.position = 'absolute';
                  targetEtaMarker.style.transform = 'translate(-50%, -50%)';
                  targetEtaMarker.style.pointerEvents = 'none';
                  targetEtaMarker.innerHTML = '<div style="position: absolute; width: 34px; height: 34px; background: rgba(37, 99, 235, 0.35); border: 2px solid #3B82F6; border-radius: 50%; transform: translate(-50%, -50%); animation: marinePulse 1.8s infinite ease-out;"></div><div style="position: absolute; width: 16px; height: 16px; background: #2563EB; border: 3px solid #FFFFFF; border-radius: 50%; box-shadow: 0 0 10px #1D4ED8, 0 0 18px #3B82F6; transform: translate(-50%, -50%);"></div><div id="target-eta-label" style="position: absolute; transform: translate(-50%, -100%); margin-top: -14px; background: rgba(15, 23, 42, 0.92); border: 1.5px solid #38BDF8; color: #FFFFFF; padding: 2px 7px; border-radius: 4px; font-weight: 900; font-family: -apple-system, sans-serif; font-size: 10px; white-space: nowrap; box-shadow: 0 2px 6px rgba(0,0,0,0.5); letter-spacing: 0.3px;">🔵 📍 ' + (config.targetEtaName || 'Varış Mevkii') + '</div>';
                  container.appendChild(targetEtaMarker);
              }
              targetEtaMarker.style.display = 'block';
              var targetLabel = document.getElementById('target-eta-label');
              if (targetLabel) {
                  targetLabel.textContent = '🔵 📍 ' + (config.targetEtaName || 'Varış Mevkii');
              }
          } else if (targetEtaMarker) {
              targetEtaMarker.style.display = 'none';
          }

          if (window._marineNavInterval) clearInterval(window._marineNavInterval);
          
          function updatePositions() {
              try {
                  var url = window.location.href;
                  var cxMatch = url.match(/centerx:([\d\.\-]+)/);
                  var cyMatch = url.match(/centery:([\d\.\-]+)/);
                  var zMatch = url.match(/zoom:([\d\.\-]+)/);
                  
                  if (!cxMatch || !cyMatch || !zMatch) return;
                  
                  var cx = parseFloat(cxMatch[1]);
                  var cy = parseFloat(cyMatch[1]);
                  var z = parseFloat(zMatch[1]);
                  
                  var cxPx = lonToX(cx, z);
                  var cyPx = latToY(cy, z);
                  
                  var w2 = window.innerWidth / 2;
                  var h2 = window.innerHeight / 2;
                  
                  var metersPerPixel = 156543.03392 * Math.cos(cy * Math.PI / 180) / Math.pow(2, z);
                  
                  if (config.mobActive && mobMarker) {
                      var mX = lonToX(config.mobLon, z) - cxPx + w2;
                      var mY = latToY(config.mobLat, z) - cyPx + h2;
                      mobMarker.style.left = mX + 'px';
                      mobMarker.style.top = mY + 'px';
                  }

                  // GPS Gemi Konumu Güncelle
                  if ((config.hasGps || config.gpsLat) && gpsMarker) {
                      var gX = lonToX(config.gpsLon, z) - cxPx + w2;
                      var gY = latToY(config.gpsLat, z) - cyPx + h2;
                      gpsMarker.style.left = gX + 'px';
                      gpsMarker.style.top = gY + 'px';

                      var headingArrow = document.getElementById('gps-heading-wrap') || document.getElementById('gps-heading-arrow');
                      if (headingArrow) {
                          headingArrow.style.transform = 'rotate(' + (config.gpsCog || 0) + 'deg)';
                      }
                      var gpsLabel = document.getElementById('gps-label');
                      if (gpsLabel) {
                          var sogText = config.gpsSog > 0 ? (' ' + config.gpsSog.toFixed(1) + ' kn') : '';
                          var cogText = ' ' + Math.round(config.gpsCog || 0) + '°';
                          gpsLabel.textContent = '🟢 GPS' + sogText + cogText;
                      }

                      if (gpsAccuracyCircle) {
                          var accMeters = Math.max(config.gpsAcc || 10, 8);
                          var accPx = Math.max(accMeters / metersPerPixel, 8);
                          gpsAccuracyCircle.style.left = gX + 'px';
                          gpsAccuracyCircle.style.top = gY + 'px';
                          gpsAccuracyCircle.style.width = (accPx * 2) + 'px';
                          gpsAccuracyCircle.style.height = (accPx * 2) + 'px';
                      }
                  }

                  // AIS Gemi Konumu Güncelle (Aynı GPS gibi canlı harita üzerinde)
                  if (config.hasAis && config.aisLat !== 0 && aisMarker) {
                      var aX = lonToX(config.aisLon, z) - cxPx + w2;
                      var aY = latToY(config.aisLat, z) - cyPx + h2;
                      aisMarker.style.left = aX + 'px';
                      aisMarker.style.top = aY + 'px';

                      var aisArrow = document.getElementById('ais-heading-wrap') || document.getElementById('ais-heading-arrow');
                      if (aisArrow) {
                          var heading = config.aisHeading > 0 ? config.aisHeading : (config.aisCog || 0);
                          aisArrow.style.transform = 'rotate(' + heading + 'deg)';
                      }
                      var aisLabel = document.getElementById('ais-label');
                      if (aisLabel) {
                          var sogT = config.aisSog > 0 ? (' ' + config.aisSog.toFixed(1) + ' kn') : '';
                          var cogT = ' ' + Math.round(config.aisCog || 0) + '°';
                          var nameT = config.aisName ? config.aisName.substring(0, 14) : (config.aisMmsi ? ('MMSI: ' + config.aisMmsi) : 'AIS');
                          aisLabel.textContent = '🟡 ' + nameT + sogT + cogT;
                      }
                  }
                  
                  if (config.anchorActive && anchorOuter && anchorInner && anchorCenter) {
                      var anX = lonToX(config.anchorLon, z) - cxPx + w2;
                      var anY = latToY(config.anchorLat, z) - cyPx + h2;
                      
                      var r1Px = config.r1Meters / metersPerPixel;
                      var r2Px = config.r2Meters / metersPerPixel;
                      
                      anchorOuter.style.left = anX + 'px';
                      anchorOuter.style.top = anY + 'px';
                      anchorOuter.style.width = (r2Px * 2) + 'px';
                      anchorOuter.style.height = (r2Px * 2) + 'px';
                      
                      anchorInner.style.left = anX + 'px';
                      anchorInner.style.top = anY + 'px';
                      anchorInner.style.width = (r1Px * 2) + 'px';
                      anchorInner.style.height = (r1Px * 2) + 'px';
                      
                      anchorCenter.style.left = anX + 'px';
                      anchorCenter.style.top = anY + 'px';
                  }

                  // 4. Hedef / ETA Varış Mevkii Mavi Nokta Konumu Güncelle
                  if (config.hasTargetEta && config.targetEtaLat !== 0 && targetEtaMarker) {
                      var tX = lonToX(config.targetEtaLon, z) - cxPx + w2;
                      var tY = latToY(config.targetEtaLat, z) - cyPx + h2;
                      targetEtaMarker.style.left = tX + 'px';
                      targetEtaMarker.style.top = tY + 'px';
                  }
              } catch(e) {}
          }
          
          window._marineNavInterval = setInterval(updatePositions, 100);
          updatePositions();
      })();
    """.trimIndent()
  }
}
