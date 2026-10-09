package com.biometria;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class CadastroUsuario {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o nome do utilizador a registar: ");
        String nome = scanner.nextLine();

        int idUtilizador = -1;

        // 1. LIGAÇÃO À BASE DE DADOS E INSERÇÃO DO NOME
        try {
            Connection conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/biometria_db", "root", "valentina231207");
            // O Statement.RETURN_GENERATED_KEYS serve para o MySQL nos devolver o ID que acabou de criar
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
            scanner.close();
        } catch (Exception e) {
            System.out.println("Erro na Base de Dados: " + e.getMessage());
            return; // Pára a execução se a base de dados falhar
        }

        // 2. CAPTURA DO ROSTO COM A CÂMARA
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        OpenCVFrameConverter.ToMat conversor = new OpenCVFrameConverter.ToMat();
        CascadeClassifier classificador = new CascadeClassifier("controle-acesso/haarcascade_frontalface_alt.xml");

        try {
            grabber.start();
            CanvasFrame canvas = new CanvasFrame("Olhe para a câmara para registar o rosto", CanvasFrame.getDefaultGamma() / grabber.getGamma());
            canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

            boolean rostoCapturado = false;

            while (canvas.isVisible() && (grabber.grab()) != null && !rostoCapturado) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    Mat imagemCores = conversor.convert(frame);
                    Mat imagemCinza = new Mat();
                    
                    // Converte para tons de cinza (a Inteligência Artificial treina melhor sem cores)
                    cvtColor(imagemCores, imagemCinza, COLOR_BGR2GRAY);
                    
                    RectVector rostosDetetados = new RectVector();
                    classificador.detectMultiScale(imagemCinza, rostosDetetados);

                    // Se detetar 1 rosto, faz a captura
                    if (rostosDetetados.size() == 1) {
                        Rect dadosRosto = rostosDetetados.get(0);
                        
                        // Recorta a imagem para apanhar apenas a cara e não o cenário de fundo
                        Mat rostoRecortado = new Mat(imagemCinza, dadosRosto);
                        
                        // Redimensiona para um tamanho padrão (160x160 píxeis) para facilitar o treino futuro
                        Mat rostoPadronizado = new Mat();
                        resize(rostoRecortado, rostoPadronizado, new Size(160, 160));

                        // Define o caminho e o nome do ficheiro (Ex: controle-acesso/fotos/1-Joao.jpg)
                        String caminhoFoto = "controle-acesso/fotos/" + idUtilizador + "-" + nome.replaceAll("\\s+", "") + ".jpg";
                        
                        // Guarda a imagem no disco
                        imwrite(caminhoFoto, rostoPadronizado);
                        System.out.println("Rosto capturado e guardado com sucesso em: " + caminhoFoto);
                        
                        rostoCapturado = true; // Muda para true para sair do laço (while) e desligar a câmara
                    }
                    
                    canvas.showImage(conversor.convert(imagemCores));
                }
            }

            grabber.stop();
            grabber.close();
            classificador.close();
            canvas.dispose();
            System.out.println("Cadastro finalizado com sucesso!");

        } catch (Exception e) {
            System.out.println("Erro na captura de vídeo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}