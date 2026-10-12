# Relatório do Projeto APS — Reconhecimento Facial

## 1. Visão geral

Este relatório descreve o estado do projeto observado nos arquivos disponíveis no momento da análise. O projeto, identificado como **APS — Reconhecimento Facial**, contém um módulo Java chamado `controle-acesso`, com exemplos executáveis de captura de vídeo por webcam, detecção de faces em tempo real e conexão de teste a um banco MySQL.

Apesar do nome “Reconhecimento Facial”, a funcionalidade de visão computacional implementada neste estado é **detecção facial**: o programa localiza regiões que parecem conter rostos e desenha retângulos sobre elas. Não foi encontrada implementação de identificação de pessoas, comparação biométrica, cadastro de usuários faciais ou decisão de autorização de acesso.

> **Atualização:** as seções 1 a 11 registram um levantamento anterior do projeto. A seção 12 documenta o código atual, incluindo cadastro de usuários, coleta de imagens, treinamento LBPH e tentativa de identificação integrada ao MySQL. Em caso de divergência, considere a seção 12 como a descrição mais recente.

### Resumo do estado observado

| Aspecto | Estado observado |
|---|---|
| Linguagem | Java |
| Build | Maven |
| Versão de compilação declarada | Java 17 |
| Módulo principal | `controle-acesso` |
| Código-fonte da aplicação | 3 classes Java |
| Visão computacional | JavaCV / OpenCV com classificador Haar |
| Banco de dados | Driver MySQL declarado e um programa de teste de conexão |
| Interface | Janela desktop para exibição da câmera |
| Testes automatizados | Não foram encontrados no escopo inspecionado |
| Documentação do projeto já existente | Não foi encontrado README na listagem inspecionada |

## 2. Estrutura do projeto

```text
APS - Reconhecimento Facial/
├── RELATORIO_PROJETO_APS_RECONHECIMENTO_FACIAL.md
├── .github/
│   └── modernize/
│       └── java-upgrade/
│           └── hooks/scripts/
│               ├── recordToolUse.ps1
│               └── recordToolUse.sh
└── controle-acesso/
    ├── pom.xml
    ├── haarcascade_frontalface_alt.xml
    ├── src/main/java/com/biometria/
    │   ├── CapturaCamera.java
    │   ├── ConexaoBD.java
    │   └── DeteccaoFacial.java
    └── target/classes/com/biometria/
        ├── CapturaCamera.class
        ├── ConexaoBD.class
        └── DeteccaoFacial.class
```

`target/classes` contém arquivos compilados que são artefatos de build, e não código-fonte. Sua presença, isoladamente, não comprova que a versão atual do código tenha sido compilada com sucesso.

Os arquivos em `.github/modernize/java-upgrade/hooks/scripts/` são scripts de apoio à automação de modernização. Eles não fazem parte dos fluxos funcionais de captura, detecção facial ou acesso ao banco de dados descritos abaixo.

## 3. Tecnologias e dependências

As informações desta seção vêm do [pom.xml](./controle-acesso/pom.xml).

### Linguagem e build

- **Java:** nível de origem e destino configurados como `17`.
- **Maven:** manifesto do projeto no formato Maven POM.
- **Coordenadas Maven:** `com.biometria:controle-acesso:1.0-SNAPSHOT`.
- Não há, no POM inspecionado, configuração explícita de plugins Maven, empacotamento especial, execução de testes ou definição adicional de versão do compilador.

### Dependências declaradas

| Dependência | Versão | Uso identificável no código |
|---|---:|---|
| `org.bytedeco:javacv-platform` | `1.5.10` | Acesso à câmera, conversão de frames, interface de vídeo e APIs OpenCV utilizadas na detecção |
| `com.mysql:mysql-connector-j` | `8.3.0` | Driver JDBC para conexão ao MySQL |

O sufixo `-platform` da dependência JavaCV indica o artefato destinado a incluir suporte nativo às plataformas atendidas pelo pacote. A execução ainda depende de compatibilidade com o sistema operacional, câmera, bibliotecas nativas e ambiente gráfico.

