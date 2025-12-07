// src/app/models/personality-question.model.ts

export interface PersonalityTurn {
  question: string;
  answer: string;
}

export interface PersonalityQuestionResponse {
  /** Järgmine küsimus, mille GPT kasutajale esitab */
  question: string;

  /** Kui true, siis intervjuu on läbi – enam küsimusi ei tule */
  isFinal: boolean;
}
