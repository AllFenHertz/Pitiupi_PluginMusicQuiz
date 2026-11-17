Escolha manual da placa de rede (Pitiupi)

Arquivos alterados (3), em pitiupi/src/pitiupi/presentation/shell/:
  - LocalNetworkInfo.java    lista as placas e deixa uma delas valer sobre Cabo/Wi-Fi
  - NetworkPreferences.java  grava e carrega a placa escolhida
  - SettingsPanel.java       novo campo "Placa de rede" na aba Configuracoes

Como aplicar:
  1. Faça uma copia dos 3 originais.
  2. Copie estes 3 arquivos por cima, no mesmo caminho do seu repositorio.
  3. Na raiz do projeto: scripts\pitiupi.cmd run --all   (recompila o nucleo)

Como usar:
  Configuracoes > "Placa de rede" > escolha a do ZeroTier (descricao
  "ZeroTier Virtual Port") > Aplicar. O campo "Interface" e o "IP local"
  devem mostrar o IP do ZeroTier.

Sem mexer em ConnectionManager, SocketUDP nem ConnectionFactory: eles ja usam
LocalNetworkInfo para escolher a interface.