### Recursos externos necessários para executar os fluxos

- Uma webcam acessível como dispositivo `0` para os dois programas de câmera.
- Ambiente gráfico desktop compatível com as janelas Swing/JavaCV.
- O arquivo de classificador Haar presente no módulo.
- Para o teste de banco: servidor MySQL local, porta `3306`, banco `biometria_db` e credenciais válidas.

## 4. Componentes e comportamentos implementados

### 4.1 `CapturaCamera`

Arquivo: [CapturaCamera.java](./controle-acesso/src/main/java/com/biometria/CapturaCamera.java)

Esta classe oferece um programa independente com método `main` que:

1. Cria um `OpenCVFrameGrabber` apontado para a câmera de índice `0`.
2. Inicia a captura.
3. Abre uma janela intitulada “Câmera - APS Biometria”.
4. Enquanto a janela estiver visível e houver frames, mostra a imagem capturada.
5. Ao sair do loop, interrompe e fecha o grabber, descarta a janela e imprime uma mensagem de encerramento.
6. Em caso de exceção, informa o erro e imprime o stack trace.

**Observação técnica:** a condição do `while` chama `grabber.grab()` para verificar se há frame e o corpo chama `grabber.grab()` novamente. Assim, o frame obtido na condição é descartado antes da leitura usada para exibição, o que pode reduzir a frequência de frames apresentada.

### 4.2 `DeteccaoFacial`

Arquivo: [DeteccaoFacial.java](./controle-acesso/src/main/java/com/biometria/DeteccaoFacial.java)

Esta classe oferece outro programa independente com método `main`. O fluxo é:

1. Abre a câmera de índice `0`.
2. Cria um conversor de frames JavaCV para `Mat` do OpenCV.
3. Carrega `controle-acesso/haarcascade_frontalface_alt.xml` em um `CascadeClassifier`.
4. Inicia a captura e exibe uma janela “Detecção Facial - APS Biometria”.
5. Para cada frame lido, converte a imagem para `Mat` e executa `detectMultiScale`.
6. Para cada retângulo retornado pelo classificador, desenha um contorno vermelho (representado em BGR) de espessura `2`.
7. Converte a imagem processada para `Frame` e a exibe na janela.
8. Ao encerrar normalmente, interrompe a captura, descarta a janela e imprime uma mensagem.
9. Em caso de exceção, imprime a mensagem e o stack trace.

O classificador especifica um cascade do tipo `BOOST` com características `HAAR`, adequado à detecção de faces conforme o modelo fornecido. Ele localiza faces; não produz, no código inspecionado, uma identidade ou um identificador biométrico.

**Observações técnicas:**

- A condição do loop também captura um frame e o corpo captura outro, podendo descartar um frame por iteração.
- O caminho do classificador é relativo ao diretório de trabalho. Iniciar a aplicação a partir de diretórios diferentes pode impedir que o modelo seja localizado.
- A validação do retorno do classificador, o tratamento de recursos e a liberação de objetos nativos não aparecem como etapas explícitas no fluxo normal.

### 4.3 `ConexaoBD`

Arquivo: [ConexaoBD.java](./controle-acesso/src/main/java/com/biometria/ConexaoBD.java)

Esta classe é um programa independente de verificação de conectividade JDBC. Ela:

1. Tenta conectar-se ao banco `biometria_db` em um MySQL local, na porta padrão explicitada na URL.
2. Usa o driver JDBC disponível no classpath.
3. Imprime uma mensagem em caso de sucesso e fecha a conexão.
4. Em caso de `SQLException`, imprime a mensagem e o stack trace.

As credenciais de conexão estão escritas diretamente no código-fonte. Por segurança, este relatório não reproduz o valor da senha. Além de dificultar a configuração por ambiente, uma credencial real incluída no fonte deve ser considerada exposta: recomenda-se removê-la do código, substituir/rotacionar a senha e fornecê-la por configuração externa protegida.

