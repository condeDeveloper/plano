package br.com.conde.plano;

import java.util.ArrayList;
import java.util.List;

/**
 * A GULOSA: junta sempre o par que produz menos linhas, e repete.
 *
 * <p>Ela nao guarda plano de conjunto nenhum e nao volta atras. Com n tabelas ela faz n
 * menos uma rodadas, e em cada uma olha todos os pares que restam: alguns milhares de
 * comparacoes onde a dinamica faz dezenas de milhares e a forca bruta faz milhoes.
 *
 * <p>E ela existe neste repositorio por um motivo que nao e didatico. Acima de umas vinte
 * tabelas a programacao dinamica deixa de caber: 2 elevado a 20 ja e um milhao de conjuntos
 * e 2 elevado a 40 nao existe. Todo otimizador de verdade tem um limite a partir do qual
 * ele troca a busca exata por alguma coisa assim, e a pergunta que interessa e quanto isso
 * custa quando a busca exata ainda caberia.
 *
 * <p>O empate e desfeito pelo par de menor indice, para o plano nao depender da ordem em
 * que os candidatos foram varridos.
 */
public final class Gulosa implements Otimizador {

    @Override
    public String nome() {
        return "gulosa";
    }

    @Override
    public Plano planejar(Consulta consulta) {
        List<Plano> pedacos = new ArrayList<>();

        for (int i = 0; i < consulta.tabelas(); i++) pedacos.add(new Plano.Folha(i));

        while (pedacos.size() > 1) {
            int melhorA = 0;
            int melhorB = 1;
            double menor = Double.POSITIVE_INFINITY;

            for (int a = 0; a < pedacos.size(); a++)
                for (int b = a + 1; b < pedacos.size(); b++) {
                    double linhas = consulta.cardinalidade(
                            pedacos.get(a).conjunto() | pedacos.get(b).conjunto());

                    if (linhas >= menor) continue;

                    menor = linhas;
                    melhorA = a;
                    melhorB = b;
                }

            Plano esquerda = pedacos.get(melhorA);
            Plano direita = pedacos.get(melhorB);

            /* Tira o de indice maior primeiro, senao o outro indice escorrega. */
            pedacos.remove(melhorB);
            pedacos.remove(melhorA);

            pedacos.add(new Plano.Juncao(esquerda, direita));
        }

        return pedacos.get(0);
    }

    @Override
    public long conjuntosExaminados(Consulta consulta) {
        long total = 0;

        for (int restam = consulta.tabelas(); restam > 1; restam--)
            total += (long) restam * (restam - 1) / 2;

        return total;
    }
}
