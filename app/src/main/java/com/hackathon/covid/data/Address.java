package com.hackathon.covid.data;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Embedded;

import com.hackathon.covid.utils.Utils;

public class Address {

    private String pincode;
    @Embedded(prefix = "addressLine1")
    private MultiLingual addressLine1;
    @Embedded(prefix = "addressLine2")
    private MultiLingual addressLine2;
    @Embedded(prefix = "city")
    private MultiLingual city;
    @Embedded(prefix = "district")
    private MultiLingual district;
    @Embedded(prefix = "state")
    private MultiLingual state;
    @Embedded(prefix = "country")
    private MultiLingual country;

    public Address() {}

    public String getAddressLine1AsString() {
        return addressLine1 != null ? addressLine1.getString().trim() : "";
    }

    public void setAddressLine1(MultiLingual addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2AsString() {
        return addressLine2 != null ? addressLine2.getString().trim() : "";
    }

    public void setAddressLine2(MultiLingual addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCityAsString() {
        return city != null ? city.getString().trim() : "";
    }

    public void setCity(MultiLingual city) {
        this.city = city;
    }

    public String getDistrictAsString() {
        return district != null ? district.getString().trim() : "";
    }

    public void setDistrict(MultiLingual district) {
        this.district = district;
    }

    public String getStateAsString() {
        return state != null ? state.getString().trim() : "";
    }

    public void setState(MultiLingual state) {
        this.state = state;
    }

    public String getCountryAsString() {
        return country != null ? country.getString().trim() : "";
    }

    public void setCountry(MultiLingual country) {
        this.country = country;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public MultiLingual getAddressLine1() {
        return addressLine1;
    }

    public MultiLingual getAddressLine2() {
        return addressLine2;
    }

    public MultiLingual getCity() {
        return city;
    }

    public MultiLingual getDistrict() {
        return district;
    }

    public MultiLingual getState() {
        return state;
    }

    public MultiLingual getCountry() {
        return country;
    }

    @NonNull
    @Override
    public String toString() {
        String address = "";
        if (!Utils.isNullOrEmpty(getAddressLine1AsString())) {
            address += getAddressLine1AsString() + ",\n";
        }
        if (!Utils.isNullOrEmpty(getAddressLine2AsString())) {
            address += getAddressLine2AsString() + ",\n";
        }
        if (!Utils.isNullOrEmpty(getCityAsString())) {
            address += getCityAsString() + ",\n";
        }
        if (!Utils.isNullOrEmpty(getStateAsString())) {
            address += getStateAsString();
        }
        if (!getCountryAsString().toLowerCase().contains("india")) {
            address = address + ",\n" + getCountryAsString();
        }

        return address;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Address) {
            Address address = (Address) obj;
            boolean addressLine1Equal = Objects.equals(addressLine1, address.addressLine1);
            boolean addressLine2Equal = Objects.equals(addressLine2, address.addressLine2);
            boolean cityEqual = Objects.equals(city, address.city);
            boolean districtEqual = Objects.equals(district, address.district);
            boolean stateEqual = Objects.equals(state, address.state);
            boolean countryEqual = Objects.equals(country, address.country);
            boolean pincodeEqual = Objects.equals(pincode, address.pincode);

            return addressLine1Equal && addressLine2Equal && cityEqual && districtEqual && pincodeEqual && countryEqual && stateEqual;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pincode, addressLine1, addressLine2, city, district, state, country);
    }

    public String getAddressUpto(String level) {
        String address = "";
        switch (level) {
            case "AddressLine1":
                address += getAddressLine1AsString();
                break;
            case "AddressLine2":
                address = getAddressUpto("AddressLine1") + ", ";
                address += getAddressLine2AsString();
                break;
            case "City":
                address = getAddressUpto("AddressLine2") + ", ";
                address += getCityAsString();
                break;
            case "District":
                address = getAddressUpto("City") + ", ";
                address += getDistrictAsString();
                break;
            case "State":
                address = getAddressUpto("District") + ", ";
                address += getStateAsString();
                break;
            default:
                address = getAddressUpto("State") + ", ";
                address += getCountryAsString();
                break;
        }
        address = address.trim();
        while (address.contains(", ,")) {
            address = address.replace(", ,", ",");
        }
        while (address.contains("  ")) {
            address = address.replace("  ", " ");
        }
        if (address.endsWith(",")) {
            address = address.substring(0, address.length() - 1);
        }
        if (address.startsWith(",")) {
            address = address.substring(1);
        }
        return address.trim();
    }

    public String getLastTwoFields(String level) {
        String address = getAddressUpto(level);
        String[] splits = address.split(",");
        if (splits.length == 1) {
            return splits[0];
        } else {
            return splits[splits.length - 2] + ", " + splits[splits.length - 1];
        }
    }
}