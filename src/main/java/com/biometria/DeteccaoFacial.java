package com.biometria;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

public class DeteccaoFacial {
    public static void main(String[] args) {
        // Acede à câmara integrada
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        // Conversor necessário para formatar a imagem para o classificador
        OpenCVFrameConverter.ToMat conversor = new OpenCVFrameConverter.ToMat();
        
        // Carrega o modelo de inteligência artificial (Haar Cascade)
        CascadeClassifier classificador = new CascadeClassifier("controle-acesso/haarcascade_frontalface_alt.xml");

        try {
            System.out.println("A iniciar a câmara e o modelo de IA...");
            grabber.start();
            CanvasFrame canvas = new CanvasFrame("Detecção Facial - APS Biometria", CanvasFrame.getDefaultGamma() / grabber.getGamma());
            canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

            while (canvas.isVisible() && (grabber.grab()) != null) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    Mat imagemCores = conversor.convert(frame);
                    RectVector rostosDetetados = new RectVector();
                    
                    // A inteligência artificial analisa o frame à procura de rostos
                    classificador.detectMultiScale(imagemCores, rostosDetetados);
                    
                    // Desenha o retângulo vermelho à volta dos rostos encontrados
                    for (int i = 0; i < rostosDetetados.size(); i++) {
                        Rect dadosRosto = rostosDetetados.get(i);
                        rectangle(imagemCores, 
                                  new Point(dadosRosto.x(), dadosRosto.y()), 
                                  new Point(dadosRosto.x() + dadosRosto.width(), dadosRosto.y() + dadosRosto.height()), 
                                  new Scalar(0, 0, 255, 0), // Cor: Vermelho (BGR)
                                  2, 0, 0);                 // Espessura da linha
                    }
                    
                    // Mostra o resultado final no ecrã
                    canvas.showImage(conversor.convert(imagemCores));
                }
            }

            grabber.stop();
            canvas.dispose();
            System.out.println("Sistema de IA encerrado.");

        } catch (Exception e) {
            System.out.println("Erro durante a execução: " + e.getMessage());
            e.printStackTrace();
        }
    }
}