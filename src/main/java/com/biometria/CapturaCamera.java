package com.biometria;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameGrabber;

public class CapturaCamera {
    public static void main(String[] args) {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);

        try {
            System.out.println("Iniciando a câmera...");
            grabber.start();

            CanvasFrame canvas = new CanvasFrame("Câmera - APS Biometria", CanvasFrame.getDefaultGamma() / grabber.getGamma());
            canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            
            System.out.println("Câmera ligafa! Feche a janela para encerrar o progrmama.");

            while (canvas.isVisible() && (grabber.grab()) != null) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    canvas.showImage(frame);
                }
            }

            grabber.stop();
            grabber.close();
            canvas.dispose();
            System.out.println("Programa encerrado.");
            
        } catch (Exception e) {
            System.err.println("Erro ao capturar a câmera: " + e.getMessage());
            e.printStackTrace();
        }
    }    
}
