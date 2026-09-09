package com.example.engine

import com.example.ui.TideUiState
import com.example.model.AnchorCalculationResult

object MapOverlayInjector {
  
  /**
   * Generates a JavaScript payload to inject into MarineTraffic WebView.
   * This script reads the map's center and zoom from the URL and draws
   * the Anchor Swinging Circle and MOB markers at the correct geographic pixels.
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

          var mobMarker = document.getElementById('mob-marker');
          if (config.mobActive) {
              if (!mobMarker) {
                  mobMarker = document.createElement('div');
                  mobMarker.id = 'mob-marker';
                  mobMarker.style.position = 'absolute';
                  mobMarker.innerHTML = '<div style="background: rgba(239, 68, 68, 0.95); border: 2px solid white; color: white; padding: 2px 6px; border-radius: 4px; font-weight: bold; font-family: sans-serif; font-size: 11px; transform: translate(-50%, -100%); margin-top: -8px; white-space: nowrap; box-shadow: 0 2px 4px rgba(0,0,0,0.3);">🚨 MOB</div><div style="width: 12px; height: 12px; background: red; border: 2px solid white; border-radius: 50%; transform: translate(-50%, -50%); box-shadow: 0 0 8px red;"></div>';
                  container.appendChild(mobMarker);
              }
              mobMarker.style.display = 'block';
          } else if (mobMarker) {
              mobMarker.style.display = 'none';
          }

          var anchorOuter = document.getElementById('anchor-outer');
          var anchorInner = document.getElementById('anchor-inner');
          var anchorCenter = document.getElementById('anchor-center');
          if (config.anchorActive) {
              if (!anchorOuter) {
                  anchorOuter = document.createElement('div');
                  anchorOuter.id = 'anchor-outer';
                  anchorOuter.style.position = 'absolute';
                  anchorOuter.style.border = '4px dashed rgba(239, 68, 68, 1)'; 
                  anchorOuter.style.backgroundColor = 'rgba(239, 68, 68, 0.25)';
                  anchorOuter.style.boxShadow = '0 0 10px rgba(239, 68, 68, 0.5)';
                  anchorOuter.style.borderRadius = '50%';
                  anchorOuter.style.transform = 'translate(-50%, -50%)';
                  container.appendChild(anchorOuter);

                  anchorInner = document.createElement('div');
                  anchorInner.id = 'anchor-inner';
                  anchorInner.style.position = 'absolute';
                  anchorInner.style.border = '3px dashed rgba(245, 158, 11, 1)'; 
                  anchorInner.style.backgroundColor = 'rgba(245, 158, 11, 0.20)';
                  anchorInner.style.boxShadow = '0 0 8px rgba(245, 158, 11, 0.5)';
                  anchorInner.style.borderRadius = '50%';
                  anchorInner.style.transform = 'translate(-50%, -50%)';
                  container.appendChild(anchorInner);
                  
                  anchorCenter = document.createElement('div');
                  anchorCenter.id = 'anchor-center';
                  anchorCenter.style.position = 'absolute';
                  anchorCenter.style.width = '8px';
                  anchorCenter.style.height = '8px';
                  anchorCenter.style.backgroundColor = '#0284C7';
                  anchorCenter.style.border = '2px solid white';
                  anchorCenter.style.borderRadius = '50%';
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
                  
                  if (config.anchorActive && anchorOuter && anchorInner && anchorCenter) {
                      var aX = lonToX(config.anchorLon, z) - cxPx + w2;
                      var aY = latToY(config.anchorLat, z) - cyPx + h2;
                      
                      var r1Px = config.r1Meters / metersPerPixel;
                      var r2Px = config.r2Meters / metersPerPixel;
                      
                      anchorOuter.style.left = aX + 'px';
                      anchorOuter.style.top = aY + 'px';
                      anchorOuter.style.width = (r2Px * 2) + 'px';
                      anchorOuter.style.height = (r2Px * 2) + 'px';
                      
                      anchorInner.style.left = aX + 'px';
                      anchorInner.style.top = aY + 'px';
                      anchorInner.style.width = (r1Px * 2) + 'px';
                      anchorInner.style.height = (r1Px * 2) + 'px';
                      
                      anchorCenter.style.left = aX + 'px';
                      anchorCenter.style.top = aY + 'px';
                  }
              } catch(e) {}
          }
          
          window._marineNavInterval = setInterval(updatePositions, 100);
          updatePositions();
      })();
    """.trimIndent()
  }
}