No escopo inspecionado, não foram encontrados comandos SQL, operações CRUD, entidades, mapeamentos ORM ou definição do esquema do banco. Portanto, há um teste de conexão, mas não há evidência de persistência de registros biométricos ou de controle de acesso associado ao banco.

## 5. Arquivo de modelo facial

O arquivo [haarcascade_frontalface_alt.xml](./controle-acesso/haarcascade_frontalface_alt.xml) é referenciado pela classe de detecção e contém a estrutura XML de um classificador em cascata OpenCV, com tipo `BOOST` e características `HAAR`.

Esse arquivo representa o modelo usado para localizar faces na imagem. Ele não corresponde a uma base de pessoas cadastradas nem a dados de identidade dos usuários.

## 6. Fluxos funcionais encontrados

```text
CapturaCamera.main
  -> câmera 0
  -> OpenCVFrameGrabber
  -> CanvasFrame
  -> exibição dos frames

DeteccaoFacial.main
  -> câmera 0
  -> OpenCVFrameGrabber
  -> conversão Frame -> Mat
  -> CascadeClassifier (Haar)
  -> detectMultiScale
  -> desenho dos retângulos
  -> exibição do frame processado

ConexaoBD.main
  -> DriverManager / JDBC
  -> MySQL local (biometria_db)
  -> confirmação ou erro de conexão
```

As três classes são pontos de entrada `main` separados. Não foi encontrada evidência de que uma classe invoque as outras ou de que exista uma aplicação única que coordene câmera, detecção e autorização.

## 7. Dados e persistência

Na versão observada, o projeto não declara um modelo de domínio persistente. Não foram encontrados:

- Classes de usuário, pessoa, funcionário, visitante ou credencial biométrica.
- Tabelas ou scripts de criação do banco.
- Mapeamentos JPA/Hibernate ou outro ORM.
- Operações de leitura ou gravação no banco.
- Relação entre o resultado da detecção facial e uma decisão de acesso.

A conexão MySQL parece isolada como verificação técnica; o programa fecha a conexão após confirmar que ela foi estabelecida.

## 8. Interface e interação

A interface encontrada é uma janela desktop criada por `CanvasFrame`, e não uma aplicação web. O usuário inicia uma das classes com `main`, observa o fluxo de vídeo e encerra fechando a janela. Na aplicação de detecção, as faces detectadas são indicadas por retângulos vermelhos.

Não foram encontrados menus, telas de cadastro, autenticação, configuração de usuários, mensagens visuais de autorização/negação ou controles de acesso integrados à interface.

## 9. Limitações e pontos de atenção identificados

Esta seção registra aspectos observáveis do código; não representa funcionalidades presumidas.

1. **Detecção não equivale a reconhecimento:** não há extração/comparação de identidade, busca em base de usuários, limiar biométrico ou tratamento de falso aceite/falsa rejeição no código inspecionado.
2. **Controle de acesso não está implementado no fluxo:** não foi encontrada regra de autorização, abertura de fechadura/dispositivo, registro de entrada ou associação da detecção com permissões.
3. **Frames lidos duas vezes por iteração:** ambos os loops de câmera chamam `grab()` na condição e novamente no corpo. Isso descarta frames e pode afetar a fluidez.
4. **Caminho do modelo dependente do diretório de execução:** o caminho relativo utilizado pelo classificador pode funcionar apenas quando a aplicação é executada a partir de uma localização compatível.
5. **Credencial embutida no fonte:** a classe de conexão contém uma senha em texto no código. Remova-a, rotacione-a se for real e use configuração externa protegida.
6. **Configuração fixa do banco:** endereço, porta, banco e usuário são definidos diretamente no programa, sem configuração por ambiente observada.
7. **Liberação de recursos em exceções:** as operações de parada/fechamento aparecem no caminho normal, mas não em um bloco `finally`; uma falha no meio da captura pode deixar recursos abertos.
8. **Tratamento de erro voltado ao console:** os erros são impressos no terminal; não há recuperação, retentativa ou apresentação de estado de erro na interface.
9. **Dependência de ambiente físico/desktop:** a câmera no índice `0` e a interface gráfica limitam a execução em máquinas sem webcam, permissões ou sessão gráfica.
10. **Banco sem contrato de esquema no repositório inspecionado:** não há script ou migração que documente como o banco `biometria_db` deve ser criado.
11. **Sem testes automatizados identificados:** não há testes sob a estrutura de fontes observada, e o POM não declara configuração de testes.

