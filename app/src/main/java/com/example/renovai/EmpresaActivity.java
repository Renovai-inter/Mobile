package com.example.renovai;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renovai.view.EmpresaHomeActivity;

/** Compatibilidade para entradas antigas da área Empresa. */
public class EmpresaActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, EmpresaHomeActivity.class));
        finish();
    }
}
