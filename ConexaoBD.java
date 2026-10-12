package com.biometria;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBD {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/biometria_db";
        String username = "root";
        String password = "valentina231207";

        System.out.println("Conectando ao banco de dados...");

        try {
            Connection conexao = DriverManager.getConnection(url, username, password);
            System.out.println("Conexão bem-sucedida!");
            conexao.close();
        } catch (SQLException e) {
            System.out.println("Erro ao conectar ao banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