## 10. Build, testes e validação

O projeto declara um POM Maven e arquivos compilados em `controle-acesso/target/classes`. Durante a preparação deste relatório, foi feita inspeção estática dos arquivos; não foi executado `mvn test`, `mvn package`, nem a aplicação com webcam ou MySQL.

Consequentemente:

- A presença dos `.class` indica que arquivos compilados existem no diretório `target`, mas não valida o estado atual do código-fonte.
- Não se afirma que o projeto compile ou que os programas funcionem no ambiente atual.
- O funcionamento da câmera, do modelo e do banco requer validação em ambiente com os recursos correspondentes.

## 11. Escopo desta análise

O relatório cobre o conteúdo de projeto listado e inspecionado, com foco no módulo `controle-acesso`, no seu POM, nas três classes Java e no modelo Haar. Diretórios de build, como `target/classes`, foram tratados como artefatos gerados. Scripts de automação de modernização foram identificados como apoio e não como parte da aplicação.

Este documento descreve evidências estáticas encontradas nos arquivos; não pressupõe funcionalidades ausentes nem resultados de execução que não foram verificados.

## 12. Relatório atualizado do código atual

Esta seção foi acrescentada após a revisão dos seis arquivos Java presentes em `controle-acesso/src/main/java/com/biometria/`. Ela complementa e substitui as descrições anteriores sempre que o código atual apresentar funcionalidades que ainda não existiam no levantamento original.

### 12.1 Resumo atualizado

O módulo contém uma aplicação desktop Java com uma janela principal Swing e fluxos para cadastrar usuários, capturar imagens faciais, treinar um reconhecedor LBPH e comparar rostos capturados pela webcam com as imagens disponíveis. O código também inclui dois programas independentes para testar a câmera e demonstrar a detecção facial, além de um programa de teste de conexão JDBC.

| Aspecto | Estado atual observado |
|---|---|
| Linguagem e compilação | Java 17, configurado no Maven |
| Interface principal | Swing (`JFrame`, botões e caixa de diálogo) |
| Captura e processamento de vídeo | JavaCV e OpenCV |
| Detecção de faces | Classificador Haar Cascade |
| Reconhecimento | OpenCV LBPH, treinado a partir de arquivos de imagem |
| Banco de dados | MySQL via JDBC; código espera uma tabela `usuarios` |
| Coleta de amostras | Até 25 imagens por cadastro, após contagem regressiva |
| Testes automatizados | Não identificados na estrutura inspecionada |
| Execução real | Não validada neste relatório; depende de webcam, ambiente gráfico, modelo e MySQL |

### 12.2 Estrutura atual dos arquivos de código

```text
controle-acesso/
├── pom.xml
├── haarcascade_frontalface_alt.xml
├── Fotos/
│   └── imagens faciais já armazenadas
└── src/main/java/com/biometria/
    ├── MenuPrincipal.java
    ├── CadastroUsuario.java
    ├── Reconhecimento.java
    ├── DeteccaoFacial.java
    ├── CapturaCamera.java
    └── ConexaoBD.java
```

O projeto também possui o diretório `target/`, com artefatos de compilação gerados anteriormente. Esses arquivos não substituem a verificação do código-fonte atual.

### 12.3 Explicação detalhada de cada classe

#### `MenuPrincipal`

Arquivo: [MenuPrincipal.java](./controle-acesso/src/main/java/com/biometria/MenuPrincipal.java)

É o ponto de entrada da interface gráfica principal. A classe herda de `JFrame` e configura uma janela intitulada “Controle de Acesso Biométrico”, com tamanho de 400 por 250 pixels, centralizada na tela. O layout usa uma grade com três linhas: um título e dois botões.

