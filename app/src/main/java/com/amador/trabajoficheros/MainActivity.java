package com.amador.trabajoficheros;


import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity {


    private Button btnDescargar, btnDetener;
    private EditText edtUrlImages, edtUrlFrases;
    private static final String URL_ERROR  = "https://itishereapp.000webhostapp.com/escribir.php";
    private static final String URL_ERROR_FILE  = "https://itishereapp.000webhostapp.com/errores.txt";
    private static final String PARAM_ERROR_NAME = "contenido";
    private ImageView imv;
    private TextView txvFrases;
    private String[] rutasImagenes = new String[0];
    private String[] rutasFrases = new String[0];
    private int posFrases;
    private int posImagenes;
    private int intervalo;

    //Manejador para el evento tick
    private ChronoThread.OnTickListener listener = new ChronoThread.OnTickListener() {
        @Override
        public void tick() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Verifica que pueda mostrar frases
                    if(rutasFrases != null && rutasFrases.length > 0){

                        //Recorre las frases de forma circular
                        mostrarFrases(posFrases++%rutasFrases.length);
                    }

                    //Verifica que pueda mostrar imagenes
                    if(rutasImagenes != null && rutasImagenes.length > 0){

                        //Recorre las imagenes de forma circular
                        mostrarImagenes(posImagenes++%rutasImagenes.length);
                    }
                }
            });
        }
    };
    private ChronoThread chrono;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }



    private void init() {


        edtUrlFrases = (EditText)findViewById(R.id.edtUrlFrases);
        edtUrlImages = (EditText)findViewById(R.id.edtUrlImages);
        btnDescargar = (Button)findViewById(R.id.btnDescargar);
        imv = (ImageView)findViewById(R.id.imv);
        txvFrases = (TextView)findViewById(R.id.txvFrases);
        btnDetener = (Button)findViewById(R.id.btnDetener);
        posFrases = 0;
        posImagenes = 0;


        btnDetener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(chrono != null){

                    //Detiene el hilo
                    chrono.interrupt();

                }

                //Resetea al estado original
                btnDescargar.setVisibility(View.VISIBLE);
                btnDetener.setVisibility(View.GONE);
                imv.setImageDrawable(null);
                txvFrases.setText("");
                rutasFrases = null;
                rutasImagenes = null;
            }
        });


        btnDescargar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                String rutaImagenes;
                String rutaFrases;

                //Si hay conexión
                if(isNetworkAvailable()){

                    //Extrae las rutas
                    rutaFrases = edtUrlFrases.getText().toString();

                    rutaImagenes = edtUrlImages.getText().toString();

                    //No dejo que ningun campo sea vacio
                    if(rutaFrases.isEmpty() || rutaImagenes.isEmpty()){

                        edtUrlFrases.setError("Ningun campo puede ser vacio");

                    }else {


                        //A currar
                        descargar(rutaImagenes, rutaFrases);
                    }

                }else {

                    Toast.makeText(MainActivity.this, "No hay una conexion a internet", Toast.LENGTH_LONG).show();
                }
            }
        });

        edtUrlFrases.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                edtUrlFrases.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        edtUrlImages.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                edtUrlImages.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

    }

    //Extrae el tiempo para el intervalo
    private int leerTiempo() {

        StreamIO stream = new StreamIO(null);
        int resultado = Integer.parseInt(stream.readLine(getResources().openRawResource(R.raw.intervalo)));
        return resultado;
    }

    //Muestra la frase en la posicion dada
    private void mostrarFrases(int pos){

        String contenido = rutasFrases[pos];
        txvFrases.setText(contenido);
    }

    //Muestra las imagenes en la posicion dada
    private void mostrarImagenes(int pos){

        try {
            Picasso.with(MainActivity.this).load(rutasImagenes[pos]).placeholder(R.drawable.descargando)
                    .error(R.drawable.error).into(imv);
        } catch (IllegalArgumentException e) {

            Picasso.with(MainActivity.this).load(R.drawable.error);
        }
    }


    //Lo tipico
    private boolean isNetworkAvailable(){
        boolean result = false;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            result = true;

        return result;

    }


    private void descargar(final String urlImagenes, String urlFrases){

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        final ProgressDialog progress = new ProgressDialog(MainActivity.this);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setMessage("Descargando. . .");
        progress.setCancelable(false);
        progress.show();
        //Peticion para las imagenes
        StringRequest respuestaImagenes = new StringRequest(Request.Method.GET, urlImagenes, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {


                rutasImagenes = response.split("\n");

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                //Si hay error lo muestra y lo escribe en el fichero del servidor
                Calendar calendar = new GregorianCalendar();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh/mm/yyyy hh:mm:ss");
                String me = error.toString();
                Toast.makeText(MainActivity.this, me, Toast.LENGTH_LONG).show();
                RequestParams params = new RequestParams();
                params.put(PARAM_ERROR_NAME, urlImagenes+" Error: "+error.toString()+" Fecha: "+simpleDateFormat.format(calendar.getTime()));
                RestClient.post(URL_ERROR, params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                        Toast.makeText(MainActivity.this, "No se pudo escribir el error en el servidor", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {

                        Toast.makeText(MainActivity.this, "El error ha sido escrito en "+URL_ERROR_FILE, Toast.LENGTH_LONG).show();
                    }
                });

            }
        });

        //Peticion para las frases
        final StringRequest respuestaFrases = new StringRequest(Request.Method.GET, urlFrases, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

               rutasFrases = response.split("\n");

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                //Si hay error lo muestra y lo escribe en el fichero del servidor
                Calendar calendar = new GregorianCalendar();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh/mm/yyyy hh:mm:ss");
                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                RequestParams params = new RequestParams();
                params.put(PARAM_ERROR_NAME, urlImagenes+" Error: "+error.toString()+" Fecha: "+simpleDateFormat.format(calendar.getTime()));
                RestClient.post(URL_ERROR, params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                        Toast.makeText(MainActivity.this, "No se pudo escribir el error en el servidor", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {

                        Toast.makeText(MainActivity.this, "El error ha sido escrito en "+URL_ERROR_FILE, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });


        //Cuando finalizan las peticiones inicia el chrono
        requestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {


            @Override
            public void onRequestFinished(Request<Object> request) {

                progress.dismiss();
                intervalo = leerTiempo();
                chrono = new ChronoThread(intervalo * 1000);
                chrono.setOnTickListener(listener);
                chrono.start();
                btnDetener.setVisibility(View.VISIBLE);
                btnDescargar.setVisibility(View.GONE);


            }
        });

        //A la cola
        requestQueue.add(respuestaImagenes);
        requestQueue.add(respuestaFrases);
    }




}
