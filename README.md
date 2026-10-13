# plano

Ordenação de junções do zero em Java 21: **errar a estimativa custa mais do que errar o
algoritmo.**

O juiz é a **força bruta**, que monta todos os planos que existem e fica com o mais barato. Ela é
o juiz certo e não só o conveniente porque não supõe o princípio da otimalidade, que é exatamente
o que a programação dinâmica precisa que seja verdade. O princípio vale neste modelo de custo e
pode não valer em outro, por exemplo quando o custo depende da ordem em que as linhas saem de uma
junção. A força bruta nota; uma prova no papel não.

E ele exige **validade e um piso, não eficiência**: que o plano use todas as tabelas, cada uma uma
vez só, com exatamente n menos uma junções, produzindo o número certo de linhas, e que o custo
nunca fique abaixo do que a força bruta achou. Chegar perto do ótimo não é exigido, e a distância
até ele é o que as tabelas medem em vez de julgar.

A unidade é a **linha intermediária**, nunca o segundo. O custo de um plano é a soma dos tamanhos
de todos os resultados intermediários, que é o modelo que captura exatamente aquilo que a ordem
das junções controla e deixa de fora a escolha do algoritmo de junção, que é outra decisão.

## A dinâmica acerta, e custa nove vezes menos

```
$ mvn exec:java -Dexec.args="correcao"

  formato     tabelas  consultas  planos montados  conjuntos da DP  invalidos    discordou
  cadeia            6         40            37800             2520          0            0
  estrela           6         40            37800             2520          0            0
  ciclo             6         40            37800             2520          0            0
  floco             6         40            37800             2520          0            0
  completa          6         40            37800             2520          0            0

  213800 planos montados contra 23800 conjuntos da dinamica.
  0 planos invalidos, 0 consultas em que a dinamica discordou.
```

Com n tabelas são `(2n-3)!!` planos: 1, 3, 15, 105, **945**, 10.395 e 135.135 de duas a oito
tabelas. Com nove já são dois milhões, e é por isso que o juiz para em oito e o assunto existe.

Em mil consultas de cinco formatos, a programação dinâmica achou o mesmo custo que a força bruta
em **todas**. É essa tabela que autoriza as outras quatro a usarem a dinâmica como régua; sem ela,
usar a dinâmica como ótimo seria supor o que se quer provar.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `ordem`, `forma`, `guloso` e `estimativa`.

## O que as medidas me corrigiram

**Eu montei o repositório para comparar algoritmos de ordenação. O algoritmo é a menor das
decisões: errar as seletividades por um fator dois já custa o mesmo que jogar a busca exata fora.**

```
$ mvn exec:java -Dexec.args="estimativa"

   tabelas otimizador               erro          media        mediana             pior
        10 dinamica                    1          1.000          1.000        1.000e+00
        10 dinamica                    2          1.120          1.000        3.622e+00
        10 dinamica                   10          2.426          1.266        6.663e+01
        10 dinamica                  100          9.288          2.081        9.333e+02
        10 a esquerda                  1          1.000          1.000        1.164e+00
        10 gulosa                      1          1.118          1.000        1.151e+01
```

As duas últimas linhas são heurísticas rodando com **estimativa perfeita**: elas sabem a
seletividade de verdade e só escolhem pior. As quatro primeiras são a busca exata rodando com
estimativa errada por um fator: ela escolhe o melhor plano para a consulta que ela **acha** que
tem, e depois a máquina paga o custo da consulta que ela **tem**.

Leia as linhas de 2 e de gulosa juntas: **1,120 e 1,118**. Errar cada seletividade por um fator
dois, que é otimismo num sistema de verdade, custa exatamente o mesmo que trocar a programação
dinâmica pela heurística mais simples que existe.

Um fator dez custa **2,426**, o dobro disso. Um fator cem, que acontece quando o erro se
multiplica ao longo de uma cadeia de junções, custa **9,288**, com uma consulta chegando a 933
vezes o ótimo.

Isso inverte o que eu ia escrever. A pergunta útil não é qual algoritmo de busca usar. É quanto a
estimativa de cardinalidade merece de esforço em relação a tudo o mais, e a resposta medida é:
mais do que o resto junto.

### O ótimo quase nunca é uma árvore à esquerda, e proibir as outras não custa nada

```
$ mvn exec:java -Dexec.args="forma"

  formato     tabelas        media           pior       perde em otimo a esquerda otimo com produto
  cadeia            4       1.0008          1.140         0.0100           0.3100           0.0000
  cadeia            8       1.0041          1.330         0.0250           0.1250           0.0000
  cadeia           10       1.0000          1.000         0.0000           0.0850           0.0000
  estrela          10       1.0000          1.000         0.0000           1.0000           0.0000
  floco            10       1.0010          1.164         0.0100           0.3300           0.0000
  completa         10       1.0000          1.000         0.0000           0.1750           0.0000
```