- **Cadastrar Novo Usuário:** abre uma caixa de diálogo Swing para solicitar o nome. Se o usuário cancelar ou deixar o campo vazio (inclusive apenas espaços), o cadastro não é iniciado. Caso contrário, o texto informado é passado para `CadastroUsuario.iniciarCadastro`.
- **Acessar (Reconhecimento):** chama `Reconhecimento.iniciarReconhecimento`.
- A janela principal é aberta por `SwingUtilities.invokeLater`, que agenda a criação da interface na fila de eventos do Swing.

Os listeners executam as operações diretamente quando o botão é acionado. Como captura de vídeo e banco são operações potencialmente demoradas, executá-las na fila de eventos da interface pode fazer a janela principal parar de responder enquanto o fluxo está em andamento.

#### `CadastroUsuario`

Arquivo: [CadastroUsuario.java](./controle-acesso/src/main/java/com/biometria/CadastroUsuario.java)

O método `iniciarCadastro(String nome)` realiza primeiro a inserção do usuário no MySQL e, em seguida, inicia a captura das amostras faciais.

1. Inicializa o identificador local com `-1`.
2. Abre uma conexão JDBC com os parâmetros de conexão definidos diretamente no código.
3. Executa `INSERT INTO usuarios (nome) VALUES (?)` com `PreparedStatement`, usando parâmetro para o nome.
4. Solicita a chave gerada pelo banco e guarda o primeiro valor retornado como identificador do usuário.
5. Fecha a conexão após a inserção.
6. Configura a webcam de índice `0`, o conversor de frames para `Mat` e o classificador Haar indicado por um caminho relativo.
7. Abre uma janela de captura. Fechar essa janela encerra apenas o fluxo de captura, pois ela usa `DISPOSE_ON_CLOSE`.
8. Só inicia a contagem regressiva quando encontra exatamente um rosto no frame. A contagem é de cinco segundos; se nenhum ou mais de um rosto for detectado antes de começar a fotografar, o cronômetro volta a zero.
9. Depois da contagem, recorta o rosto da imagem em tons de cinza, redimensiona-o para 160 × 160 pixels e grava as amostras como JPG.
10. Atualiza na janela a contagem de fotos e termina ao atingir 25 capturas.
11. No fluxo normal, para e fecha o grabber, fecha o classificador e descarta a janela. Exceções são informadas no console.

O caminho produzido para as imagens usa `controle-acesso/fotos/` com “fotos” em minúsculas. O código de reconhecimento procura `controle-acesso/Fotos/` com “Fotos” em maiúsculas. Em sistemas de arquivos sensíveis a maiúsculas e minúsculas, como a configuração Linux usada nesta análise, esses diretórios são diferentes. Além disso, ambos os caminhos são relativos ao diretório de execução. Essa diferença pode fazer com que as imagens recém-cadastradas não sejam encontradas pelo treinamento.

A inserção no banco ocorre antes da captura. Se a captura falhar ou for cancelada, o registro do usuário pode permanecer no banco sem as 25 imagens esperadas. O código também pressupõe que o diretório de destino exista e que as imagens possam ser gravadas nele.

#### `Reconhecimento`

Arquivo: [Reconhecimento.java](./controle-acesso/src/main/java/com/biometria/Reconhecimento.java)

O método `iniciarReconhecimento()` reúne treinamento do modelo, captura ao vivo, detecção facial, previsão da identidade e consulta ao banco.

