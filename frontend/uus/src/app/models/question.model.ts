export interface EvaluationResult {
  /**
   * Skoor 0–100, mida backend/OpenAI tagastab.
   */
  score: number;

  /**
   * Lühike hinnang, nt "Strong answer", "Needs improvement".
   */
  verdict?: string;

  /**
   * Positiivsed küljed vastuses.
   */
  strengths?: string[];

  /**
   * Parandamisideed / soovitused.
   */
  improvements?: string[];

  /**
   * Üldine kokkuvõte või kommentaar vastuse kohta.
   */
  summary?: string;
}

/**
 * Frontendi Question mudel.
 *
 * NB! Backend saadab välja nimega `question`, aga
 * osad komponendid kasutavad `text`. Seepärast
 * hoiame mõlemat ja sünkroniseerime need service’is.
 */
export interface Question {
  id: string;

  /** Tekst, nagu backend saadab */
  question: string;

  /** Alias samale tekstile, et kood saaks kasutada `text` */
  text?: string;

  /** Näiteks "TECH" või "SOFT" */
  category: string;

  /** Näiteks "JUNIOR", "MID", "SENIOR" */
  difficulty: string;

  /** Kandidaadi vastus (ainult frontendis) */
  answer?: string;

  /** Vastuse hindamise tulemus */
  evaluation?: EvaluationResult;
}