Árvore à esquerda quer dizer que todo lado direito de uma junção é uma tabela base: junta-se o
que já se tem com mais uma, e nunca dois resultados intermediários entre si. É a restrição
clássica, e ela corta a busca.

Olhei primeiro a coluna `otimo a esquerda`: numa cadeia de dez tabelas, o melhor plano é uma
árvore à esquerda em **8,5%** das consultas. Escrevi que restringir a busca a árvores à esquerda
devia custar caro.

A coluna da média diz **1,0000**. Zero. Em duzentas consultas de dez tabelas, a busca restrita
achou um plano tão barato quanto o melhor de todos, sempre.

Os dois números não se contradizem, e o que eles dizem juntos é mais interessante que qualquer um
sozinho: há **muitos** planos empatados no ótimo, e sempre havia um à esquerda entre eles. A
dinâmica devolve outro só porque a ordem de varredura dela chega primeiro num plano de qualquer
forma. A restrição não corta o ótimo, corta empates.

No formato **estrela** isso vira regra: o ótimo é à esquerda em **100%** das oitocentas consultas.
Faz sentido depois de medido, porque as pontas de uma estrela só ligam com o centro, e juntar duas
pontas entre si é produto cartesiano.

E a última coluna é **zero em todas as vinte linhas**: em quatro mil consultas, o melhor plano
nunca juntou dois lados sem predicado entre eles. Essa é a heurística mais antiga do assunto, de
1979, e ela não é teorema; aqui ela se confirmou em todas.

## Não otimizar custa de 2 a 40 mil vezes

```
$ mvn exec:java -Dexec.args="ordem"

  formato     tabelas            media          mediana               pior
  cadeia            4            154.0              2.5          2.120e+04
  cadeia           10            490.9             19.1          1.333e+04
  estrela          10            144.8             12.9          3.127e+03
  ciclo             6            458.4             14.3          3.833e+04
  completa          6            903.2             13.7          3.992e+04
```

A ordem escrita não é um otimizador: ela junta as tabelas na ordem em que aparecem na consulta. É
o que acontece quando não há otimizador, e quem já reordenou a cláusula `FROM` para a consulta
ficar rápida estava usando este.

A diferença entre as duas colunas é metade da leitura. A média passa de 400 vezes o ótimo e a
mediana fica em 19: **o estrago não está espalhado, está numa cauda.** Metade das consultas
escritas na ordem errada custa menos de 20 vezes o certo, e algumas custam 40 mil.

É assim que um sistema sem otimizador parece funcionar por meses e depois trava numa consulta só.

## A gulosa depende inteiramente do formato

```
$ mvn exec:java -Dexec.args="guloso"

  formato     tabelas        media           pior    acerta em pares da gulosa conjuntos da DP
  cadeia            6       1.2477         14.375       0.6950             35             63
  cadeia           10       1.1995          4.893       0.5500            165           1023
  estrela          10       1.0000          1.000       1.0000            165           1023
  floco            10       1.2592         11.508       0.4450            165           1023
  completa         10       1.0000          1.000       1.0000            165           1023
```

A gulosa junta sempre o par que produz menos linhas e nunca volta atrás. Com dez tabelas ela olha
**165** pares onde a dinâmica olha **1.023** conjuntos, e a diferença cresce exponencialmente:
acima de umas vinte tabelas a busca exata simplesmente não cabe, e todo otimizador de verdade
troca para alguma coisa assim.

Na **estrela** e na **completa** ela é exatamente ótima em todas as oitocentas consultas de cada.
No **floco**, que é o formato mais comum em sistema de verdade, ela acha o ótimo em **44,5%** das
vezes e custa 26% a mais na média, com uma consulta chegando a 11,5 vezes.

Ou seja: num armazém de dados, onde quase tudo é estrela, a busca exata não compra nada. Fora
dele, compra.

## O que não tem aqui

**O algoritmo de cada junção.** O modelo de custo diz que produzir n linhas custa n, e não se é
laço aninhado, espalhamento ou ordena-e-mescla. Essa é outra decisão e está no repositório
[`juncao`](https://github.com/condeDeveloper/juncao). Misturar as duas numa conta só é o jeito de
não conseguir explicar nenhuma.

**De onde vem a estimativa.** Aqui o erro de estimativa é um fator sorteado. Num sistema de
verdade ele vem de histogramas, de amostragem e da hipótese de independência entre predicados, que
quase nunca vale, e o erro é **enviesado**: quase sempre para baixo, e quase sempre pior em cadeias
longas. A tabela da estimativa é, se alguma coisa, otimista.

**Replanejar no meio.** Um otimizador moderno pode medir a cardinalidade de verdade do primeiro
passo e refazer o resto do plano. É a resposta direta à tabela da estimativa e não está aqui.

**Memória e paralelismo.** Uma árvore à esquerda pode fluir sem materializar; uma árvore de
qualquer forma precisa guardar os dois lados. Esse custo é real e não entra em nenhuma coluna, e
ele é a razão prática de a restrição à esquerda existir, muito mais do que o tamanho da busca.

## Licença

MIT.
