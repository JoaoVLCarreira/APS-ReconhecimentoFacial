package com.biometria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuPrincipal extends JFrame {

    public MenuPrincipal() {
        setTitle("Controle de Acesso Biométrico");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 1, 15, 15));

        JLabel titulo = new JLabel("Sistema de Segurança", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 22));

        JButton btnCadastrar = new JButton("Cadastrar Novo Usuário");
        btnCadastrar.setFont(new Font("Arial", Font.PLAIN, 18));

        JButton btnAcessar = new JButton("Acessar (Reconhecimento)");
        btnAcessar.setFont(new Font("Arial", Font.PLAIN, 18));

        btnCadastrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nome = JOptionPane.showInputDialog(null, "Digite o nome do novo usuário:", "Cadastro", JOptionPane.QUESTION_MESSAGE);
                if (nome != null && !nome.trim().isEmpty()) {
                    CadastroUsuario.iniciarCadastro(nome);
                }
            }
        });

        btnAcessar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Reconhecimento.iniciarReconhecimento();
            }
        });

        add(titulo);
        add(btnCadastrar);
        add(btnAcessar);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MenuPrincipal().setVisible(true);
        });
    }
}