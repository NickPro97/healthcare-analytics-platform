import { AskPanel } from "../components/AskPanel";

export function AssistantPage() {
  return (
    <div className="page">
      <header className="page-head">
        <h1>AI Assistant</h1>
        <p className="muted">
          Answers come only from curated reference notes (retrieved from ChromaDB) and cite them.
          Open a patient to ask about that patient's data.
        </p>
      </header>
      <div className="card">
        <AskPanel suggestions={[
          "What does a NEWS2 of 6 mean and what is the response?",
          "What is a red score?",
          "How is qSOFA calculated?",
          "What are the limits of the ML risk model?",
        ]} />
      </div>
    </div>
  );
}
