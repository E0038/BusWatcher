package org.e38.android.buswatcher.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sergi on 4/1/16.
 */
public class BusPoint implements Serializable {
    /**
     * fecha falsa se usada para rellenar campos que no se usan
     */
    public static final String MOCK_FECHA_PROCESSO = "2016-04-01T00:00:00+02:00";
    /**
     * id false usada para rellenar campos que no se usan
     */
    public static final int MOCK_ID_LOC = 3;
    private String matricula, provider;
    private double longitut, latitut;
    private long time;


    public BusPoint() {
    }

    public BusPoint(String matricula, String provider, double longitut, double latitut, long time) {
        this.matricula = matricula;
        this.provider = provider;
        this.longitut = longitut;
        this.latitut = latitut;
        this.time = time;
    }

    public static List<BusPoint> fromJsonArray(JSONArray jsonArray) throws JSONException {
        List<BusPoint> busPoints = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            busPoints.add(fromJson(jsonArray.getJSONObject(i)));
        }
        return busPoints;
    }

    public static BusPoint fromJson(JSONObject jsonObject) throws JSONException {
        String matricula = jsonObject.getString("matricula");
        String provider = jsonObject.getString("provider");
        double longitut = jsonObject.getDouble("logitut"),
                latitut = jsonObject.getDouble("latitut");
        long time = jsonObject.getLong("time");
        return new BusPoint(matricula, provider, longitut, latitut, time);
    }

    public String getMatricula() {
        return matricula;
    }

    public BusPoint setMatricula(String matricula) {
        this.matricula = matricula;
        return this;
    }

    public String getProvider() {
        return provider;
    }

    public BusPoint setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public double getLongitut() {
        return longitut;
    }

    public BusPoint setLongitut(double longitut) {
        this.longitut = longitut;
        return this;
    }

    public double getLatitut() {
        return latitut;
    }

    public BusPoint setLatitut(double latitut) {
        this.latitut = latitut;
        return this;
    }

    public long getTime() {
        return time;
    }

    public BusPoint setTime(long time) {
        this.time = time;
        return this;
    }

    public Map<String, Object> toMap() {
        //'{"fechaProceso":"2016-04-01T00:00:00+02:00","idLoc":3,"latitut":120,"logitut":30,"matricula":"23456",
        // "provider":"gps","reciptId":16,"time":999999}'

        Map<String, Object> map = new HashMap<>();
        map.put("fechaProceso", MOCK_FECHA_PROCESSO);//da igual lo que pongamos se ignorara pero la api requiere que exista el campo
        map.put("idLoc", MOCK_ID_LOC);//se ignora
        map.put("matricula", matricula);
        map.put("provider", provider);
        map.put("logitut", longitut);
        map.put("latitut", latitut);
        map.put("time", time);

//        try {
//            for (Field field : getClass().getDeclaredFields()) {
//                map.put(field.getName(), field.get(this));
//            }
//        } catch (IllegalAccessException e) {
//            Log.e(getClass().getName(), "acces error", e);
//        }
        return map;
    }
}
