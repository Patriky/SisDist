# Sistemas Distribuídos - Trabalho de Implementação 1
	1- Criar no mínimo 3 Threads
	2- Utilizar comunicação MultiCast para as Threads se conhecerem (trocar chaves públicas) (0,6)
	3- Utilizar comunicação UniCast: (0,9)
		- no retorno da busca por arquivos
		- no envio e recepção de arquivos
		- implementar reputação (no caso de existir mais de um par que tenha o mesmo arquivo)
		- no caso de falha de envios, implementar contador para o número de falhas (isso é parte do cálculo de reputação)
	4- Implementar a Segurança: (0,5)
		- evitar que uma Thread mal-intencionada assuma o papel de um par escolhido
		- evitar que uma Thread mal-intencionada envie arquivo incorreto
		- evitar que uma Thread mal-intencionada prejudique a reputação de um par
		- implementar chaves assimétricas (chave pública e privada)
		- todo envio deve ser criptografado com a chave privada do par que está enviando o arquivo em questão
	
	Obs.:
	- Obrigatório documentar todo o código
	- Nada automático, todas as solicitações de arquivos se da por meio de entrada na interface
	- Criar um diretório para cada processo, com seu respectivo arquivo
