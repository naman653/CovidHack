package com.hackathon.covid.data;


import androidx.annotation.Nullable;

import java.util.Objects;


public class MultiLingual {
    public String en;
    public String hi;

    public MultiLingual() {}

    private String getEn() {
        if (en != null && !en.isEmpty()) {
            return en;
        } else {
            return hi;
        }
    }

    public void setEn(String en) {
        this.en = en;
    }

    private String getHi() {
        if (hi != null && !hi.isEmpty()) {
            return hi;
        } else {
            return en;
        }
    }

    public void setHi(String hi) {
        this.hi = hi;
    }

    public String getString() {
        return getEn();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof MultiLingual) {
            MultiLingual m1 = (MultiLingual) obj;
            boolean enEqual = m1.en != null ? m1.en.equals(en) : true;
            boolean hiEqual = m1.hi != null ? m1.hi.equals(hi) : true;
            return enEqual && hiEqual;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(en, hi);
    }
}
