package com.biometria;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;

public class CadastroUsuario {
    
    public static void iniciarCadastro(String nome) {
        int idUtilizador = -1;

        try {
            Connection conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/biometria_db", "root", "valentina231207");
            String sql = "INSERT INTO usuarios (nome) VALUES (?)";
            PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, nome);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                idUtilizador = rs.getInt(1);
                System.out.println("Utilizador inserido no MySQL com o ID: " + idUtilizador);
            }
            conexao.close();
        } catch (Exception e) {
            System.out.println("Erro na Base de Dados: " + e.getMessage());
            return; 
        }

        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        OpenCVFrameConverter.ToMat conversor = new OpenCVFrameConverter.ToMat();
        CascadeClassifier classificador = new CascadeClassifier("controle-acesso/haarcascade_frontalface_alt.xml");

        try {
            grabber.start();
            CanvasFrame canvas = new CanvasFrame("Registo Biométrico", CanvasFrame.getDefaultGamma() / grabber.getGamma());
            canvas.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE); 

            boolean capturaConcluida = false;
            long tempoInicio = 0; 
            int fotosCapturadas = 0;
            int totalFotos = 25;

            while (canvas.isVisible() && (grabber.grab()) != null && !capturaConcluida) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    Mat imagemCores = conversor.convert(frame);
                    Mat imagemCinza = new Mat();
                    
                    cvtColor(imagemCores, imagemCinza, COLOR_BGRA2GRAY);
                    
                    RectVector rostosDetetados = new RectVector();
                    classificador.detectMultiScale(imagemCinza, rostosDetetados);

                    if (rostosDetetados.size() == 1) {
                        Rect dadosRosto = rostosDetetados.get(0);
                        
                        rectangle(imagemCores, new Point(dadosRosto.x(), dadosRosto.y()), 
                                  new Point(dadosRosto.x() + dadosRosto.width(), dadosRosto.y() + dadosRosto.height()), 
                                  new Scalar(0, 255, 0, 0), 2, LINE_AA, 0);

                        if (tempoInicio == 0 && fotosCapturadas == 0) {
                            tempoInicio = System.currentTimeMillis();
                        }
                        
                        long tempoDecorrido = System.currentTimeMillis() - tempoInicio;
                        int segundosRestantes = 5 - (int) (tempoDecorrido / 1000);
                        
                        if (segundosRestantes > 0 && fotosCapturadas == 0) {
                            putText(imagemCores, "Foto em: " + segundosRestantes + "s", new Point(30, 50), 
                                    FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(0, 255, 255, 0), 2, LINE_AA, false);
                        } 
                        else {
                            Mat rostoRecortado = new Mat(imagemCinza, dadosRosto);
                            Mat rostoPadronizado = new Mat();
                            resize(rostoRecortado, rostoPadronizado, new Size(160, 160));

                            fotosCapturadas++;
                            
                            String caminhoFoto = "controle-acesso/fotos/" + idUtilizador + "-" + nome.replaceAll("\\s+", "") + "-" + fotosCapturadas + ".jpg";
                            imwrite(caminhoFoto, rostoPadronizado);
                            
                            putText(imagemCores, "A capturar: " + fotosCapturadas + "/" + totalFotos, new Point(30, 50), 
                                    FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(0, 0, 255, 0), 2, LINE_AA, false);
                            
                            System.out.println("Foto " + fotosCapturadas + " guardada.");

                            if (fotosCapturadas >= totalFotos) {
                                System.out.println("Todas as " + totalFotos + " fotos foram capturadas com sucesso!");
                                capturaConcluida = true; 
                            }
                        }
                    } else {
                        if (fotosCapturadas == 0) tempoInicio = 0;
                        putText(imagemCores, "Posicione o rosto...", new Point(30, 50), 
                                FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(0, 0, 255, 0), 2, LINE_AA, false);
                    }
                    
                    canvas.showImage(conversor.convert(imagemCores));
                }
            }

            grabber.stop();
            grabber.close();
            classificador.close();
            canvas.dispose();

        } catch (Exception e) {
            System.out.println("Erro na captura de vídeo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}