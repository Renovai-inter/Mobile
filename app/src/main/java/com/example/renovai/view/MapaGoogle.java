package com.example.renovai.view;

import android.annotation.SuppressLint;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@link MapaRota} no Google Maps (Maps SDK for Android). */
final class MapaGoogle implements MapaRota {

    private final GoogleMap mapa;
    private final View container;
    private final float densidade;
    private final List<Marker> marcadores = new ArrayList<>();
    private Polyline linhaFeita, linhaRestante;

    MapaGoogle(GoogleMap mapa, View container, AoTocar aoTocar) {
        this.mapa = mapa;
        this.container = container;
        this.densidade = container.getResources().getDisplayMetrics().density;
        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.setOnMarkerClickListener(mk -> {
            Object tag = mk.getTag();
            if (tag instanceof Integer) aoTocar.parada((Integer) tag);
            return true;
        });
    }

    @Override
    public void desenhar(List<Pino> pinos, List<double[]> feito, List<double[]> resto) {
        for (Marker m : marcadores) m.remove();
        marcadores.clear();
        if (linhaFeita != null) linhaFeita.remove();
        if (linhaRestante != null) linhaRestante.remove();
        linhaFeita = linhaRestante = null;
        if (feito.size() >= 2)
            linhaFeita = mapa.addPolyline(new PolylineOptions().addAll(latLngs(feito))
                    .color(MotoristaNavegacaoActivity.VERDE).width(6 * densidade));
        if (resto.size() >= 2)
            linhaRestante = mapa.addPolyline(new PolylineOptions().addAll(latLngs(resto))
                    .color(MotoristaNavegacaoActivity.CINZA).width(5 * densidade)
                    .pattern(Arrays.asList(new Dash(20 * densidade), new Gap(10 * densidade))));
        for (Pino p : pinos) {
            Marker m = mapa.addMarker(new MarkerOptions()
                    .position(new LatLng(p.ponto[0], p.ponto[1]))
                    .anchor(0.5f, 0.5f)
                    .zIndex(p.destaque ? 2f : 1f)
                    .title("Parada " + (p.indice + 1))
                    .icon(BitmapDescriptorFactory.fromBitmap(p.icone)));
            if (m != null) {
                m.setTag(p.indice);
                marcadores.add(m);
            }
        }
    }

    private static List<LatLng> latLngs(List<double[]> pontos) {
        List<LatLng> l = new ArrayList<>(pontos.size());
        for (double[] p : pontos) l.add(new LatLng(p[0], p[1]));
        return l;
    }

    @Override
    public void enquadrar(List<double[]> pontos) {
        if (pontos.isEmpty()) return;
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (double[] p : pontos) b.include(new LatLng(p[0], p[1]));
        int pad = (int) (48 * densidade);
        container.post(() -> {
            try {
                if (pontos.size() == 1) mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(b.build().getCenter(), 15f));
                else mapa.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), pad));
            } catch (IllegalStateException semTamanho) {
                mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(b.build().getCenter(), 13f));
            }
        });
    }

    @Override
    public void centralizar(double[] ponto) {
        mapa.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(ponto[0], ponto[1])));
    }

    @Override
    public void posicao(double[] ponto) {
        // o ponto azul do próprio SDK acompanha o motorista
    }

    @SuppressLint("MissingPermission")
    @Override
    public void localizacaoPermitida() {
        mapa.setMyLocationEnabled(true);
    }

    @Override
    public void retomar() {}

    @Override
    public void pausar() {}

    @Override
    public void destruir() {}
}
