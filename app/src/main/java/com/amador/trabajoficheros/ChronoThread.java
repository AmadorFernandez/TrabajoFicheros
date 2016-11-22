package com.amador.trabajoficheros;

/**
 * Created by amador on 21/11/16.
 */

public class ChronoThread extends Thread {

    private int intervalo;
    private OnTickListener listener;

    //Interfaz para el evento
    public interface OnTickListener{

        void tick();
    }

    public void setOnTickListener(OnTickListener listener){

        this.listener = listener;
    }


    public ChronoThread(int intervalo){

        this.intervalo = intervalo;

    }

    @Override
    public void run() {

        while (true){

            if(listener != null){

                //Lanza el evento
                listener.tick();
                try {
                    //Duerme el tiempo que la ha sido dado
                    sleep(this.intervalo);
                } catch (InterruptedException e) {
                    break; //Sale del bucle termina el run y el hilo muere
                }
            }
        }
    }
}
