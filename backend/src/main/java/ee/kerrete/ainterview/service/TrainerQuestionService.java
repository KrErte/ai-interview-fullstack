package ee.kerrete.ainterview.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Tagastab treeneri jaoks järgmise küsimuse.
 *
 * Esialgu lihtne in-memory list, hiljem saab tuua DB-st / OpenAI-st.
 */
@Service
public class TrainerQuestionService {

    private static final List<String> QUESTIONS = List.of(
            "Kirjelda olukorda, kus pidid tegema kiire otsuse vähese infoga.",
            "Räägi ajast, kui pidid lahendama konflikti oma tiimis.",
            "Kirjelda hetke, kus võtsid ise initsiatiivi ilma, et keegi oleks käskinud.",
            "Räägi olukorrast, kus pidid motiveerima kolleegi, kes oli alla andmas.",
            "Kirjelda projekti, mis ebaõnnestus – mida sellest õppisid?",
            "Räägi ajast, kui parandasid või automatiseerisid mingi protsessi tiimis.",
            "Kirjelda olukorda, kus pidid ütlema 'ei' ebarealistlikule tähtajale.",
            "Räägi hetkest, kus pidid kaitsma oma tehnilist otsust teiste ees.",
            "Kirjelda olukorda, kus pidid töötama väga ebamääraste nõuetega.",
            "Räägi ajast, mil võtsid mentorrolli ja aitasid juniorit areneda."
    );

    private final Random random = new Random();

    public String getRandomQuestion() {
        return QUESTIONS.get(random.nextInt(QUESTIONS.size()));
    }
}