1. Procura arquivos terminados em `.jpg` no diretório relativo `controle-acesso/Fotos`.
2. Se o diretório não puder ser lido ou não contiver arquivos, mostra um aviso e encerra o fluxo.
3. Para cada foto, carrega a imagem em escala de cinza. O identificador do usuário é obtido da parte anterior ao primeiro hífen no nome do arquivo. Por exemplo, nomes no padrão `id-nome-numero.jpg` fornecem o `id` como rótulo.
4. Coloca as imagens em um `MatVector` e seus identificadores em uma matriz de rótulos OpenCV do tipo inteiro.
5. Cria um reconhecedor `LBPHFaceRecognizer` e treina-o com as imagens e rótulos carregados.
6. Abre a webcam, o conversor de frames e o classificador Haar para localizar rostos durante o vídeo.
7. Para cada rosto detectado, converte a imagem para tons de cinza, recorta o rosto, redimensiona-o para 160 × 160 e solicita ao reconhecedor um identificador previsto e uma medida de confiança.
8. Considera o resultado elegível para consulta quando o identificador não é `-1` e o valor de confiança é inferior a `50.0`.
9. Consulta `usuarios` por meio de `SELECT nome FROM usuarios WHERE id = ?`, usando `PreparedStatement`. Se localizar um nome, exibe “Acesso desbloqueado” e muda a cor da caixa para verde. O estado inicial é desconhecido e a cor inicial é vermelha.
10. Desenha a caixa e o texto sobre o frame e apresenta a imagem na janela.
11. Ao terminar normalmente, para o grabber e fecha a conexão. A janela usa `DISPOSE_ON_CLOSE`, preservando a janela principal.

O texto “Acesso desbloqueado” representa apenas uma mensagem exibida pela aplicação. Não foi encontrado no código um comando para acionar uma fechadura ou outro dispositivo físico. A decisão depende do modelo treinado, das imagens encontradas e do limite numérico fixo no código; não há calibração ou validação de desempenho documentada.

#### `DeteccaoFacial`

Arquivo: [DeteccaoFacial.java](./controle-acesso/src/main/java/com/biometria/DeteccaoFacial.java)

É um programa independente, iniciado pelo seu próprio `main`, para demonstrar a localização de rostos. Ele abre a câmera `0`, converte frames para a estrutura `Mat`, aplica o classificador Haar e desenha um retângulo vermelho ao redor de cada região detectada. Mostra o vídeo processado numa janela intitulada “Detecção Facial - APS Biometria”.

Este programa detecta regiões faciais, mas não identifica pessoas, não consulta o banco e não toma decisões de acesso. O classificador é carregado de um caminho relativo, portanto a localização efetiva do arquivo depende do diretório de trabalho da execução.

#### `CapturaCamera`

Arquivo: [CapturaCamera.java](./controle-acesso/src/main/java/com/biometria/CapturaCamera.java)

É um utilitário independente de teste da webcam. Seu `main` abre a câmera `0`, exibe os frames numa janela e fecha os recursos no encerramento normal. Em caso de exceção, escreve uma mensagem de erro e o stack trace no console.

Na condição do loop, o código chama `grabber.grab()` para verificar se há frame; dentro do loop, chama `grab()` novamente para obter o frame que será mostrado. Assim, o frame obtido na condição não é reaproveitado e pode ser descartado.

#### `ConexaoBD`

Arquivo: [ConexaoBD.java](./controle-acesso/src/main/java/com/biometria/ConexaoBD.java)

É um teste independente de conectividade JDBC. Usa `DriverManager` para tentar abrir conexão ao banco local configurado, imprime mensagens de sucesso ou erro e fecha a conexão em caso de sucesso. Não cria o banco nem tabelas e não executa operações de cadastro.

#### `haarcascade_frontalface_alt.xml`

Arquivo: [haarcascade_frontalface_alt.xml](./controle-acesso/haarcascade_frontalface_alt.xml)

É o modelo Haar Cascade utilizado para localizar regiões que se parecem com rostos. O arquivo não contém os usuários cadastrados nem substitui as imagens usadas para treinar o reconhecedor LBPH.

### 12.4 Fluxo funcional integrado

```text
MenuPrincipal
├── Cadastrar Novo Usuário
│   └── CadastroUsuario.iniciarCadastro(nome)
│       ├── grava nome e obtém ID em usuarios
│       ├── detecta um rosto com Haar Cascade
│       └── salva até 25 imagens recortadas e redimensionadas
└── Acessar (Reconhecimento)
    └── Reconhecimento.iniciarReconhecimento()
        ├── carrega fotos e IDs dos nomes dos arquivos
        ├── treina LBPH
        ├── localiza rosto ao vivo com Haar Cascade
        ├── estima ID com LBPH
        └── consulta nome em usuarios e mostra o resultado

Pontos de entrada independentes:
├── DeteccaoFacial.main() — demonstra detecção facial
├── CapturaCamera.main() — demonstra captura de vídeo
└── ConexaoBD.main() — testa conexão MySQL
```

