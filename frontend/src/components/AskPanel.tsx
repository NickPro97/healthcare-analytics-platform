import { useState, type FormEvent } from "react";
import { api } from "../api/client";
import type { AssistantAnswer } from "../api/types";

interface Turn {
  question: string;
  answer?: AssistantAnswer;
  error?: string;
}

export function AskPanel({ patientId, suggestions }: { patientId?: number; suggestions: string[] }) {
  const [question, setQuestion] = useState("");
  const [turns, setTurns] = useState<Turn[]>([]);
  const [busy, setBusy] = useState(false);

  async function ask(q: string) {
    const text = q.trim();
    if (!text || busy) return;
    setBusy(true);
    setQuestion("");
    setTurns((t) => [...t, { question: text }]);
    try {
      const answer = await api.ask(text, patientId);
      setTurns((t) => t.map((turn, i) => (i === t.length - 1 ? { ...turn, answer } : turn)));
    } catch (e) {
      const error = e instanceof Error ? e.message : "The assistant is unavailable";
      setTurns((t) => t.map((turn, i) => (i === t.length - 1 ? { ...turn, error } : turn)));
    } finally {
      setBusy(false);
    }
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    void ask(question);
  }

  return (
    <div className="ask">
      <div className="chips">
        {suggestions.map((s) => (
          <button key={s} type="button" className="chip" onClick={() => void ask(s)} disabled={busy}>{s}</button>
        ))}
      </div>
      <div className="turns" aria-live="polite">
        {turns.map((t, i) => (
          <div key={i} className="turn">
            <div className="q">{t.question}</div>
            {t.answer && (
              <div className="a">
                <div className="answer-text">{t.answer.answer}</div>
                <div className="small muted">
                  Sources: {t.answer.sources.join(", ") || "none"}
                  {t.answer.usedPatientData && " · used this patient's recent data"}
                </div>
                <div className="small disclaimer">{t.answer.disclaimer}</div>
              </div>
            )}
            {t.error && <div className="error-note">{t.error}</div>}
            {!t.answer && !t.error && <div className="muted small">Thinking...</div>}
          </div>
        ))}
      </div>
      <form className="ask-form" onSubmit={submit}>
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder={patientId ? "Ask about this patient..." : "Ask about NEWS2, sepsis screening, the risk model..."}
          maxLength={1000}
          aria-label="Question"
        />
        <button className="btn" type="submit" disabled={busy || !question.trim()}>Ask</button>
      </form>
    </div>
  );
}
