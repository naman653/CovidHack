package com.hackathon.covid.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.libraries.places.api.model.Place;
import com.hackathon.covid.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationUtils {
    private static final String TAG = LocationUtils.class.getSimpleName();

    private LocationUtils() {
        // Never Use
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = Settings.Secure.LOCATION_MODE_OFF;
        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Throwable e) {
            Utils.logException(TAG, e);
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    //can be made more generic, but require to change all the occurences of getCompleteAddressString
    public static String getNonEditableAddressString(android.location.Address address) {
        String city = address.getLocality() != null ? address.getLocality() : "";
        String state = address.getAdminArea() != null ? ", " + address.getAdminArea() : "";
        String country = address.getCountryName() != null ? ", " + address.getCountryName() : "";
        String postalCode = address.getPostalCode() != null ? " - " + address.getPostalCode() : "";
        return city + state + country + postalCode;
    }

    public static String getEditableAddressString(android.location.Address address) {
        String city = address.getLocality() != null ? address.getLocality() : "";
        String state = address.getAdminArea() != null ? ", " + address.getAdminArea() : "";
        String postalCode = address.getPostalCode() != null ? " " + address.getPostalCode() : "";
        String country = address.getCountryName() != null ? ", " + address.getCountryName() : "";
        String replaceableAddress = city + state + postalCode + country;
        String editableAddress = address.getAddressLine(0).replaceAll(", " + replaceableAddress, "");
        return editableAddress;
    }

    public static void getBackendDataSegregated(com.hackathon.covid.data.Address address, EditText etAddress, TextView tvAddress) {
        if (address != null) {
            if (!address.getAddressLine1AsString().trim().equals("") && !address.getAddressLine2AsString().trim().equals("")) {
                etAddress.setText(String.format("%s, %s", address.getAddressLine1AsString(), address.getAddressLine2AsString()));
            } else {
                etAddress.setText(
                        !address.getAddressLine1AsString().trim().equals("") ? address.getAddressLine1AsString() : address.getAddressLine2AsString());
            }
            String city = !address.getCityAsString().equals("") ? address.getCityAsString() : "";
            String district = address.getDistrictAsString();
            String state = !address.getStateAsString().equals("") ? ", " + address.getStateAsString() : "";
            String country = !address.getCountryAsString().equals("") ? ", " + address.getCountryAsString() : "";
            String postalCode = !address.getPincode().equals("") ? " - " + address.getPincode() : "";
            tvAddress.setText(String.format("%s%s%s%s%s", city, district, state, country, postalCode));
        }
    }

    public static String getCompleteAddressString(Context context, double latitude, double longitude, String defaultValue) {
        String address = defaultValue;
        Address addressObj = getAddress(context, latitude, longitude);
        if (addressObj != null) {
            address = addressObj.getAddressLine(0);
        }
        return address;
    }

    public static Address getAddress(Context context, double latitude, double longitude) {
        Address address = null;
        List<Address> addressList = addressList(context, latitude, longitude, Locale.getDefault());
        if (!addressList.isEmpty()) {
            address = addressList.get(0);
        }
        return address;
    }

    public static List<Address> addressList(Context context, double latitude, double longitude, Locale locale) {
        List<Address> addressList = new ArrayList<>();
        if (Utils.isNetworkAvailable(context)) {
            try {
                Geocoder geocoder = new Geocoder(context, locale);
                addressList = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            } catch (Exception e) {
                Utils.logException(TAG, e);
            }
        } else {
            showToast(context, context.getString(R.string.internet_unavailable));
        }
        return addressList;
    }

    public static String addressLine1(Context context, double latitude, double longitude) {
        String addressLine1 = "";
        try {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(context, Locale.getDefault());

            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            addressLine1 = addresses.get(0).getSubAdminArea();
        } catch (Exception e) {
            Utils.logException(TAG, e);
        }
        return addressLine1;
    }

    public static String addressLine2(Context context, double latitude, double longitude) {
        String addressLine2 = "";
        try {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(context, Locale.getDefault());

            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            addressLine2 = addresses.get(0).getAddressLine(1);
        } catch (Exception e) {
            Utils.logException(TAG, e);
        }
        return addressLine2;
    }

    public static String city(Context context, double latitude, double longitude) {
        String city = "";
        try {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(context, Locale.getDefault());

            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            city = addresses.get(0).getLocality();
        } catch (Exception e) {
            Utils.logException(TAG, e);
        }
        return city;
    }

    public static String state(Context context, double latitude, double longitude) {
        String state = "";
        try {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(context, Locale.getDefault());
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            state = addresses.get(0).getAdminArea();
        } catch (Exception e) {
            Utils.logException(TAG, e);
        }
        return state;
    }

    public static String country(Context context, double latitude, double longitude) {
        String country = "";
        try {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(context, Locale.getDefault());
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            country = addresses.get(0).getCountryName();
        } catch (Exception e) {
            Utils.logException(TAG, e);
        }
        return country;
    }

    public static String postalCode(Context context, double latitude, double longitude) {
        String postalCode = "";
        try {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(context, Locale.getDefault());
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            postalCode = addresses.get(0).getPostalCode();
        } catch (Exception e) {
            Utils.logException(TAG, e);
        }
        return postalCode;
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double distance;
        Location locationA = new Location("Point A");
        locationA.setLatitude(lat1);
        locationA.setLongitude(lon1);

        Location locationB = new Location("Point B");
        locationB.setLatitude(lat2);
        locationB.setLongitude(lon2);
        distance = locationA.distanceTo(locationB) / 1000;   // in km
        return distance;
    }

    public static double distance1(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static String getAddressString(@NonNull Place place) {
        String desiredAddress = "";
        if (!Utils.isNullOrEmpty(place.getName()) && !Utils.isNullOrEmpty(place.getAddress())) {
            if (place.getAddress().toLowerCase().startsWith(place.getName().toLowerCase())) {
                desiredAddress = place.getAddress();
            } else {
                desiredAddress = place.getName() + ", " + place.getAddress();
            }
        } else if (Utils.isNullOrEmpty(place.getName()) && !Utils.isNullOrEmpty(place.getAddress())) {
            desiredAddress = place.getAddress();
        }
        return desiredAddress;
    }

    public static Spannable getSpannableAddressString(@NonNull String addressString) {
        int index = addressString.indexOf(",");
        String placeName = "";
        if (index != -1) {
            placeName = addressString.substring(0, index);
        }
        return new SpannableStringBuilder()
                .append(placeName, new StyleSpan(android.graphics.Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append(addressString.replace(placeName, ""));
    }
}
