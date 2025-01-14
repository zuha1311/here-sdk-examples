/*
 * Copyright (C) 2020-2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.examples.positioning;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.here.sdk.consent.Consent;
import com.here.sdk.consent.ConsentEngine;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolygon;
import com.here.sdk.core.Location;
import com.here.sdk.core.LocationListener;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.location.LocationAccuracy;
import com.here.sdk.location.LocationEngine;
import com.here.sdk.location.LocationEngineStatus;
import com.here.sdk.location.LocationFeature;
import com.here.sdk.location.LocationStatusListener;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolygon;
import com.here.sdk.mapview.MapView;

import java.util.List;

public class PositioningExample {

    private static final String TAG = PositioningExample.class.getSimpleName();

    private static final double CENTER_RADIUS_IN_METERS = 1;
    private static final int CAMERA_DISTANCE_IN_METERS = 200;

    private final GeoCoordinates defaultLocation = new GeoCoordinates(52.520798,13.409408);
    private final Color colorCenter = Color.valueOf(0, 0.56f, 0.54f, 1); // RGBA
    private final Color colorAccuracy = Color.valueOf(0.46f, 0.89f, 0.86f, 0.20f); // RGBA

    private MapView mapView;
    private Context context;
    private MapPolygon locationAccuracyCircle;
    private MapMarker locationCenterCircle;
    private LocationEngine locationEngine;
    private ConsentEngine consentEngine;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationUpdated(@NonNull Location location) {
            final double accuracy = (location.horizontalAccuracyInMeters != null) ? location.horizontalAccuracyInMeters: 0.0;
            updateMyLocation(location.coordinates, accuracy);

            //Update the map viewport to be centered on the location.
            mapView.getCamera().lookAt(location.coordinates, CAMERA_DISTANCE_IN_METERS);
        }
    };

    private final LocationStatusListener locationStatusListener = new LocationStatusListener() {
        @Override
        public void onStatusChanged(@NonNull LocationEngineStatus locationEngineStatus) {
            if(locationEngineStatus == LocationEngineStatus.ENGINE_STOPPED) {
                locationEngine.removeLocationListener(locationListener);
                locationEngine.removeLocationStatusListener(locationStatusListener);
            }
        }

        @Override
        public void onFeaturesNotAvailable(@NonNull List<LocationFeature> features) {
            for (LocationFeature feature : features) {
                Log.d(TAG, "Feature not available: " + feature.name());
            }
        }
    };

    public void onMapSceneLoaded(MapView mapView, Context context) {
        this.mapView = mapView;
        this.context = context;

        try {
            consentEngine = new ConsentEngine();
            locationEngine = new LocationEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization failed: " + e.getMessage());
        }

        if (consentEngine.getUserConsentState() == Consent.UserReply.NOT_HANDLED) {
            consentEngine.requestUserConsent();
        }

        Location myLastLocation = locationEngine.getLastKnownLocation();
        if (myLastLocation != null) {
            final double accuracy = (myLastLocation.horizontalAccuracyInMeters != null) ? myLastLocation.horizontalAccuracyInMeters: 0.0;
            addMyLocationToMap(myLastLocation.coordinates, accuracy);

            //Update the map viewport to be centered on the location.
            mapView.getCamera().lookAt(myLastLocation.coordinates, CAMERA_DISTANCE_IN_METERS);
        } else {
            addMyLocationToMap(defaultLocation, 0.0);

            //Update the map viewport to be centered on the location.
            mapView.getCamera().lookAt(defaultLocation, CAMERA_DISTANCE_IN_METERS);
        }

        startLocating();
    }

    private void startLocating() {
        locationEngine.addLocationStatusListener(locationStatusListener);
        locationEngine.addLocationListener(locationListener);
        locationEngine.start(LocationAccuracy.BEST_AVAILABLE);
    }

    public void stopLocating() {
        locationEngine.stop();
    }

    private void addMyLocationToMap(@NonNull GeoCoordinates geoCoordinates, @NonNull double accuracyRadiusInMeters) {
        //Transparent halo around the current location: the true geographic coordinates lie with a probability of 68% within that.
        locationAccuracyCircle = new MapPolygon(new GeoPolygon(new GeoCircle(geoCoordinates, accuracyRadiusInMeters)), colorAccuracy);
        //Solid circle on top of the current location.
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), R.drawable.red_dot);
        locationCenterCircle = new MapMarker(geoCoordinates, mapImage);

        //Add the circle to the map.
        mapView.getMapScene().addMapPolygon(locationAccuracyCircle);
        mapView.getMapScene().addMapMarker(locationCenterCircle);
    }

    private void updateMyLocation(@NonNull GeoCoordinates geoCoordinates, @NonNull double accuracyRadiusInMeters) {
        locationAccuracyCircle.setGeometry(new GeoPolygon(new GeoCircle(geoCoordinates, accuracyRadiusInMeters)));
        locationCenterCircle.setCoordinates(geoCoordinates);
    }
}
