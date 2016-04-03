package org.e38.android.buswatcher.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sergi on 4/1/16.
 */
public class Buss implements Serializable {
    private String matricula;
    private List<Localizacion> localizacions = new ArrayList<>();

    public static class Localizacion {
        private double latitut, longitut;
        private long time;
        private String proveidor;
    }
}
