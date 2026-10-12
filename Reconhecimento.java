package com.biometria;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class Reconhecimento {
    
    public static void iniciarReconhecimento() {
        System.out.println("A iniciar o sistema de reconhecimento...");

        File diretorioFotos = new File("controle-acesso/Fotos");

        System.out.println("Tentando ler fotos no diretório: " + diretorioFotos.getAbsolutePath());        
        FilenameFilter filtroJpg = (dir, nome) -> nome.endsWith(".jpg");
        File[] arquivos = diretorioFotos.listFiles(filtroJpg);

        if (arquivos == null || arquivos.length == 0) {
            javax.swing.JOptionPane.showMessageDialog(null, "Nenhuma foto encontrada para treino.\nPor favor, faça um cadastro primeiro.", "Erro de Reconhecimento", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        MatVector fotos = new MatVector(arquivos.length);
        Mat rotulos = new Mat(arquivos.length, 1, CV_32SC1);
        IntBuffer rotulosBuffer = rotulos.createBuffer();

        for (int i = 0; i < arquivos.length; i++) {
            Mat foto = imread(arquivos[i].getAbsolutePath(), IMREAD_GRAYSCALE);
            int id = Integer.parseInt(arquivos[i].getName().split("-")[0]);
            
            fotos.put(i, foto);
            rotulosBuffer.put(i, id);
        }

        FaceRecognizer reconhecedor = LBPHFaceRecognizer.create();
        reconhecedor.train(fotos, rotulos);
        System.out.println("Inteligência Artificial treinada com sucesso!");

        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        OpenCVFrameConverter.ToMat conversor = new OpenCVFrameConverter.ToMat();
        CascadeClassifier classificador = new CascadeClassifier("controle-acesso/haarcascade_frontalface_alt.xml");

        try {
            grabber.start();
            CanvasFrame canvas = new CanvasFrame("Controle de Acesso - Reconhecimento", CanvasFrame.getDefaultGamma() / grabber.getGamma());
            canvas.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE); 

            Connection conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/biometria_db", "root", "valentina231207");

            while (canvas.isVisible() && (grabber.grab()) != null) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    Mat imagemCores = conversor.convert(frame);
                    Mat imagemCinza = new Mat();
                    cvtColor(imagemCores, imagemCinza, COLOR_BGRA2GRAY);
                    
                    RectVector rostosDetetados = new RectVector();
                    classificador.detectMultiScale(imagemCinza, rostosDetetados);

                    for (int i = 0; i < rostosDetetados.size(); i++) {
                        Rect dadosRosto = rostosDetetados.get(i);
                        Mat rostoRecortado = new Mat(imagemCinza, dadosRosto);
                        Mat rostoPadronizado = new Mat();
                        resize(rostoRecortado, rostoPadronizado, new Size(160, 160));

                        int[] idPrevisto = new int[1];
                        double[] confianca = new double[1];
                        reconhecedor.predict(rostoPadronizado, idPrevisto, confianca);

                        String mensagem = "Desconhecido";
                        Scalar corCaixa = new Scalar(0, 0, 255, 0);

                        if (idPrevisto[0] != -1 && confianca[0] < 50.0) {
                            String sql = "SELECT nome FROM usuarios WHERE id = ?";
                            PreparedStatement stmt = conexao.prepareStatement(sql);
                            stmt.setInt(1, idPrevisto[0]);
                            ResultSet rs = stmt.executeQuery(); 
                            
                            if (rs != null && rs.next()) {
                                mensagem = "Acesso desbloqueado: " + rs.getString("nome");
                                corCaixa = new Scalar(0, 255, 0, 0);
                            }
                        } else {
                            mensagem = "Usuario nao encontrado";
                        }

                        rectangle(imagemCores, new Point(dadosRosto.x(), dadosRosto.y()), 
                                  new Point(dadosRosto.x() + dadosRosto.width(), dadosRosto.y() + dadosRosto.height()), 
                                  corCaixa, 2, LINE_AA, 0);
                                  
                        putText(imagemCores, mensagem, new Point(dadosRosto.x(), dadosRosto.y() - 10), 
                                FONT_HERSHEY_SIMPLEX, 0.7, corCaixa, 2, LINE_AA, false);
                    }
                    canvas.showImage(conversor.convert(imagemCores));
                }
            }
            
            grabber.stop();
            conexao.close();

        } catch (Exception e) {
            System.out.println("Erro durante o reconhecimento: " + e.getMessage());
        }
    }
}