### 12.5 Dados esperados e dependências operacionais

O código de cadastro e reconhecimento pressupõe um MySQL local acessível e uma tabela `usuarios` com, pelo menos, uma coluna `id` compatível com chave gerada e uma coluna `nome`. O repositório inspecionado não inclui script de criação do banco ou definição formal dessas colunas.

As classes `CadastroUsuario`, `Reconhecimento` e `ConexaoBD` contêm configuração de conexão escrita no código. Os valores sensíveis não são reproduzidos neste relatório. Recomenda-se remover credenciais do fonte, substituí-las se forem reais e fornecê-las por configuração externa protegida.

Para execução são necessários Java 17, dependências Maven declaradas, uma webcam acessível como dispositivo `0`, um ambiente gráfico e o XML do classificador no caminho esperado. O fluxo de reconhecimento também depende de imagens compatíveis com o padrão de nomes e com o diretório que o código procura.

### 12.6 Observações, riscos e limitações do código atual

1. **Diferença no diretório das imagens:** cadastro escreve em `fotos` minúsculo, enquanto reconhecimento lê `Fotos` com maiúscula. Isso é incompatível em sistemas de arquivos sensíveis a caixa.
2. **Caminhos relativos:** diretório das fotos e modelo Haar dependem do diretório de trabalho; iniciar a aplicação de outro local pode impedir a leitura ou gravação de arquivos.
3. **Configuração sensível no código:** parâmetros e credenciais JDBC estão embutidos nas classes. Não devem ser publicados nem reutilizados se forem credenciais reais.
4. **Cadastro parcial:** o registro no banco ocorre antes de capturar as imagens, então uma falha durante a câmera pode deixar usuário sem amostras.
5. **Recursos em caso de exceção:** vários recursos são fechados no caminho normal, mas não há fechamento garantido em `finally` ou `try-with-resources` em todos os fluxos.
6. **Leitura duplicada de frames:** os loops que testam `grab()` na condição e o chamam novamente no corpo podem descartar frames.
7. **Interface potencialmente bloqueada:** as operações de câmera e banco são acionadas pelos listeners da interface Swing e executadas no mesmo fluxo de eventos.
8. **Limite LBPH fixo:** o valor `50.0` está definido diretamente no código, sem documentação de calibração para diferentes câmeras ou condições de iluminação.
9. **Persistência parcial do cadastro facial:** não há transação que coordene inserção do usuário e gravação das imagens, nem limpeza automática do registro em caso de falha.
10. **Controle de acesso físico não implementado:** a aplicação informa o resultado na janela, mas não aciona uma fechadura, relé ou serviço externo.
11. **Sem testes automatizados identificados:** não foram encontrados testes de unidade ou integração na estrutura inspecionada.

### 12.7 Alterações documentadas nesta revisão

Na revisão anterior do código foram removidos comentários explicativos genéricos dos arquivos `MenuPrincipal.java`, `CadastroUsuario.java`, `DeteccaoFacial.java` e `Reconhecimento.java`. A retirada desses comentários não alterou a lógica dos métodos. `CapturaCamera.java` e `ConexaoBD.java` não continham comentários equivalentes; URLs JDBC não são comentários.

Esta atualização altera apenas o relatório. Não foram modificadas as classes Java, o esquema do banco ou as imagens cadastradas.

### 12.8 Validação e limites da análise

O conteúdo foi documentado por inspeção estática dos arquivos. A tentativa de compilar o módulo não pôde ser realizada porque o comando `mvn` não está instalado no ambiente usado nesta revisão. Também não foram executados os fluxos com webcam, MySQL ou interface gráfica. Portanto, este relatório descreve o comportamento pretendido segundo o código e destaca dependências observáveis, mas não afirma que o programa foi executado ou validado funcionalmente.
